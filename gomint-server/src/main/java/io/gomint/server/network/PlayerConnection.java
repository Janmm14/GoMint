/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.network;

import io.gomint.jraknet.Connection;
import io.gomint.jraknet.PacketBuffer;
import io.gomint.jraknet.PacketReliability;
import io.gomint.math.Location;
import io.gomint.math.Vector;
import io.gomint.server.async.Delegate;
import io.gomint.server.entity.EntityCow;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.network.packet.*;
import io.gomint.server.scheduler.SyncScheduledTask;
import io.gomint.server.util.IntPair;
import io.gomint.server.world.CoordinateUtils;
import io.gomint.server.world.WorldAdapter;
import io.gomint.world.World;

import net.openhft.koloboke.collect.LongCursor;
import net.openhft.koloboke.collect.set.LongSet;
import net.openhft.koloboke.collect.set.hash.HashLongSets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static io.gomint.server.network.Protocol.*;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class PlayerConnection {

    private static final Logger logger = LoggerFactory.getLogger( PlayerConnection.class );

    // Network manager that created this connection:
    private final NetworkManager networkManager;

    // Actual connection for wire transfer:
    private final Connection connection;

    // Commonly used delegates:
    private Delegate<Packet> sendDelegate;

    // Connection State:
    private long lastPerformance;
    private PlayerConnectionState state;
    private int sentChunks;

    // Entity
    private EntityPlayer entity;

    // Used for BatchPacket decompression; stored here in order to save allocations at runtime:
    private Inflater batchDecompressor;
    private byte[] batchIntermediate;

    // World data
    private LongSet playerChunks;
    private LongSet currentlySendingPlayerChunks;
    private int previousChunkX;
    private int previousChunkZ;

    /**
     * Constructs a new player connection.
     *
     * @param networkManager The network manager creating this instance
     * @param connection     The jRakNet connection for actual wire-transfer
     * @param initialState   The player connection's initial state
     */
    PlayerConnection( NetworkManager networkManager, Connection connection, PlayerConnectionState initialState ) {
        this.networkManager = networkManager;
        this.connection = connection;
        this.state = initialState;

        this.batchDecompressor = new Inflater();
        this.batchIntermediate = new byte[1024];

        this.playerChunks = HashLongSets.newMutableSet();
        this.currentlySendingPlayerChunks = HashLongSets.newMutableSet();

        this.sendDelegate = new Delegate<Packet>() {
            @Override
            public void invoke( Packet arg ) {
                if ( arg != null ) {
                    PlayerConnection.this.send( arg );
                }
            }
        };
    }

    /**
     * Gets the player connection's current state.
     *
     * @return The player connection's current state
     */
    public PlayerConnectionState getState() {
        return this.state;
    }

    /**
     * Gets the player's actual jRakNet connection.
     *
     * @return The player's actual jRakNet connection
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Gets the delegate to be used whenever a packet is to be sent once an asynchronous
     * operation completes.
     *
     * @return The delegate to be used for sending packets asynchronously
     */
    public Delegate<Packet> getSendDelegate() {
        return this.sendDelegate;
    }

    /**
     * Gets the EntityPlayer associated with this player connection. This might return null if
     * the player has not logged in yet.
     *
     * @return The entity associated with this player connection
     */
    public EntityPlayer getEntity() {
        return this.entity;
    }

    /**
     * Performs a network tick on this player connection. All incoming packets are received and handled
     * accordingly.
     */
    public void tick() {
        // Receive all waiting packets:
        byte[] packetData;
        while ( ( packetData = this.connection.receive() ) != null ) {
            this.handleSocketData( new PacketBuffer( packetData, 0 ), false );
        }
    }

    /**
     * Sends the given packet to the player.
     *
     * @param packet The packet to send to the player
     */
    public void send( Packet packet ) {
        PacketBuffer buffer = new PacketBuffer( packet.estimateLength() == -1 ? 64 : packet.estimateLength() + 2 );
        buffer.writeByte( (byte) 0x8E );
        buffer.writeByte( packet.getId() );
        packet.serialize( buffer );
        this.connection.send( PacketReliability.RELIABLE, packet.orderingChannel(), buffer.getBuffer(), 0, buffer.getPosition() );
    }

    /**
     * Sends the given packet to the player.
     *
     * @param reliability     The reliability to send the packet with
     * @param orderingChannel The ordering channel to send the packet on
     * @param packet          The packet to send to the player
     */
    public void send( PacketReliability reliability, int orderingChannel, Packet packet ) {
        PacketBuffer buffer = new PacketBuffer( packet.estimateLength() == -1 ? 64 : packet.estimateLength() + 2 );
        buffer.writeByte( (byte) 0x8E );
        buffer.writeByte( packet.getId() );
        packet.serialize( buffer );
        this.connection.send( reliability, orderingChannel, buffer.getBuffer(), 0, buffer.getPosition() );
    }

    /**
     * Sends a world chunk to the player. This is used by world adapters in order to give the player connection
     * a chance to know once it is ready for spawning.
     *
     * @param chunkHash The hash of the chunk to keep track of what the player has loaded
     * @param chunkData The chunk data packet to send to the player
     */
    public void sendWorldChunk( long chunkHash, Packet chunkData ) {
        this.send( chunkData );

        synchronized ( this.playerChunks ) {
            this.currentlySendingPlayerChunks.removeLong( chunkHash );
            this.playerChunks.add( chunkHash );
        }

        if ( this.state == PlayerConnectionState.LOGIN ) {
            this.sentChunks++;

            if ( this.sentChunks == 64 ) {
                int spawnXChunk = CoordinateUtils.fromBlockToChunk( (int) this.entity.getLocation().getX() );
                int spawnZChunk = CoordinateUtils.fromBlockToChunk( (int) this.entity.getLocation().getZ() );

                WorldAdapter worldAdapter = (WorldAdapter) this.entity.getWorld();
                worldAdapter.movePlayerToChunk( Integer.MAX_VALUE, Integer.MAX_VALUE, spawnXChunk, spawnZChunk, this.entity );

                PacketConfirmChunkRadius packetConfirmChunkRadius = new PacketConfirmChunkRadius();
                packetConfirmChunkRadius.setChunkRadius( this.entity.getViewDistance() );
                this.send( packetConfirmChunkRadius );

                this.sendPlayState( PacketPlayState.PlayState.SPAWN );
                this.sendWorldTime( 0, false );
                this.sendMovePlayer( this.entity.getLocation() );
                this.state = PlayerConnectionState.PLAYING;
            }
        }
    }

    // ========================================= PACKET HANDLERS ========================================= //

    /**
     * Handles data received directly from the player's connection.
     *
     * @param buffer The buffer containing the received data
     */
    private void handleSocketData( PacketBuffer buffer, boolean batch ) {
        if ( buffer.getRemaining() <= 0 ) {
            // Malformed packet:
            return;
        }

        // Grab the packet ID from the packet's data
        byte packetId = buffer.readByte();
        if ( packetId == (byte) 0x8E && buffer.getRemaining() > 0 ) {
            packetId = buffer.readByte();
        }

        // If we are still in handshake we only accept certain packets:
        if ( this.state == PlayerConnectionState.HANDSHAKE ) {
            if ( packetId == PACKET_BATCH ) {
                this.handleBatchPacket( buffer, batch );
            } else if ( packetId == PACKET_LOGIN ) {
                PacketLogin login = new PacketLogin();
                login.deserialize( buffer );
                this.handleLoginPacket( login );
            } else {
	            System.out.println ( "Received odd packet" );
            }

            // Don't allow for any other packets if we are in HANDSHAKE state:
            return;
        }

        if ( packetId == PACKET_BATCH ) {
            this.handleBatchPacket( buffer, false );
        } else {
            Packet packet = Protocol.createPacket( packetId );
            if ( packet == null ) {
                this.networkManager.notifyUnknownPacket( packetId, buffer );

	            // Got to skip
	            buffer.skip( buffer.getRemaining() );
                return;
            }

            packet.deserialize( buffer );
            this.handlePacket( packet );
        }
    }

    /**
     * Handles compressed batch packets directly by decoding their payload.
     *
     * @param buffer The buffer containing the batch packet's data (except packet ID)
     */
    private void handleBatchPacket( PacketBuffer buffer, boolean batch ) {
	    if ( batch ) {
		    this.networkManager.getLogger().error( "Malformed batch packet payload: Batch packets are not allowed to contain further batch packets" );
		    return;
	    }

        buffer.skip( 4 );               // Compressed payload length (not of interest; only uncompressed size matters)

        this.batchDecompressor.reset();
        this.batchDecompressor.setInput( buffer.getBuffer(), buffer.getPosition(), buffer.getRemaining() );

	    ByteArrayOutputStream bout = new ByteArrayOutputStream();

	    try {
		    while ( this.batchDecompressor.finished() ) {
			    int read = this.batchDecompressor.inflate( this.batchIntermediate );
			    bout.write( this.batchIntermediate, 0, read );
		    }
	    } catch ( DataFormatException e ) {
		    this.networkManager.getLogger().error( "Failed to decompress batch packet", e );
		    return;
	    }

	    byte[] payload = bout.toByteArray();

        PacketBuffer payloadBuffer = new PacketBuffer( payload, 0 );
        while ( payloadBuffer.getRemaining() > 0 ) {
	        int packetLength = payloadBuffer.readInt();
	        int expectedPosition = payloadBuffer.getPosition() + packetLength;

            this.handleSocketData( payloadBuffer, true );

	        if ( payloadBuffer.getPosition() != expectedPosition ) {
		        this.networkManager.getLogger().error( "Malformed batch packet payload: Could not read enclosed packet data correctly" );
		        return;
	        }
        }
    }

    /**
     * Handles a deserialized packet by dispatching it to the appropriate handler method.
     *
     * @param packet The packet to handle
     */
    private void handlePacket( Packet packet ) {
        switch ( packet.getId() ) {
            case PACKET_TEXT:
                this.handleChat( (PacketText) packet );
                break;
            case PACKET_MOVE_PLAYER:
                this.handleMovePacket( (PacketMovePlayer) packet );
                break;
            case PACKET_SET_CHUNK_RADIUS:
                this.handleSetChunkRadius( (PacketSetChunkRadius) packet );
                break;
        }
    }

	private void handleChat( PacketText packet ) {
		if ( packet.getType() != PacketText.Type.PLAYER_CHAT ) {
			// Players are not allowed to send any other chat messages:
			return;
		}

		// Verify sender:
		if ( !packet.getSender().equals( this.entity.getUsername() ) ) {
			// Player is trying to fake messages:
			return;
		}

		// TODO: Trigger chat event here
		this.networkManager.broadcast( PacketReliability.RELIABLE, 0, packet );

        if ( packet.getMessage().contains( "spawn" ) ) {
	        Vector position = this.entity.getPosition();
	        WorldAdapter world = this.entity.getWorld();
	        world.spawnEntityAt( new EntityCow( world ), position.getX(), position.getY(), position.getZ() );
        }
	}

    private void handleSetChunkRadius( PacketSetChunkRadius packet ) {
        // Check if the wanted Viewdistance is under the servers setting
        int oldViewdistance = this.entity.getViewDistance();
        this.entity.setViewDistance( Math.min( packet.getChunkRadius(), this.networkManager.getServer().getServerConfig().getViewDistance() ) );
        if ( oldViewdistance != this.entity.getViewDistance() ) {
            logger.debug( "Setting new Viewdistance for " + this.entity.getUsername() + " to " + this.entity.getViewDistance() );
            this.checkForNewChunks();
        }

        PacketConfirmChunkRadius packetConfirmChunkRadius = new PacketConfirmChunkRadius();
        packetConfirmChunkRadius.setChunkRadius( this.entity.getViewDistance() );
        this.send( packetConfirmChunkRadius );
    }

    private void handleMovePacket( PacketMovePlayer packet ) {
        // TODO: Send some sort of movement event

	    Vector currentPosition = this.entity.getPosition();
	    int previousBlockX = (int) currentPosition.getX();
	    int previousBlockZ = (int) currentPosition.getZ();

	    this.previousChunkX = CoordinateUtils.fromBlockToChunk( previousBlockX );
	    this.previousChunkZ = CoordinateUtils.fromBlockToChunk( previousBlockZ );

	    this.entity.setPosition( packet.getX(), packet.getY(), packet.getZ() );

        if ( previousBlockX != (int) packet.getX() ||
             previousBlockZ != (int) packet.getZ() ) {
            this.checkForNewChunks();
        }
    }

    private void checkForNewChunks() {
        WorldAdapter worldAdapter = ( (WorldAdapter) this.entity.getWorld() );

        int currentXChunk = CoordinateUtils.fromBlockToChunk( (int) this.entity.getLocation().getX() );
        int currentZChunk = CoordinateUtils.fromBlockToChunk( (int) this.entity.getLocation().getZ() );

        int viewDistance = this.entity.getViewDistance();
        synchronized ( this.playerChunks ) {
            for ( int sendXChunk = currentXChunk - viewDistance; sendXChunk < currentXChunk + viewDistance; sendXChunk++ ) {
                for ( int sendZChunk = currentZChunk - viewDistance; sendZChunk < currentZChunk + viewDistance; sendZChunk++ ) {
                    if ( !this.playerChunks.contains( CoordinateUtils.toLong( sendXChunk, sendZChunk ) ) &&
                            !this.currentlySendingPlayerChunks.contains( CoordinateUtils.toLong( sendXChunk, sendZChunk ) ) ) {
                        this.currentlySendingPlayerChunks.add( CoordinateUtils.toLong( sendXChunk, sendZChunk ) );
                        worldAdapter.sendChunk( sendXChunk, sendZChunk, this.entity );
                    }
                }
            }
        }

        // Move the player to this chunk
        worldAdapter.movePlayerToChunk( this.previousChunkX, this.previousChunkZ, currentXChunk, currentZChunk, this.entity );

        // Check for unloading chunks
        synchronized ( this.playerChunks ) {
            LongCursor longCursor = this.playerChunks.cursor();
            while ( longCursor.moveNext() ) {
                IntPair intPair = CoordinateUtils.toIntPair( longCursor.elem() );
                if ( intPair.getX() > currentXChunk + viewDistance ||
                        intPair.getX() < currentXChunk - viewDistance ||
                        intPair.getZ() > currentZChunk + viewDistance ||
                        intPair.getZ() < currentZChunk - viewDistance ) {
                    // TODO: Check for Packets to send to the client to unload the chunk?
                    longCursor.remove();
                }
            }
        }
    }

    private void handleLoginPacket( PacketLogin packet ) {
        this.state = PlayerConnectionState.LOGIN;
        this.lastPerformance = System.currentTimeMillis();

        this.sendPlayState( PacketPlayState.PlayState.HANDSHAKE );

        // Create entity:
        WorldAdapter world = this.networkManager.getServer().getDefaultWorld();
        this.entity = new EntityPlayer( world, this );
        this.entity.setUsername( packet.getUsername() );
        this.entity.setUuid( packet.getClientUUID() );
        this.entity.setSecretToken( packet.getClientSecret() );
        this.entity.setViewDistance( this.networkManager.getServer().getServerConfig().getViewDistance() );

        this.sendWorldInitialization();

        // Add player to world (will send world chunk packets):
        world.addPlayer( this.entity );

        // Send Crafting recipes:
        this.send( PacketReliability.RELIABLE_ORDERED, 0, this.networkManager.getServer().getRecipeManager().getCraftingRecipesBatch() );
    }

    // ====================================== PACKET SENDERS ====================================== //

    /**
     * Sends a PacketPlayState with the specified state to this player.
     *
     * @param state The state to send
     */
    private void sendPlayState( PacketPlayState.PlayState state ) {
        PacketPlayState packet = new PacketPlayState();
        packet.setState( state );
        this.send( PacketReliability.RELIABLE_ORDERED, 0, packet );
    }

    /**
     * Sends the player a move player packet which will teleport him to the
     * given location.
     *
     * @param location The location to teleport the player to
     */
    private void sendMovePlayer( Location location ) {
        PacketMovePlayer move = new PacketMovePlayer();
        move.setEntityId( 0 );                      // All packets referencing the local player have entity ID 0
        move.setX( location.getX() );
        move.setY( location.getY() );
        move.setZ( location.getZ() );
        move.setYaw( 0.0F );
        move.setPitch( 0.0F );
        move.setTeleport( true );
        move.setOnGround( false );
        this.send( move );
    }

    /**
     * Sends the player the specified time as world time. The original client sends
     * the current world time every 256 ticks in order to synchronize all client's world
     * times.
     *
     * @param ticks    The current number of ticks of the world time
     * @param counting Whether or not the world time is counting upwards
     */
    private void sendWorldTime( int ticks, boolean counting ) {
        PacketWorldTime time = new PacketWorldTime();
        time.setTicks( ticks );
        time.setCounting( counting );
        this.send( time );
    }

    /**
     * Sends a world initialization packet of the world the entity associated with this
     * connection is currently in to this player.
     */
    private void sendWorldInitialization() {
        World world = this.entity.getWorld();
        PacketWorldInitialization init = new PacketWorldInitialization();
        init.setSeed( -1 );
        init.setDimension( (byte) 0 );
        init.setGenerator( 1 );
        init.setGamemode( 1 );
        init.setEntityId( 0L );
        init.setSpawnX( (int) world.getSpawnLocation().getX() );
        init.setSpawnY( (int) world.getSpawnLocation().getY() );
        init.setSpawnZ( (int) world.getSpawnLocation().getZ() );
        init.setX( world.getSpawnLocation().getX() );
        init.setY( world.getSpawnLocation().getY() );
        init.setZ( world.getSpawnLocation().getZ() );
        init.setAllowCheats( true );
        this.send( init );
    }

    /**
     * The underlying RakNet Connection closed. Cleanup
     */
    public void close() {
        if ( this.entity.getWorld() != null ) {
            ( (WorldAdapter) this.entity.getWorld() ).removePlayer( this.entity );
        }
    }
}
