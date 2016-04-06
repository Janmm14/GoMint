/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world;

import io.gomint.server.entity.Entity;

/**
 * Helper class that manages all entites inside a world.
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class EntityManager {

	private final WorldAdapter world;

	public EntityManager( WorldAdapter world ) {
		this.world = world;
	}

	/**
	 * Spawns the given entity at the specified position.
	 *
	 * @param entity The entity to spawn
	 * @param positionX The x coordinate to spawn the entity at
	 * @param positionY The y coordinate to spawn the entity at
	 * @param positionZ The z coordinate to spawn the entity at
	 */
	public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ ) {
		this.spawnEntityAt( entity, positionX, positionY, positionZ, 0.0F, 0.0F );
	}


	public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ, float yaw, float pitch ) {
		entity.setPosition( positionX, positionY, positionZ );
	}

}
