/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.network;

import io.gomint.server.network.packet.Packet;
import io.gomint.server.network.packet.PacketA8;
import io.gomint.server.network.packet.PacketB9;
import io.gomint.server.network.packet.PacketBatch;
import io.gomint.server.network.packet.PacketDespawnEntity;
import io.gomint.server.network.packet.PacketSpawnEntity;
import io.gomint.server.network.packet.PacketText;
import io.gomint.server.network.packet.PacketCraftingRecipes;
import io.gomint.server.network.packet.PacketEntityMetadata;
import io.gomint.server.network.packet.PacketEntityMotion;
import io.gomint.server.network.packet.PacketEntityMovement;
import io.gomint.server.network.packet.PacketLogin;
import io.gomint.server.network.packet.PacketMovePlayer;
import io.gomint.server.network.packet.PacketPlayState;
import io.gomint.server.network.packet.PacketSetChunkRadius;
import io.gomint.server.network.packet.PacketWorldChunk;
import io.gomint.server.network.packet.PacketWorldInitialization;
import io.gomint.server.network.packet.PacketWorldTime;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public final class Protocol {

	// ========================================= PACKET IDS ========================================= //
	// CHECKSTYLE:OFF
	public static final byte PACKET_LOGIN                = (byte) 0x8F;
	public static final byte PACKET_PLAY_STATE           = (byte) 0x90;
	public static final byte PACKET_BATCH                = (byte) 0x92;
	public static final byte PACKET_TEXT                 = (byte) 0x93;
	public static final byte PACKET_WORLD_TIME           = (byte) 0x94;
	public static final byte PACKET_WORLD_INITIALIZATION = (byte) 0x95;
	public static final byte PACKET_SPAWN_ENTITY         = (byte) 0x98;
	public static final byte PACKET_DESPAWN_ENTITY       = (byte) 0x99;
	public static final byte PACKET_ENTITY_MOVEMENT      = (byte) 0x9C;
	public static final byte PACKET_MOVE_PLAYER          = (byte) 0x9D;
	public static final byte PACKET_A8                   = (byte) 0xA8;
	public static final byte PACKET_ENTITY_METADATA      = (byte) 0xAD;
	public static final byte PACKET_ENTITY_MOTION        = (byte) 0xAE;
	public static final byte PACKET_SET_COMPASS_TARGET   = (byte) 0xB1;
	public static final byte PACKET_B9                   = (byte) 0xB9;
	public static final byte PACKET_CRAFTING_RECIPES     = (byte) 0xBA;
	public static final byte PACKET_WORLD_CHUNK          = (byte) 0xBF;
	public static final byte PACKET_SET_CHUNK_RADIUS     = (byte) 0xC8;
	public static final byte PACKET_CONFIRM_CHUNK_RADIUS = (byte) 0xC9;
	// CHECKSTYLE:ON

	// ========================================= PACKET METHODS ========================================= //

	/**
	 * Creates a new packet instance given the packet ID found inside the first byte of any
	 * packet's data.
	 *
	 * @param id The ID of the the packet to create
	 *
	 * @return The created packet or null if it could not be created
	 */
	public static Packet createPacket( byte id ) {
		switch ( id ) {
			case PACKET_LOGIN:
				return new PacketLogin();

			case PACKET_PLAY_STATE:
				return new PacketPlayState();

			case PACKET_BATCH:
				return new PacketBatch();

			case PACKET_TEXT:
				return new PacketText();

			case PACKET_WORLD_TIME:
				return new PacketWorldTime();

			case PACKET_WORLD_INITIALIZATION:
				return new PacketWorldInitialization();

			case PACKET_SPAWN_ENTITY:
				return new PacketSpawnEntity();

			case PACKET_DESPAWN_ENTITY:
				return new PacketDespawnEntity();

			case PACKET_ENTITY_MOVEMENT:
				return new PacketEntityMovement();

			case PACKET_MOVE_PLAYER:
				return new PacketMovePlayer();

			case PACKET_A8:
				return new PacketA8();

			case PACKET_ENTITY_METADATA:
				return new PacketEntityMetadata();

			case PACKET_ENTITY_MOTION:
				return new PacketEntityMotion();

			case PACKET_B9:
				return new PacketB9();

			case PACKET_CRAFTING_RECIPES:
				return new PacketCraftingRecipes();

			case PACKET_WORLD_CHUNK:
				return new PacketWorldChunk();

			case PACKET_SET_CHUNK_RADIUS:
				return new PacketSetChunkRadius();

			default:
				return null;
		}
	}


	private Protocol() {
		throw new AssertionError( "Cannot instantiate Protocol!" );
	}

}
