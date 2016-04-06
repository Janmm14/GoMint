/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world;

import io.gomint.jraknet.PacketReliability;
import io.gomint.server.entity.Entity;
import io.gomint.server.network.packet.PacketSpawnEntity;
import net.openhft.koloboke.collect.map.LongObjMap;
import net.openhft.koloboke.collect.map.hash.HashLongObjMaps;

/**
 * Helper class that manages all entities inside a world.
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class EntityManager {

	private final WorldAdapter world;

	private LongObjMap<Entity> entitiesById;

	public EntityManager( WorldAdapter world ) {
		this.world = world;
		this.entitiesById = HashLongObjMaps.newMutableMap();
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

	/**
	 * Spawns the given entity at the specified position with the specified rotation.
	 *
	 * @param entity The entity to spawn
	 * @param positionX The x coordinate to spawn the entity at
	 * @param positionY The y coordinate to spawn the entity at
	 * @param positionZ The z coordinate to spawn the entity at
	 * @param yaw The yaw value of the entity ; will be applied to both the entity's body and head
	 * @param pitch The pitch value of the entity
	 */
	public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ, float yaw, float pitch ) {
		entity.setPosition( positionX, positionY, positionZ );
		entity.setYaw( yaw );
		entity.setHeadYaw( yaw );
		entity.setPitch( pitch );
		this.entitiesById.put( entity.getId(), entity );

		// Broadcast spawn entity packet:
		PacketSpawnEntity packet = new PacketSpawnEntity();
		packet.setEntityId( entity.getId() );
		packet.setEntityType( entity.getType() );
		packet.setX( positionX );
		packet.setY( positionY );
		packet.setZ( positionZ );
		packet.setVelocityX( 0.0F );
		packet.setVelocityY( 0.0F );
		packet.setVelocityZ( 0.0F );
		packet.setYaw( yaw );
		packet.setHeadYaw( yaw );
		packet.setMetadata( entity.getMetadata() );
		this.world.broadcast( PacketReliability.RELIABLE, 0, packet );
	}

}
