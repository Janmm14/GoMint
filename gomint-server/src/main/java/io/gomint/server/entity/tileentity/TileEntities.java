/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.entity.tileentity;

import io.gomint.server.world.WorldAdapter;
import io.gomint.taglib.NBTTagCompound;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * @author geNAZt
 * @version 1.0
 */
public enum TileEntities {

    /**
     * Represents a sign. This TileEntity holds 4 lines of strings to be displayed on a piece of wood
     */
    SIGN( "Sign", SignTileEntity.class ),

    /**
     * Represents a chest. This TileEntity holds a inventory which can hold up to 27 itemstacks
     */
    CHEST( "Chest", ChestTileEntity.class );

    private final String nbtID;
    private MethodHandle tileEntityConstructor;

    /**
     * Construct a new TileEntity enum value
     *
     * @param nbtID             The ID which can be found in NBT Tags
     * @param tileEntityClass   The class which should be used to instantiate the TileEntity
     */
    TileEntities( String nbtID, Class<? extends TileEntity> tileEntityClass ) {
        this.nbtID = nbtID;

        try {
            this.tileEntityConstructor = MethodHandles.lookup().unreflectConstructor( tileEntityClass.getConstructor( NBTTagCompound.class, WorldAdapter.class ) );
        } catch ( IllegalAccessException | NoSuchMethodException e ) {
            e.printStackTrace();
            this.tileEntityConstructor = null;
        }
    }

    /**
     * Construct a new TileEntity which then reads in the data from the given Compound
     *
     * @param compound  The compound from which the data should be read
     * @param world     The world in which this TileEntity resides
     * @return The constructed and ready to use TileEntity or null
     */
    public static TileEntity construct( NBTTagCompound compound, WorldAdapter world ) {
	    // Check if compound has a id
	    String id = compound.getString( "id", null );
	    if ( id == null ) {
		    return null;
	    }

	    // Search for correct tile entity
	    for ( TileEntities tileEntities : values() ) {
		    if ( tileEntities.nbtID.equals( id ) ) {
			    try {
				    return (TileEntity) tileEntities.tileEntityConstructor.invoke( compound, world );
			    } catch ( Throwable throwable ) {
				    throwable.printStackTrace();
				    return null;
			    }

		    }
	    }

        return null;
    }

}
