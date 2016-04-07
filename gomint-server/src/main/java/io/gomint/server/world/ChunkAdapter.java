/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.math.MathUtils;
import io.gomint.server.async.Delegate2;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.entity.tileentity.TileEntity;
import io.gomint.server.network.packet.Packet;
import io.gomint.server.network.packet.PacketBatch;
import io.gomint.server.network.packet.PacketWorldChunk;
import io.gomint.server.util.Color;
import io.gomint.server.util.DumpUtil;
import io.gomint.taglib.NBTTagCompound;
import io.gomint.world.Biome;
import io.gomint.world.Block;
import io.gomint.world.Chunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public abstract class ChunkAdapter implements Chunk {

	// CHECKSTYLE:OFF
	// World
	protected WorldAdapter world;

	// Networking
	protected boolean               dirty;
	protected SoftReference<Packet> cachedPacket;

	// Chunk
	protected int x;
	protected int z;
	protected long inhabitedTime;

	// Biomes
	protected byte[] biomes = new byte[16 * 16];
	protected byte[] biomeColors = new byte[16 * 16 * 3];

	// Blocks
	protected byte[]      blocks     = new byte[PEWorldConstraints.BLOCKS_PER_CHUNK];
	protected NibbleArray data       = new NibbleArray( PEWorldConstraints.BLOCKS_PER_CHUNK );
	protected NibbleArray blockLight = new NibbleArray( PEWorldConstraints.BLOCKS_PER_CHUNK );
	protected NibbleArray skyLight   = new NibbleArray( PEWorldConstraints.BLOCKS_PER_CHUNK );
	protected NibbleArray height     = new NibbleArray( 16 * 16 );

	// Players / Chunk GC
	protected List<EntityPlayer> players = new ArrayList<>();
	protected long lastPlayerOnThisChunk;
	protected long loadedTime;
	protected long lastSavedTimestamp;

	// TileEntities
	protected List<TileEntity> tileEntities = new ArrayList<>();
	// CHECKSTYLE:ON

    /**
     * Add a player to this chunk. This is needed to know when we can GC a chunk
     *
     * @param player The player which we want to add to this chunk
     */
    public void addPlayer( EntityPlayer player ) {
	    this.players.add( player );
    }

    /**
     * Remove a player from this chunk. This is needed to know when we can GC a chunk
     *
     * @param player The player which we want to remove from this chunk
     */
    public void removePlayer( EntityPlayer player ) {
	    this.players.remove( player );
	    this.lastPlayerOnThisChunk = System.currentTimeMillis();
    }

	/**
	 * Remove the dirty state for the chunk and set the batched packet to the
	 * cache.
	 *
	 * @param batch     The batch which has been generated to be sent to the clients
	 */
	public void setCachedPacket( PacketBatch batch ) {
		this.dirty = false;
		this.cachedPacket = new SoftReference<>( batch );
	}


	/**
	 * Gets the time at which this chunk was last written out to disk.
	 *
	 * @return The timestamp this chunk was last written out at
	 */
	public long getLastSavedTimestamp() {
		return this.lastSavedTimestamp;
	}

	/**
	 * Sets the timestamp on which this chunk was last written out to disk.
	 *
	 * @param timestamp The timestamp to set
	 */
	public void setLastSavedTimestamp( long timestamp ) {
		this.lastSavedTimestamp = timestamp;
	}

	/**
	 * Recalculates the biome colors of the chunk by interpolating between
	 * each block's adjacent biomes. This operation is expensive so use with
	 * care!
	 */
	public void calculateBiomeColors() {
		for ( int i = 0; i < 16; ++i ) {
			for ( int k = 0; k < 16; ++k ) {
				Color average = this.averageColorsRGB(
						this.getBiomeColorRaw( i, k ),
						this.getBiomeColorRaw( i, k - 1 ),
						this.getBiomeColorRaw( i, k + 1 ),
						this.getBiomeColorRaw( i - 1, k ),
						this.getBiomeColorRaw( i - 1, k - 1 ),
						this.getBiomeColorRaw( i - 1, k + 1 ),
						this.getBiomeColorRaw( i + 1, k ),
						this.getBiomeColorRaw( i + 1, k - 1 ),
						this.getBiomeColorRaw( i + 1, k + 1 )
				);

				this.setBiomeColorRGB( i, k, average.getR(), average.getG(), average.getB() );
			}
		}
	}

	/**
	 * Gets the raw biome color from a block's biome.
	 *
	 * @return The block's raw biome color
	 */
	private int getBiomeColorRaw( int x, int z ) {
		// Fix bad parameters:
		int xClamped = MathUtils.clamp( x, 0, 15 );
		int zClamped = MathUtils.clamp( z, 0, 15 );

		Biome biome = this.getBiome( xClamped, zClamped );
		if ( biome != null ) {
			return biome.getColorRGB( true, 0 );
		} else {
			throw new IllegalStateException( "Corrupt chunk: Block column has unknown biome <" + this.biomes[( xClamped << 4 ) | zClamped] + ">" );
		}
	}

	// ==================================== MANIPULATION ==================================== //

	/**
	 * Makes a request to package this chunk asynchronously. The package that will be
	 * given to the provided callback will be a world chunk packet inside a batch packet.
	 * <p>
	 * This operation is done asynchronously in order to limit how many chunks are being
	 * packaged in parallel as well as to cache some chunk packets.
	 *
	 * @param callback The callback to be invoked once the operation is complete
	 */
	public void packageChunk( Delegate2<Long, Packet> callback ) {
		if ( !this.dirty && this.cachedPacket != null ) {
			Packet packet = this.cachedPacket.get();
			if ( packet != null ) {
				callback.invoke( CoordinateUtils.toLong( x, z ), packet );
			} else {
				this.world.notifyPackageChunk( x, z, callback );
			}
		} else {
			this.world.notifyPackageChunk( x, z, callback );
		}
	}

	/**
	 * Checks if this chunk can be gced
	 *
	 * @return true when it can be gced, false when not
	 */
	public boolean canBeGCed() {
		int secondsAfterLeft = this.world.getServer().getServerConfig().getSecondsUntilGCAfterLastPlayerLeft();
		int waitAfterLoad = this.world.getServer().getServerConfig().getWaitAfterLoadForGCSeconds();

		return System.currentTimeMillis() - this.loadedTime > TimeUnit.SECONDS.toMillis( waitAfterLoad ) &&
		       this.players.isEmpty() &&
		       System.currentTimeMillis() - this.lastPlayerOnThisChunk > TimeUnit.SECONDS.toMillis( secondsAfterLeft );
	}

	/**
	 * Return a collection of players which are currently on this chunk
	 *
	 * @return non modifiable collection of players on this chunk
	 */
	public Collection<EntityPlayer> getPlayers() {
		return Collections.unmodifiableCollection( this.players );
	}

	/**
	 * Gets the x-coordinate of the chunk.
	 *
	 * @return The chunk's x-coordinate
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * Gets the z-coordinate of the chunk.
	 *
	 * @return The chunk's z-coordinate
	 */
	public int getZ() {
		return this.z;
	}

	/**
	 * Sets the ID of a block at the specified coordinates given in chunk coordinates.
	 *
	 * @param x  The x-coordinate of the block
	 * @param y  The y-coordinate of the block
	 * @param z  The z-coordinate of the block
	 * @param id The ID to set the block to
	 */
	public void setBlock( int x, int y, int z, int id ) {
		this.blocks[( x * 2048 ) + ( z * 128 ) + y] = (byte) id;
		this.dirty = true;
	}

	/**
	 * Sets the ID of a block at the specified coordinates given in chunk coordinates.
	 *
	 * @param x The x-coordinate of the block
	 * @param y The y-coordinate of the block
	 * @param z The z-coordinate of the block
	 * @return The ID of the block
	 */
	public byte getBlock( int x, int y, int z ) {
		return this.blocks[( x * 2048 ) + ( z * 128 ) + y];
	}

	/**
	 * Sets the metadata value of the block at the specified coordinates.
	 *
	 * @param x    The x-coordinate of the block
	 * @param y    The y-coordinate of the block
	 * @param z    The z-coordinate of the block
	 * @param data The data value to set
	 */
	public void setData( int x, int y, int z, byte data ) {
		this.data.set( ( x * 2048 ) + ( z * 128 ) + y, data );
		this.dirty = true;
	}

	/**
	 * Gets the metadata value of the block at the specified coordinates.
	 *
	 * @param x The x-coordinate of the block
	 * @param y The y-coordinate of the block
	 * @param z The z-coordinate of the block
	 * @return The data value of the block
	 */
	public byte getData( int x, int y, int z ) {
		return this.data.get( ( x * 2048 ) + ( z * 128 ) + y );
	}

	/**
	 * Sets the maximum block height at a specific coordinate-pair.
	 *
	 * @param x      The x-coordinate relative to the chunk
	 * @param z      The z-coordinate relative to the chunk
	 * @param height The maximum block height
	 */
	private void setHeight( int x, int z, byte height ) {
		this.height.set( ( x << 4 ) + z, height );
		this.dirty = true;
	}

	/**
	 * Gets the maximum block height at a specific coordinate-pair. Requires the height
	 * map to be up-to-date.
	 *
	 * @param x The x-coordinate relative to the chunk
	 * @param z The z-coordinate relative to the chunk
	 * @return The maximum block height
	 */
	public byte getHeight( int x, int z ) {
		return this.height.get( ( x << 4 ) + z );
	}

	/**
	 * Sets the lighting value of the specified block
	 *
	 * @param x     The x-coordinate of the block
	 * @param y     The y-coordinate of the block
	 * @param z     The z-coordinate of the block
	 * @param value The lighting value
	 */
	protected void setBlockLight( int x, int y, int z, byte value ) {
		this.blockLight.set( ( x * 2048 ) + ( z * 128 ) + y, value );
		this.dirty = true;
	}

	/**
	 * Gets the lighting value of the specified block
	 *
	 * @param x The x-coordinate of the block
	 * @param y The y-coordinate of the block
	 * @param z The z-coordinate of the block
	 * @return The block's lighting value
	 */
	public byte getBlockLight( int x, int y, int z ) {
		return this.blockLight.get( ( x * 2048 ) + ( z * 128 ) + y );
	}

	/**
	 * Sets the skylight value of the specified block
	 *
	 * @param x     The x-coordinate of the block
	 * @param y     The y-coordinate of the block
	 * @param z     The z-coordinate of the block
	 * @param value The lighting value
	 */
	protected void setSkyLight( int x, int y, int z, byte value ) {
		this.skyLight.set( ( x * 2048 ) + ( z * 128 ) + y, value );
		this.dirty = true;
	}

	/**
	 * Gets the skylight value of the specified block
	 *
	 * @param x The x-coordinate of the block
	 * @param y The y-coordinate of the block
	 * @param z The z-coordinate of the block
	 * @return The block's lighting value
	 */
	public byte getSkyLight( int x, int y, int z ) {
		return this.skyLight.get( ( x * 2048 ) + ( z * 128 ) + y );
	}

	/**
	 * Sets a block column's biome.
	 *
	 * @param x     The x-coordinate of the block column
	 * @param z     The z-coordinate of the block column
	 * @param biome The biome to set
	 */
	protected void setBiome( int x, int z, Biome biome ) {
		this.biomes[( x << 4 ) + z] = (byte) biome.getId();
		this.dirty = true;
	}

	/**
	 * Gets a block column's biome.
	 *
	 * @param x The x-coordinate of the block column
	 * @param z The z-coordinate of the block column
	 * @return The block column's biome
	 */
	public Biome getBiome( int x, int z ) {
		return Biome.getBiomeById( this.biomes[( x << 4 ) + z] );
	}

	/**
	 * Gets the RGB color of the block column at the specified coordinates.
	 *
	 * @param x The x-coordinate of the block column
	 * @param z The z-coordinate of the block column
	 * @param r The red to set
	 * @param g The green to set
	 * @param b The blue to set
	 */
	public void setBiomeColorRGB( int x, int z, byte r, byte g, byte b ) {
		int basisIndex = ( x << 4 ) + z;

		this.biomeColors[basisIndex * 3] = r;
		this.biomeColors[basisIndex * 3 + 1] = g;
		this.biomeColors[basisIndex * 3 + 2] = b;

		this.dirty = true;
	}

	/**
	 * Gets the RGB color of the block column at the specified coordinates.
	 *
	 * @param x The x-coordinate of the block column
	 * @param z The z-coordinate of the block column
	 * @return The block column's color
	 */
	public int getBiomeColorRGB( int x, int z ) {
		int basisIndex = ( x << 4 ) + z;

		byte r = this.biomeColors[basisIndex * 3];
		byte g = this.biomeColors[basisIndex * 3 + 1];
		byte b = this.biomeColors[basisIndex * 3 + 2];

		return ( r << 16 ) | ( g << 8 ) | b;
	}

	@Override
	public Block getBlockAt( int x, int y, int z ) {
		return null;
	}

	// ==================================== MISCELLANEOUS ==================================== //

	/**
	 * Averages multiple RGB colors linearly into one combined color.
	 *
	 * @param colors All colors to combine
	 * @return The combined color
	 */
	public Color averageColorsRGB( int... colors ) {
		byte r = 0, g = 0, b = 0;

		for ( int color : colors ) {
			r += color >> 16 & 0xff;
			g += color >> 8 & 0xff;
			b += color & 0xff;
		}

		r /= colors.length;
		g /= colors.length;
		b /= colors.length;

		return new Color( r, g, b );
	}

	/**
	 * Recalculates the height map of the chunk.
	 */
	public void calculateHeightmap() {
		for ( int i = 0; i < 16; ++i ) {
			for ( int k = 0; k < 16; ++k ) {
				for ( int j = PEWorldConstraints.MAX_BUILD_HEIGHT - 1; j > 0; --j ) {
					if ( this.getBlock( i, j, k ) != 0 ) {
						this.setHeight( i, k, (byte) j );
						break;
					}
				}
			}
		}
	}

	/**
	 * Invoked by the world's asynchronous worker thread once the chunk is supposed
	 * to actually pack itself into a world chunk packet.
	 *
	 * @return The world chunk packet that is to be sent
	 */
	PacketWorldChunk createPackagedData() {
		PacketBuffer buffer = new PacketBuffer( this.blocks.length +
		                                        this.data.raw().length +
		                                        this.blockLight.raw().length +
		                                        this.skyLight.raw().length +
		                                        this.height.length() +
		                                        ( this.biomeColors.length << 2 ) +
		                                        4 );

		// Preparations:
		this.calculateBiomeColors();

		buffer.writeBytes( this.blocks );
		buffer.writeBytes( this.data.raw() );
		buffer.writeBytes( this.blockLight.raw() );
		buffer.writeBytes( this.skyLight.raw() );
		buffer.writeBytes( this.height.toByteArray() );

		for ( int i = 0; i < this.biomes.length; ++i ) {
			byte r = this.biomeColors[i * 3];
			byte g = this.biomeColors[i * 3 + 1];
			byte b = this.biomeColors[i * 3 + 2];

			int color = ( r << 16 ) | ( g << 8 ) | b;

			buffer.writeInt( ( this.biomes[i] << 24 ) | ( color & 0x00FFFFFF ) );
		}

		buffer.writeInt( 0 );

		// TileEntity Data?
		if ( this.tileEntities.size() > 0 ) {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			for ( TileEntity tileEntity : this.tileEntities ) {
				NBTTagCompound nbtTagCompound = new NBTTagCompound( "" );
				tileEntity.toCompund( nbtTagCompound );

				try {
					nbtTagCompound.writeTo( byteArrayOutputStream, false, ByteOrder.LITTLE_ENDIAN );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}

			buffer.writeBytes( byteArrayOutputStream.toByteArray() );
		}

		PacketWorldChunk packet = new PacketWorldChunk();
		packet.setX( this.x );
		packet.setZ( this.z );
		packet.setData( buffer.getBuffer() );
		return packet;
	}
}
