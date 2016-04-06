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

    @Getter @Setter protected double x;
    @Getter @Setter protected double y;
    @Getter @Setter protected double z;

	public Vector() {

	}

    public Vector( double x, double y, double z ) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector add( double x, double y, double z ) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vector subtract( double x, double y, double z ) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vector multiply( double x, double y, double z ) {
        this.x *= x;
        this.y *= y;
        this.z *= z;

        return this;
    }

    public Vector divide( double x, double y, double z ) {
        this.x /= x;
        this.y /= y;
        this.z /= z;

        return this;
    }

    public Vector scalar( float value ) {
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
