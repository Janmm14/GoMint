/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.math;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * A mutable class representing a triple of float values resembling
 * the x, y and z coordinates of a point respectively.
 * </p>
 *
 * @author Digot
 * @author geNAZt
 * @author BlackyPaw
 * @version 1.2
 */
public class Vector implements Cloneable {
    public static final Vector ZERO = new Vector( 0, 0, 0 );

    @Getter @Setter protected float x;
    @Getter @Setter protected float y;
    @Getter @Setter protected float z;

	public Vector() {

	}

    public Vector( float x, float y, float z ) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector add( float x, float y, float z ) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

	public Vector add( Vector v ) {
		this.x += v.x;
		this.y += v.y;
		this.z += v.z;

		return this;
	}

    public Vector subtract( float x, float y, float z ) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

	public Vector subtract( Vector v ) {
		this.x -= v.x;
		this.y -= v.y;
		this.z -= v.z;

		return this;
	}

    public Vector multiply( float x, float y, float z ) {
        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

	public Vector multiply( Vector v ) {
		this.x *= v.x;
		this.y *= v.y;
		this.z *= v.z;

		return this;
	}

    public Vector divide( float x, float y, float z ) {
        this.x /= x;
        this.y /= y;
        this.z /= z;

        return this;
    }

	public Vector divide( Vector v ) {
		this.x /= v.x;
		this.y /= v.y;
		this.z /= v.z;

		return this;
	}

    public Vector multiply( float value ) {
        this.x *= value;
        this.y *= value;
        this.z *= value;

        return this;
    }

    @Override
    public String toString() {
        return String.format( "[x=%.3f, y=%.3f, z=%.3f]", this.x, this.y, this.z );
    }

    @Override
    public Vector clone() {
        try {
            Vector vector = (Vector) super.clone();
            vector.x = this.x;
            vector.y = this.y;
            vector.z = this.z;
            return vector;
        } catch ( CloneNotSupportedException e ) {
            throw new AssertionError( "Failed to clone vector!" );
        }
    }
}
