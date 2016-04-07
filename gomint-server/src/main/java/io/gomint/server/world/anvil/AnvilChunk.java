/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world.anvil;

import io.gomint.server.world.ChunkAdapter;
import io.gomint.server.world.NibbleArray;
import io.gomint.server.world.PEWorldConstraints;
import io.gomint.taglib.NBTStream;
import io.gomint.taglib.NBTStreamListener;
import io.gomint.taglib.NBTTagCompound;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author BlackyPaw
 * @version 1.0
 */
class AnvilChunk extends ChunkAdapter {

    /**
     * Load a Chunk from a NBTTagCompund. This is used when loaded from a Regionfile.
     *
     * @param world     The world in which this Chunk resides
     * @param nbtStream The NBT Stream which reads and emits data from the chunk
     */
    AnvilChunk( AnvilWorldAdapter world, NBTStream nbtStream ) {
        this.world = world;
        this.lastSavedTimestamp = System.currentTimeMillis();
        this.loadedTime = this.lastSavedTimestamp;
        this.loadFromNBT( nbtStream );
        this.dirty = false;
    }

    // ==================================== I/O ==================================== //

	/**
     * Writes the chunk's raw NBT data to the given output stream.
     *
     * @param out The output stream to write the chunk data to.
     * @throws IOException Thrown in case the chunk could not be stored
     */
    void saveToNBT( OutputStream out ) throws IOException {
        NBTTagCompound chunk = new NBTTagCompound( "" );

        NBTTagCompound level = new NBTTagCompound( "Level" );
	    level.addValue( "LightPopulated", (byte) 1 );
	    level.addValue( "TerrainPopulated", (byte) 1 );
	    level.addValue( "V", (byte) 1 );
	    level.addValue( "xPos", this.x );
	    level.addValue( "zPos", this.z );
	    level.addValue( "InhabitedTime", this.inhabitedTime );
	    level.addValue( "LastUpdate", 0L );
	    level.addValue( "Biomes", this.biomes );

	    int[] heightmap = new int[256];
	    for ( int i = 0; i < this.height.length(); ++i ) {
		    heightmap[i] = this.height.get( i );
	    }
	    level.addValue( "HeightMap", heightmap );

	    List<NBTTagCompound> sections = new ArrayList<>( 8 );

	    for ( int sectionY = 0; sectionY < 8; ++sectionY ) {
		    byte[] blocks = new byte[4096];
		    NibbleArray data = new NibbleArray( 4096 );
		    NibbleArray blockLight = new NibbleArray( 4096 );
		    NibbleArray skyLight = new NibbleArray( 4096 );
		    int baseIndex = sectionY * 16;
		    for ( int y = baseIndex; y < baseIndex + 16; ++y ) {
			    for ( int x = 0; x < 16; ++x ) {
				    for ( int z = 0; z < 16; ++z ) {
					    int blockIndex = (y - baseIndex) << 8 | z << 4 | x;

					    byte blockId = this.getBlock( x, y, z );
					    byte blockData = this.getData( x, y, z );

					    blocks[blockIndex] = AnvilBlockConverter.revertBlockID( blockId, blockData );
					    data.set( blockIndex, AnvilBlockConverter.revertBlockData( blockId, blockData ) );
					    blockLight.set( blockIndex, this.getBlockLight( x, y, z ) );
					    skyLight.set( blockIndex, this.getSkyLight( x, y, z ) );
				    }
			    }
		    }

		    NBTTagCompound section = new NBTTagCompound( "" );
		    section.addValue( "Y", (byte) sectionY );
		    section.addValue( "Blocks", blocks );
		    section.addValue( "Data", data.raw() );
		    section.addValue( "BlockLight", blockLight.raw() );
		    section.addValue( "SkyLight", skyLight.raw() );
		    sections.add( section );
	    }

	    level.addValue( "Sections", sections );
	    level.addValue( "Entities", new ArrayList( 0 ) );
	    level.addValue( "TileEntities", new ArrayList( 0 ) );

	    chunk.addChild( level );
	    chunk.writeTo( out, false, ByteOrder.BIG_ENDIAN );
    }

