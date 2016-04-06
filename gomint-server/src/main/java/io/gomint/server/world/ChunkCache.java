/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world;

import io.gomint.math.Vector;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.util.IntPair;

import net.openhft.koloboke.collect.LongCursor;
import net.openhft.koloboke.collect.map.LongObjMap;
import net.openhft.koloboke.collect.map.hash.HashLongObjMaps;
import net.openhft.koloboke.collect.set.LongSet;
import net.openhft.koloboke.collect.set.hash.HashLongSets;

import java.util.Map;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class ChunkCache {

	// CHECKSTYLE:OFF
	// ==================================== FIELDS ==================================== //
	private final WorldAdapter              world;
    private final LongObjMap<ChunkAdapter> cachedChunks;
    private       boolean                enableAutoSave;
    private       long                   autoSaveInterval;


	public ChunkCache( WorldAdapter world ) {
		this.world = world;
		this.cachedChunks = HashLongObjMaps.newMutableMap();
        this.enableAutoSave = world.getServer().getServerConfig().isAutoSave();
        this.autoSaveInterval = world.getServer().getServerConfig().getAutoSaveInterval();
	}
	// CHECKSTYLE:ON

    /**
     * Ticking for Chunk GC
     *
     * @param currentTimeMS The current time in milliseconds. Used to reduce the number of calls to System#currentTimeMillis()
     */
    public void tick( long currentTimeMS ) {
        int spawnXChunk = CoordinateUtils.fromBlockToChunk( (int) this.world.getSpawnLocation().getX() );
        int spawnZChunk = CoordinateUtils.fromBlockToChunk( (int) this.world.getSpawnLocation().getZ() );

        int spawnAreaSize = this.world.getServer().getServerConfig().getAmountOfChunksForSpawnArea();

        // Check for gc
        synchronized ( this.cachedChunks ) {
            // Calculate the hashes which are used by players viewdistances
            LongSet viewDistanceSet = HashLongSets.newMutableSet();
            for ( EntityPlayer player : this.world.getPlayers() ) {
                int          viewDistance = player.getViewDistance();
                Vector       position     = player.getPosition();
                int currentChunkX = CoordinateUtils.fromBlockToChunk( (int) position.getX() );
                int currentChunkZ = CoordinateUtils.fromBlockToChunk( (int) position.getZ() );

                for ( int x = currentChunkX - viewDistance; x < currentChunkX + viewDistance; x++ ) {
                    for ( int z = currentChunkZ - viewDistance; z < currentChunkZ + viewDistance; z++ ) {
                        viewDistanceSet.add( CoordinateUtils.toLong( x, z ) );
                    }
                }
            }

            // Copy over the current loaded chunk hashes
            LongCursor longs = HashLongSets.newMutableSet( this.cachedChunks.keySet() ).cursor();
            while ( longs.moveNext() ) {
                long chunkHash = longs.elem();

                ChunkAdapter chunk = this.cachedChunks.get( chunkHash );
                if ( chunk == null ) {
                    continue;
                }

                boolean readyForGC = false;
                if ( !viewDistanceSet.contains( chunkHash ) ) {
                    // Skip if this chunk is a spawn chunk
                    boolean isSpawnChunk = false;
                    IntPair intPair = CoordinateUtils.toIntPair( chunkHash );
                    if ( this.world.getServer().getServerConfig().getAmountOfChunksForSpawnArea() > 0 ) {
                        if ( intPair.getX() <= spawnXChunk + spawnAreaSize && intPair.getZ() <= spawnZChunk + spawnAreaSize ) {
                            isSpawnChunk = true;
                        }
                    }

                    // Ask this chunk if he wants to be gced
                    if ( !isSpawnChunk && chunk.canBeGCed() ) {
                        readyForGC = true;
                    }
                }

                if ( readyForGC || chunk.getLastSavedTimestamp() + this.autoSaveInterval < currentTimeMS ) {
                    this.world.saveChunkAsynchronously( chunk );
                    chunk.setLastSavedTimestamp( currentTimeMS );
                }

                // Ask this chunk if he wants to be gced
                if ( readyForGC ) {
                    this.cachedChunks.remove( chunkHash );
                }
            }
        }
    }

    // ==================================== CHUNK CACHE ==================================== //

    /**
     * Checks whether or not the specified chunk is currently in cache.
     *
     * @param x The x-coordinate of the chunk
     * @param z The z-coordinate of the chunk
     * @return Whether or not the specified chunk is currently in cache
     */
    public boolean hasChunk( int x, int z ) {
        synchronized ( this.cachedChunks ) {
            return this.cachedChunks.containsKey( CoordinateUtils.toLong( x, z ) );
        }
    }

    /**
     * Attempts to get the specified chunk.
     *
     * @param x The x-coordinate of the chunk
     * @param z The z-coordinate of the chunk
     * @return The chunk if it is loaded or null otherwise
     */
    public ChunkAdapter getChunk( int x, int z ) {
        synchronized ( this.cachedChunks ) {
            long chunkHash = CoordinateUtils.toLong( x, z );
            return this.cachedChunks.get( chunkHash );
        }
    }



    /**
     * Puts the specified chunk into the cache thus making it available to the outside
     *
     * @param chunk The chunk to put into the cache
     */
    public void putChunk( ChunkAdapter chunk ) {
        synchronized ( this.cachedChunks ) {
            this.cachedChunks.put( CoordinateUtils.toLong( chunk.getX(), chunk.getZ() ), chunk );
        }
    }

    // ==================================== AUTOSAVE ==================================== //

    /**
     * Checks whether or not the auto-save feature is enabled.
     *
     * @return Whether or not the auto-save feature is enabled
     */
    public boolean isAutosaveEnabled() {
        return this.enableAutoSave;
    }

    /**
     * Gets the interval in milliseconds in which chunks get saved automatically.
     *
     * @return The auto-save interval
     */
    public long getAutoSaveInterval() {
        return this.autoSaveInterval;
    }
}
