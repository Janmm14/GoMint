/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.entity;

import io.gomint.math.Location;
import io.gomint.server.entity.metadata.MetadataContainer;
import io.gomint.server.world.WorldAdapter;
import io.gomint.world.World;

import lombok.Getter;

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


    private Location location;

    /**
     * Construct a new Entity
     *
     * @param type The type of the Entity
     * @param world The world in which this entity is in
     */
	Entity( EntityType type, WorldAdapter world ) {
		this.id = ENTITY_ID.incrementAndGet();
		this.type = type;
		this.location = world.getSpawnLocation().clone();
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
	 *♥
	 * @return The world of this entity
	 */
	public World getWorld() {
		return this.location.getWorld();
	}

	/**
	 * Gets a metadata container containing all metadata values of this entity.
	 *
	 * @return This entity's metadata
	 */
	public MetadataContainer getMetadata() {
		return new MetadataContainer();
	}

}