    /**
     * Loads the chunk from the specified NBTTagCompound
     *
     * @param nbtStream The stream which loads the chunk
     */
    private void loadFromNBT( NBTStream nbtStream ) {
        // Fill in default values
        this.biomes = new byte[256];
        Arrays.fill( this.biomes, (byte) -1 );

        // Wait until the nbt stream sends some data
        final int[] oldSectionIndex = new int[]{ -1 };
        final SectionCache section = new SectionCache();

        nbtStream.addListener( new NBTStreamListener() {
            @Override
            public void onNBTValue( String path, Object object ) {
                switch ( path ) {
                    case ".Level.xPos":
                        AnvilChunk.this.x = (int) object;
                        break;
                    case ".Level.zPos":
                        AnvilChunk.this.z = (int) object;
                        break;
                    case ".Level.Biomes":
                        AnvilChunk.this.biomes = (byte[]) object;
                        break;
	                case ".Level.InhabitedTime":
		                AnvilChunk.this.inhabitedTime = (long) object;
		                break;
                    default:
                        if ( path.startsWith( ".Level.Sections" ) ) {
                            // Parse the index
                            String[] split = path.split( "\\." );
                            int sectionIndex = Integer.parseInt( split[3] );

                            // Check if we completed a chunk
                            if ( oldSectionIndex[0] != -1 && oldSectionIndex[0] != sectionIndex ) {
                                // Load and convert this section
                                loadSection( section );

                                // Reset the cache
                                section.setAdd( null );
                                section.setBlockLight( null );
                                section.setBlocks( null );
                                section.setData( null );
                                section.setSkyLight( null );
                                section.setSectionY( 0 );
                            }

                            oldSectionIndex[0] = sectionIndex;

                            // Check what we have got from the chunk
                            switch ( split[4] ) {
                                case "Y":
                                    section.setSectionY( (byte) object << 4 );
                                    break;
                                case "Blocks":
                                    section.setBlocks( (byte[]) object );
                                    break;
                                case "Add":
                                    section.setAdd( new NibbleArray( (byte[]) object ) );
                                    break;
                                case "Data":
                                    section.setData( new NibbleArray( (byte[]) object ) );
                                    break;
                                case "BlockLight":
                                    section.setBlockLight( new NibbleArray( (byte[]) object ) );
                                    break;
                                case "SkyLight":
                                    section.setSkyLight( new NibbleArray( (byte[]) object ) );
                                    break;
                                default:
                                    break;
                            }
                        }
                }
            }
        } );

        // Start parsing the nbt tag
        // CHECKSTYLE:OFF
        try {
            nbtStream.parse();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        // CHECKSTYLE:ON

        // Load last section and calc biome colors
        this.loadSection( section );
        this.calculateBiomeColors();
	    this.calculateHeightmap();
    }

    /**
     * Loads a chunk section from its raw NBT data.
     *
     * @param section The section to be loaded
     */
    private void loadSection( SectionCache section ) {
        int sectionY = section.getSectionY();
        byte[] blocks = section.getBlocks();
        NibbleArray add = section.getAdd();
        NibbleArray data = section.getData();
        NibbleArray blockLight = section.getBlockLight();
        NibbleArray skyLight = section.getSkyLight();

        if ( blocks == null || data == null || blockLight == null || skyLight == null ) {
            throw new IllegalArgumentException( "Corrupt chunk: Section is missing obligatory compounds" );
        }

        for ( int j = 0; j < 16; ++j ) {
            for ( int i = 0; i < 16; ++i ) {
                for ( int k = 0; k < 16; ++k ) {
                    int y = sectionY + j;
                    if ( y < 0 || y >= PEWorldConstraints.MAX_BUILD_HEIGHT ) {
                        continue;
                    }

                    int blockIndex = j << 8 | k << 4 | i;

                    int blockId = ( add != null ? add.get( blockIndex ) << 8 : 0 ) | blocks[blockIndex];
                    byte blockData = data.get( blockIndex );

                    blockId = AnvilBlockConverter.convertBlockID( blockId, blockData );
                    blockData = AnvilBlockConverter.convertBlockData( blockId, blockData );

                    // Fix water & lava at the bottom of a chunk
                    if ( y == 0 && ( blockId == 8 || blockId == 9 || blockId == 10 || blockId == 11 ) ) {
                        blockId = 7;
                        blockData = 0;
                    }

                    this.setBlock( i, y, k, blockId );
                    this.setData( i, y, k, blockData );
                    this.setBlockLight( i, y, k, blockLight.get( blockIndex ) );
                    this.setSkyLight( i, y, k, skyLight.get( blockIndex ) );
                }
            }
        }
    }

}
