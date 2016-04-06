/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.entity;

import io.gomint.math.Location;
import io.gomint.math.Vector;
import io.gomint.server.entity.metadata.MetadataContainer;
import io.gomint.server.world.WorldAdapter;
import io.gomint.world.World;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public abstract class Entity {

	private static final AtomicLong ENTITY_ID = new AtomicLong( 0 );

    /**
     * The id of this entity
     */
	protected final long          id;
    /**
     * Type of the entity
     */
	protected final EntityType   type;

	protected WorldAdapter world;

	// Movement and rotation related fields:
	protected float positionX;
	protected float positionY;
	protected float positionZ;

	protected float yaw;
	protected float headYaw;
	protected float pitch;

    /**
     * Construct a new Entity
     *
     * @param type The type of the Entity
     * @param world The world in which this entity is in
     */
	Entity( EntityType type, WorldAdapter world ) {
		this.id = ENTITY_ID.incrementAndGet();
		this.type = type;
		this.world = world;
	}

	/**
	 * Gets this entity's unique ID.
	 *
	 * @return The entitiy's unique ID
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * Gets the type of this entity.
	 *
	 * @return The type of this entity
	 */
	public EntityType getType() {
		return this.type;
	}

	/**
	 * Gets the world of this entity.
	 *
	 * @return The world of this entity
	 */
	public World getWorld() {
		return this.world;
	}

	/**
	 * Gets the location of this entity.
	 *
	 * @return The location of this entity
	 */
	public Location getLocation() {
		return new Location( this.world, this.positionX, this.positionY, this.positionZ, this.yaw, this.pitch );
	}

	/**
	 * Sets the position of the entity. If the specified position is far apart from
	 * the the entity's previous position it will be treated as a teleport.
	 *
	 * @param positionX The x coordinate of the position to put the entity to
	 * @param positionY The y coordinate of the position to put the entity to
	 * @param positionZ The z coordinate of the position to put the entity to
	 */
	public void setPosition( float positionX, float positionY, float positionZ ) {
		this.positionX = positionX;
		this.positionY = positionY;
		this.positionZ = positionZ;
	}

	/**
	 * Sets the position of the entity. If the specified position is far apart from
	 * the the entity's previous position it will be treated as a teleport.
	 *
	 * @param position The position to put the entity to
	 */
	public void setPosition( Vector position ) {
		this.positionX = position.getX();
		this.positionY = position.getY();
		this.positionZ = position.getZ();
	}

	/**
	 * Gets the current position of the entity.
	 *
	 * @return The entity's current position
	 */
	public Vector getPosition() {
		return new Vector( this.positionX, this.positionY, this.positionZ );
	}

	/**
	 * Sets the yaw value of the entity's body.
	 *
	 * @param yaw The yaw value of the entity's body
	 */
	public void setYaw( float yaw ) {
		this.yaw = yaw;
	}

	/**
	 * Gets the yaw value of the entity's body.
	 *
	 * @return The yaw value of the entity's body
	 */
	public float getYaw() {
		return this.yaw;
	}

	/**
	 * Sets the yaw value of the entity's head.
	 *
	 * @param headYaw The yaw value of the entity's head
	 */
	public void setHeadYaw( float headYaw ) {
		this.headYaw = headYaw;
	}

	/**
	 * Gets the yaw value of the entity's head.
	 *
	 * @return The yaw value of the entity's head
	 */
	public float getHeadYaw() {
		return this.headYaw;
	}

	/**
	 * Sets the pitch of the entity.
	 *
	 * @param pitch The pitch to set
	 */
	public void setPitch( float pitch ) {
		this.pitch = pitch;
	}

	/**
	 * Gets the pitch of the entity.
	 *
	 * @return The entity's pitch
	 */
	public float getPitch() {
		return this.pitch;
	}

	/**
	 * Gets a metadata container containing all metadata values of this entity.
	 *
	 * @return This entity's metadata
	 */
	public MetadataContainer getMetadata() {
		return new MetadataContainer();
	}

	/**
	 * Updates the entity.
	 *
	 * @param currentTimeMS The current system time in milliseconds
	 * @param dT The time that has passed since the last tick in 1/s
	 */
	public void update( long currentTimeMS, float dT ) {

	}

}
