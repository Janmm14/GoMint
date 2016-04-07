/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.entity;

import io.gomint.server.world.WorldAdapter;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class EntityCow extends Entity {
	
	/**
	 * Constructs a new entity
	 *
	 * @param world The world in which this entity is in
	 */
	public EntityCow( WorldAdapter world ) {
		super( EntityType.COW, world );
	}
}
