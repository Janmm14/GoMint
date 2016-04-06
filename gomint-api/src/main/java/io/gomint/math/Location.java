/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.math;

import io.gomint.world.World;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * A Location defines a point a world with three coordinates relative to a
 * specific world it is placed in.
 * </p>
 *
 * @author Digot
 * @author geNAZt
 * @author BlackyPaw
 * @version 1.2
 */
public class Location extends Vector implements Cloneable {

	@Getter
	@Setter
	private World world;

	public Location( World world, double x, double y, double z ) {
		super( x, y, z );
		this.world = world;
	}

	public Location( World world, Vector vector ) {
		super( vector.getX(), vector.getY(), vector.getZ() );
		this.world = world;
	}

	@Override
	public Location add( double x, double y, double z ) {
		super.add( x, y, z );
		return this;
	}

	@Override
	public Location subtract( double x, double y, double z ) {
		super.subtract( x, y, z );
		return this;
	}

	@Override
	public Location multiply( double x, double y, double z ) {
		super.multiply( x, y, z );
		return this;
	}

	@Override
	public Location divide( double x, double y, double z ) {
		super.divide( x, y, z );
		return this;
	}

	@Override
	public Location scalar( float scalar ) {
		super.scalar( scalar );
		return this;
	}

	@Override
	public String toString() {
		return String.format( "[world=%s, x=%.3f, y=%.3f, z=%.3f]", world.getLevelName(), x, y, z );
	}

	@Override
	public Location clone() {
		Location location = (Location) super.clone();
		location.world = this.world;
		return location;
	}
}
