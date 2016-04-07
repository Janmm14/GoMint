/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.jraknet.PacketReliability;
import io.gomint.math.Location;
import io.gomint.math.Vector;
import io.gomint.server.GoMintServer;
import io.gomint.server.async.Delegate;
import io.gomint.server.async.Delegate2;
import io.gomint.server.entity.Entity;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.network.packet.Packet;
import io.gomint.server.network.packet.PacketBatch;
import io.gomint.server.network.packet.PacketWorldChunk;
import io.gomint.world.Block;
import io.gomint.world.Gamerule;
import io.gomint.world.World;

import lombok.Getter;
import net.openhft.koloboke.collect.set.ObjSet;
import net.openhft.koloboke.collect.set.hash.HashObjSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public abstract class WorldAdapter implements World {

	// CHECKSTYLE:OFF
	// Shared objects
	@Getter
	protected final GoMintServer server;
	protected final Logger       logger;

	// World properties
	protected final File     worldDir;
	protected       String   levelName;
	protected       Location spawn;
	protected       Map<Gamerule, Object> gamerules = new HashMap<>();

    // I/O
    private Deflater deflater;
    private boolean asyncWorkerRunning;
    private BlockingQueue<AsyncChunkTask> asyncChunkTasks;
    private Queue<AsyncChunkPackageTask> chunkPackageTasks;

	// Chunk Handling
	protected ChunkCache chunkCache;

    // Player handling
    private ObjSet<EntityPlayer> players;

	// Entity Handling
	private EntityManager entityManager;

	protected WorldAdapter( GoMintServer server, File worldDir ) {
		this.server = server;
		this.logger = LoggerFactory.getLogger( "World-" + worldDir.getName() );
		this.worldDir = worldDir;
        this.players = HashObjSets.newMutableSet();
		this.entityManager = new EntityManager( this );
        this.deflater = new Deflater( Deflater.DEFAULT_COMPRESSION );
        this.asyncChunkTasks = new LinkedBlockingQueue<>();
        this.chunkPackageTasks = new ConcurrentLinkedQueue<>();
        this.startAsyncWorker( server.getExecutorService() );
	}
	// CHECKSTYLE:ON

	// ==================================== GENERAL ACCESSORS ==================================== //

	@Override
	public String getWorldName() {
		return this.worldDir.getName();
	}

	@Override
	public String getLevelName() {
		return this.levelName;
	}

	@Override
	public Location getSpawnLocation() {
		return this.spawn;
	}

	@Override
	public Block getBlockAt( Vector vector ) {
		return null;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T getGamerule( Gamerule<T> gamerule ) {
		return this.gamerules.containsKey( gamerule ) ? (T) this.gamerules.get( gamerule ) : null;
	}

	// ==================================== NETWORKING HELPERS ==================================== //

	/**
	 * Broadcasts the given packet to all players in this world.
	 *
	 * @param reliability The reliability to send the packet with
	 * @param orderingChannel The ordering channel to send the packet on
	 * @param packet The packet to send
	 */
	public void broadcast( PacketReliability reliability, int orderingChannel, Packet packet ) {
		// Avoid duplicate arrays containing the very same data:
		PacketBuffer buffer = new PacketBuffer( packet.estimateLength() == -1 ? 64 : packet.estimateLength() + 2 );
		buffer.writeByte( (byte) 0x8E );
		buffer.writeByte( packet.getId() );
		packet.serialize( buffer );

		// Avoid duplicate array copies:
		byte[] payload;

		if ( buffer.getRemaining() == 0 ) {
			payload = buffer.getBuffer();
		} else {
			payload = new byte[ buffer.getPosition() - buffer.getBufferOffset() ];
			System.arraycopy( buffer.getBuffer(), buffer.getBufferOffset(), payload, 0, buffer.getPosition() - buffer.getBufferOffset() );
		}

		// Send directly:
		for ( EntityPlayer player : this.players ) {
			player.getConnection().getConnection().send( reliability, orderingChannel, payload );
		}
	}

	// ==================================== ENTITY MANAGEMENT ==================================== //

	/**
	 * Get the current view of players on this world.
	 *
	 * @return The Collection View of the Players currently on this world
	 */
	public Collection<EntityPlayer> getPlayers() {
		return this.players;
	}

    /**
     * Adds a new player to this world and schedules all world chunk packets required for spawning
     * the player for send.
     *
     * @param player The player entity to add to the world
     */
    public void addPlayer( EntityPlayer player ) {
	    // Add player to player set:
	    this.players.add( player );

        // Schedule sending spawn region chunks:
        final int minBlockX = (int) (this.spawn.getX() - 64);
        final int minBlockZ = (int) (this.spawn.getZ() - 64);
        final int maxBlockX = (int) (this.spawn.getX() + 64);
        final int maxBlockZ = (int) (this.spawn.getZ() + 64);

        final int minChunkX = CoordinateUtils.fromBlockToChunk( minBlockX );
        final int minChunkZ = CoordinateUtils.fromBlockToChunk( minBlockZ );
        final int maxChunkX = CoordinateUtils.fromBlockToChunk( maxBlockX );
        final int maxChunkZ = CoordinateUtils.fromBlockToChunk( maxBlockZ );

        for ( int i = minChunkZ; i <= maxChunkZ; ++i ) {
            for ( int j = minChunkX; j <= maxChunkX; ++j ) {
                this.sendChunk( j, i, player );
            }
        }
    }

    /**
     * Removes a player from this world and cleans up its references
     *
     * @param player The player entity which should be removed from the world
     */
    public void removePlayer( EntityPlayer player ) {
	    Vector position = player.getPosition();
	    int currentChunkX = CoordinateUtils.fromBlockToChunk( (int) position.getX() );
	    int currentChunkZ = CoordinateUtils.fromBlockToChunk( (int) position.getZ() );

        ChunkAdapter chunkAdapter = this.getChunk( currentChunkX, currentChunkZ );
        if ( chunkAdapter != null ) {
            chunkAdapter.removePlayer( player );
        }

	    this.players.remove( player );
    }

	/**
	 * Spawns the given entity at the specified position.
	 *
	 * @param entity The entity to spawn
	 * @param positionX The x coordinate to spawn the entity at
	 * @param positionY The y coordinate to spawn the entity at
	 * @param positionZ The z coordinate to spawn the entity at
	 */
	public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ ) {
		this.entityManager.spawnEntityAt( entity, positionX, positionY, positionZ );
	}

	/**
	 * Spawns the given entity at the specified position with the specified rotation.
	 *
	 * @param entity The entity to spawn
	 * @param positionX The x coordinate to spawn the entity at
	 * @param positionY The y coordinate to spawn the entity at
	 * @param positionZ The z coordinate to spawn the entity at
	 * @param yaw The yaw value of the entity ; will be applied to both the entity's body and head
	 * @param pitch The pitch value of the entity
	 */
	public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ, float yaw, float pitch ) {
		this.entityManager.spawnEntityAt( entity, positionX, positionY, positionZ, yaw, pitch );
	}

	/**
	 * Finds an entity by its ID. If the entity is not found null is returned.
	 *
	 * @param entityId The unique ID of the entity
	 * @return The entity or null if not found
	 */
	public Entity findEntity( long entityId ) {
		return this.entityManager.findEntity( entityId );
	}

	/**
	 * Despawns an entity given its id.
	 *
	 * @param entityId The ID of the entity to despawn
	 */
	public void despawnEntity( long entityId ) {
		this.entityManager.despawnEntity( entityId );
	}

	// ==================================== UPDATING ==================================== //

	/**
	 * Ticks the world and updates what needs to be updated.
	 *
	 * @param currentTimeMS The current time in milliseconds. Used to reduce the number of calls to System#currentTimeMillis()
     * @param dT The delta from the full second which has been calculated in the last tick
	 */
    public void update( long currentTimeMS, float dT ) {
        // ---------------------------------------
        // Tick the chunk cache to get rid of Chunks
        this.chunkCache.tick( currentTimeMS );

        // ---------------------------------------
        // Chunk packages are done in main thread in order to be able to
        // cache packets without possibly getting into race conditions:
        if ( !this.chunkPackageTasks.isEmpty() ) {
            // One chunk per tick at max:
            AsyncChunkPackageTask task = this.chunkPackageTasks.poll();
            ChunkAdapter chunk = this.getChunk( task.getX(), task.getZ() );
            if ( chunk == null ) {
                final Object lock = new Object();

                this.getOrLoadChunk( task.getX(), task.getZ(), false, new Delegate<ChunkAdapter>() {
                    @Override
                    public void invoke( ChunkAdapter arg ) {
                        synchronized ( lock ) {
                            packageChunk( arg, task.getCallback() );
                            lock.notifyAll();
                        }
                    }
                } );

                // Wait until the chunk is loaded
                synchronized ( lock ) {
                    try {
                        lock.wait();
                    } catch ( InterruptedException e ) {
                        // Ignored .-.
                    }
                }
            } else {
                packageChunk( chunk, task.getCallback() );
            }
        }

         // ---------------------------------------
        // Perform regular updates:
    }

	// ==================================== CHUNK MANAGEMENT ==================================== //

	/**
	 * Gets the chunk at the specified coordinates. If the chunk is currently not available
	 * it will be loaded or generated.
	 *
	 * @param x The x-coordinate of the chunk
	 * @param z The z-coordinate of the chunk
	 * @return The chunk if available or null otherwise
	 */
    public ChunkAdapter getChunk( int x, int z ) {
        return this.chunkCache.getChunk( x, z );
    }

	/**
	 * Gets a chunk asynchronously. This allows to load or generate the chunk if it is not yet available
	 * and then return it once it gets available. The callback is guaranteed to be invoked: if the chunk
	 * could not be loaded nor be generated it will be passed null as its argument.
	 *
	 * @param x The x-coordinate of the chunk
	 * @param z The z-coordinate of the chunk
	 * @param generate Whether or not to generate teh chunk if it does not yet exist
	 * @param callback The callback to be invoked once the chunk is available
	 */
    public void getOrLoadChunk( int x, int z, boolean generate, Delegate<ChunkAdapter> callback ) {
        // Early out:
        ChunkAdapter chunk = this.chunkCache.getChunk( x, z );
        if ( chunk != null ) {
            callback.invoke( chunk );
            return;
        }

        // Schedule this chunk for asynchronous loading:
        AsyncChunkLoadTask task = new AsyncChunkLoadTask( x, z, generate, callback );
        this.asyncChunkTasks.offer( task );
    }

    /**
     * Send a chunk of this world to the client
     *
     * @param x The x-coordinate of the chunk
     * @param z The z-coordinate of the chunk
     * @param player The player we want to send the chunk to
     */
    public void sendChunk( int x, int z, EntityPlayer player ) {
        Delegate2<Long, Packet> sendDelegate = new Delegate2<Long, Packet>() {
            @Override
            public void invoke( Long chunkHash, Packet chunkPacket ) {
                player.getConnection().sendWorldChunk( chunkHash, chunkPacket );
            }
        };

        this.getOrLoadChunk( x, z, true, new Delegate<ChunkAdapter>() {
            @Override
            public void invoke( ChunkAdapter chunk ) {
                chunk.packageChunk( sendDelegate );
            }
        } );
    }

    /**
     * Move a player to a new chunk. This is done so we know which player is in which chunk so we can unload unneeded
     * Chunks better and faster.
     *
     * @param prevX The x-coordinate of the chunk the player was previously in
     * @param prevZ The x-coordinate of the chunk the player was previously in
     * @param x The x-coordinate of the chunk
     * @param z The z-coordinate of the chunk
     * @param player The player which should be set into the chunk
     */
    public void movePlayerToChunk( int prevX, int prevZ, int x, int z, EntityPlayer player ) {
        ChunkAdapter oldChunk = this.getChunk( prevX, prevZ );
        getOrLoadChunk( x, z, true, new Delegate<ChunkAdapter>() {
            @Override
            public void invoke( ChunkAdapter newChunk ) {
                if ( oldChunk == null ) {
                    newChunk.addPlayer( player );
                } else if ( !oldChunk.equals( newChunk ) ) {
                    oldChunk.removePlayer( player );
                    newChunk.addPlayer( player );
                }
            }
        } );
    }

    /**
     * Saves the given chunk to its region file asynchronously.
     *
     * @param chunk The chunk to save
     */
    public void saveChunkAsynchronously( ChunkAdapter chunk ) {
        AsyncChunkSaveTask task = new AsyncChunkSaveTask( chunk );
        this.asyncChunkTasks.add( task );
    }

	/**
	 * Load a Chunk from the underlying implementation
	 *
	 * @param x         The x coordinate of the chunk we want to load
	 * @param z         The x coordinate of the chunk we want to load
	 * @param generate  A boolean which decides whether or not the chunk should be generated when not found
	 * @return The loaded or generated Chunk
	 */
	protected abstract ChunkAdapter loadChunk( int x, int z, boolean generate );

	/**
	 * Saves the given chunk to its respective region file. The respective region file
	 * is created automatically if it does not yet exist.
	 *
	 * @param chunk The chunk to be saved
	 */
	protected abstract void saveChunk( ChunkAdapter chunk );

	/**
	 * Prepares the region surrounding the world's spawn point.
	 *
	 * @throws IOException Throws in case the spawn region could not be loaded nor generated
	 */
	protected void prepareSpawnRegion() throws IOException {
		final int spawnRadius = this.server.getServerConfig().getAmountOfChunksForSpawnArea() * 16;
		if ( spawnRadius == 0 ) return;

		final int minBlockX = (int) (this.spawn.getX() - spawnRadius);
		final int minBlockZ = (int) (this.spawn.getZ() - spawnRadius);
		final int maxBlockX = (int) (this.spawn.getX() + spawnRadius);
		final int maxBlockZ = (int) (this.spawn.getZ() + spawnRadius);

		final int minChunkX = CoordinateUtils.fromBlockToChunk( minBlockX );
		final int minChunkZ = CoordinateUtils.fromBlockToChunk( minBlockZ );
		final int maxChunkX = CoordinateUtils.fromBlockToChunk( maxBlockX );
		final int maxChunkZ = CoordinateUtils.fromBlockToChunk( maxBlockZ );

		for ( int i = minChunkZ; i <= maxChunkZ; ++i ) {
			for ( int j = minChunkX; j <= maxChunkX; ++j ) {
				ChunkAdapter chunk = this.loadChunk( j, i, true );
				if ( chunk == null ) {
					throw new IOException( "Failed to load / generate chunk surrounding spawn region" );
				}
			}
		}
	}

    /**
     * Notifies the world that the given chunk was told to package itself. This will effectively
     * produce an asynchronous chunk task which will be completed by the asynchronous worker thread.
     *
     * @param x        The x coordinate of the chunk we want to package
     * @param z        The z coordinate of the chunk we want to package
     * @param callback The callback to be invoked once the chunk is packaged
     */
    void notifyPackageChunk( int x, int z, Delegate2<Long, Packet> callback ) {
        AsyncChunkPackageTask task = new AsyncChunkPackageTask( x, z, callback );
        this.chunkPackageTasks.add( task );
    }

    /**
     * Package a Chunk into a ChunkData Packet for Raknet. This is done to enable caching of those packets.
     *
     * @param chunk     The chunk which should be packed
     * @param callback  The callback which should be invoked when the packing has been done
     */
    void packageChunk( ChunkAdapter chunk, Delegate2<Long, Packet> callback ) {
        PacketWorldChunk packet = chunk.createPackagedData();

        PacketBuffer buffer = new PacketBuffer( packet.estimateLength() + 1 );
        buffer.writeByte( packet.getId() );
        packet.serialize( buffer );

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream( bout );

        try {
            dout.writeInt( buffer.getPosition() );
            dout.write( buffer.getBuffer(), buffer.getBufferOffset(), buffer.getPosition() - buffer.getBufferOffset() );
            dout.flush();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        this.deflater.reset();
        this.deflater.setInput( bout.toByteArray() );
        this.deflater.finish();

        bout.reset();
        byte[] intermediate = new byte[1024];
        while ( !this.deflater.finished() ) {
            int read = this.deflater.deflate( intermediate );
            bout.write( intermediate, 0, read );
        }

        PacketBatch batch = new PacketBatch();
        batch.setPayload( bout.toByteArray() );

        try {
            dout.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        this.deflater.reset();

        chunk.setCachedPacket( batch );
        callback.invoke( CoordinateUtils.toLong( chunk.getX(), chunk.getZ() ), batch );
    }

	// ==================================== WORKER THREAD ==================================== //

    /**
     * Starts the asynchronous worker thread used by the world to perform I/O operations for chunks.
     */
    private void startAsyncWorker( ExecutorService executorService ) {
        executorService.execute( new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName( Thread.currentThread().getName() + " [Async World I/O: " + WorldAdapter.this.getWorldName() + "]" );
                WorldAdapter.this.asyncWorkerLoop();
            }
        } );

        this.asyncWorkerRunning = true;
    }

    /**
     * Main loop of the world's asynchronous worker thread.
     */
    private void asyncWorkerLoop() {
        while ( this.asyncWorkerRunning ) {
            try {
                AsyncChunkTask task = this.asyncChunkTasks.poll( 500, TimeUnit.MILLISECONDS );
                if ( task == null ) continue;

                ChunkAdapter chunk;
                switch ( task.getType() ) {
                    case LOAD:
                        AsyncChunkLoadTask load = (AsyncChunkLoadTask) task;
                        chunk = this.loadChunk( load.getX(), load.getZ(), load.isGenerate() );
                        load.getCallback().invoke( chunk );
                        break;

                    case SAVE:
                        AsyncChunkSaveTask save = (AsyncChunkSaveTask) task;
                        chunk = save.getChunk();
                        this.saveChunk( chunk );
                        break;

                    default:
                        // Log some error when this happens

                        break;
                }
            } catch ( Throwable cause ) {
                // Catching throwable in order to make sure no uncaught exceptions puts
                // the asynchronous worker into nirvana:

            }
        }
    }

}
