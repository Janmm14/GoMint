/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world.leveldb;

import io.gomint.server.entity.tileentity.TileEntities;
import io.gomint.server.entity.tileentity.TileEntity;
import io.gomint.server.util.DumpUtil;
import io.gomint.server.world.ChunkAdapter;
import io.gomint.server.world.NibbleArray;
import io.gomint.server.world.WorldAdapter;
import io.gomint.taglib.NBTTagCompound;

import com.google.common.base.Strings;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/**
 * @author geNAZt
 * @version 1.0
 */
public class LevelDBChunk extends ChunkAdapter {

    public LevelDBChunk( WorldAdapter worldAdapter, int x, int z, byte[] chunkData, byte[] tileEntityData, byte[] entityData ) {
        this.world = worldAdapter;
        this.x = x;
        this.z = z;

        // Load chunk
        this.loadChunk( chunkData );

	    // Load TileEntities
        if ( tileEntityData != null && tileEntityData.length > 0 ) {
            this.loadTileEntity( tileEntityData );
        }

	    // Load Entities
	    if ( entityData != null && entityData.length > 0 ) {
		    this.loadEntity( entityData );
	    }

        this.calculateBiomeColors();
        this.calculateHeightmap();
    }

	private void loadEntity( byte[] entityData ) {
		try ( ByteArrayInputStream inputStream = new ByteArrayInputStream( entityData ) ) {
			while ( inputStream.available() > 0 ) {
				NBTTagCompound nbtTagCompound = NBTTagCompound.readFrom( inputStream, false, ByteOrder.LITTLE_ENDIAN );
				// DumpUtil.dumpNBTCompund( nbtTagCompound );
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	private void loadTileEntity( byte[] tileEntityData ) {
        try ( ByteArrayInputStream inputStream = new ByteArrayInputStream( tileEntityData ) ) {
            while ( inputStream.available() > 0 ) {
                NBTTagCompound nbtTagCompound = NBTTagCompound.readFrom( inputStream, false, ByteOrder.LITTLE_ENDIAN );
	            TileEntity tileEntity = TileEntities.construct( nbtTagCompound, world );
	            if ( tileEntity != null ) {
					tileEntities.add( tileEntity );
		            // System.out.println( "TileEntity @ " + tileEntity.getLocation() );
	            } else {
		            System.out.println( "Found new TileEntity: \n");
		            DumpUtil.dumpNBTCompund( nbtTagCompound );
		            System.out.println( "-----------------");
	            }
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private void loadChunk( byte[] chunkData ) {
        // Wrap data and read blocks
        ByteBuffer buffer = ByteBuffer.wrap( chunkData ).order( ByteOrder.BIG_ENDIAN );
        buffer.get( this.blocks );

        // Read in data
        byte[] data = new byte[16384];
        buffer.get( data );
        this.data = new NibbleArray( data );

        // Read in skylight
        byte[] skyLight = new byte[16384];
        buffer.get( skyLight );
        this.skyLight = new NibbleArray( skyLight );

        // Read in blocklight
        byte[] blockLight = new byte[16384];
        buffer.get( blockLight );
        this.blockLight = new NibbleArray( blockLight );

        // Read in height
        for ( int i = 0; i < 256; i++ ) {
            this.height.set( i, buffer.get() );
        }

        // Read in color
        int c = 0;
        for ( int i = 0; i < 256; i++ ) {
            int biome = buffer.getInt();
            Color color = new Color( biome );
            this.biomes[i] = (byte) ( biome >> 24 );
            this.biomeColors[c++] = (byte) color.getRed();
            this.biomeColors[c++] = (byte) color.getGreen();
            this.biomeColors[c++] = (byte) color.getBlue();
        }
    }

    /**
     * Calculates the needed data to be saved back to the database
     *
     * @return The data which will be saved in the database for this chunk
     */
    byte[] getSaveData() {
        ByteBuffer byteBuffer = ByteBuffer.allocate( this.blocks.length +
                this.data.raw().length +
                this.skyLight.raw().length +
                this.blockLight.raw().length +
                this.height.length() +
                256 );

        byteBuffer.put( this.blocks );
        byteBuffer.put( this.data.raw() );
        byteBuffer.put( this.skyLight.raw() );
        byteBuffer.put( this.blockLight.raw() );
        byteBuffer.put( this.height.toByteArray() );

        for ( int i = 0; i < 256; i++ ) {
            byte r = this.biomeColors[i * 3];
            byte g = this.biomeColors[i * 3 + 1];
            byte b = this.biomeColors[i * 3 + 2];

            int color = ( r << 16 ) | ( g << 8 ) | b;

            byteBuffer.putInt( ( this.biomes[i] << 24 ) | ( color & 0x00FFFFFF ) );
        }

        return byteBuffer.array();
    }
}
