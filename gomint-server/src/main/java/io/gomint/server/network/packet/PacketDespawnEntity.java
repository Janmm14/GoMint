/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.server.network.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
@EqualsAndHashCode( callSuper = false )
public class PacketDespawnEntity extends Packet {

	private long entityId;

	public PacketDespawnEntity() {
		super( Protocol.PACKET_DESPAWN_ENTITY );
	}

	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeLong( this.entityId );
	}

	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.entityId = buffer.readLong();
	}
}
