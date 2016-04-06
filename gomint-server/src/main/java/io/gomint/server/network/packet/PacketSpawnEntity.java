/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.server.entity.EntityType;
import io.gomint.server.entity.metadata.MetadataContainer;
import io.gomint.server.network.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
@EqualsAndHashCode( callSuper = false )
public class PacketSpawnEntity extends Packet {

	private long              entityId;
	private EntityType        entityType;
	private float             x;
	private float             y;
	private float             z;
	private float             velocityX;
	private float             velocityY;
	private float             velocityZ;
	private float             yaw;
	private float             headYaw;
	private MetadataContainer metadata;

	public PacketSpawnEntity() {
		super( Protocol.PACKET_SPAWN_ENTITY );
	}

	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeLong( this.entityId );
		buffer.writeInt( this.entityType.getId() );
		buffer.writeFloat( this.x );
		buffer.writeFloat( this.y );
		buffer.writeFloat( this.z );
		buffer.writeFloat( this.velocityX );
		buffer.writeFloat( this.velocityY );
		buffer.writeFloat( this.velocityZ );
		buffer.writeFloat( this.yaw );
		buffer.writeFloat( this.headYaw );
		this.metadata.serialize( buffer );
		buffer.writeShort( (short) 0 );             // Unknown use
	}

	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.entityId = buffer.readLong();
		this.entityType = EntityType.getByID( buffer.readInt() );
		this.x = buffer.readFloat();
		this.y = buffer.readFloat();
		this.z = buffer.readFloat();
		this.velocityX = buffer.readFloat();
		this.velocityY = buffer.readFloat();
		this.velocityZ = buffer.readFloat();
		this.yaw = buffer.readFloat();
		this.headYaw = buffer.readFloat();
		this.metadata = new MetadataContainer();
		this.metadata.deserialize( buffer );
		buffer.skip( 2 );                           // Unknown use
	}
}
