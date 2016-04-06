/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.entity;

/**
 * An enumeration of possible entity types. In fact entity types do not only consist of an incremental
 * type ID that is unique for the respective entity but also several flags that hint at further information
 * about the entity in question. These flags are:
 * <p>
 * Bit 0 - 7 :  Incremental Entity Type ID<br/>
 * Bit 8     :  Whether or not the entity is alive
 * Bit 9     :  Whether or not the entity has got an AI
 * Bit 10    :  Whether or not the entity is friendly towards players
 * Bit 11    :  Whether or not the entity is hostile / a mob
 * Bit 12    :  Whether or not the entity is an animal
 * Bit 13    :  Whether or not the entity is capable of swimming
 * Bit 14    :  Whether or not the entity may be tamed
 * Bit 15    :  Whether or not the entity is able to fly (only used for bats not for ghasts nor blazes)
 * Bit 16    :  Whether or not the entity is undead
 * Bit 17    :  Whether or not the entity burns in daylight
 * Bit 18    :  Whether or not the entity is capable of climbing
 * Bit 19    :  Whether or not the entity is a vehicle (only true for Minecarts and NOT for boats)
 *
 * The remaining bits do not seem to have been used as of now.
 *
 * @author BlackyPaw
 * @version 1.0
 */
public enum EntityType {

	/**
	 * Entity Type value for a chicken.
	 */
	CHICKEN( 0x00170A ),

	/**
	 * Entity Type value for a cow.
	 */
	COW( 0x00170B ),

	/**
	 * Entity Type value for a pig.
	 */
	PIG( 0x00170C ),

	/**
	 * Entity Type value for a sheep.
	 */
	SHEEP( 0x00170D ),

	/**
	 * Entity Type value for a wolf.
	 */
	WOLF( 0x00570E ),

	/**
	 * Entity Type value for a villager.
	 */
	VILLAGER( 0x00070F ),

	/**
	 * Entity Type value for a mushroom cow.
	 */
	MUSHROOM_COW( 0x001710 ),

	/**
	 * Entity Type value for a squid.
	 */
	SQUID( 0x002711 ),

	/**
	 * Entity Type value for a rabbit.
	 */
	RABBIT( 0x001712 ),

	/**
	 * Entity Type value for a bat.
	 */
	BAT( 0x008113 ),

	/**
	 * Entity Type value for an iron golem.
	 */
	IRON_GOLEM( 0x000314 ),

	/**
	 * Entity Type value for a snow golem.
	 */
	SNOW_GOLEM( 0x000315 ),

	/**
	 * Entity Type value for a witch.
	 */
	WITCH( 0x000B2D ),

	/**
	 * Entity Type value for a zombie.
	 */
	ZOMBIE( 0x30B20 ),

	/**
	 * Entity Type value for a creeper.
	 */
	CREEPER( 0x000B21 ),

	/**
	 * Entity Type value for a skeleton.
	 */
	SKELETON( 0x010B22 ),

	/**
	 * Entity Type value for a spider.
	 */
	SPIDER( 0x040B23 ),

	/**
	 * Entity Type value for a pig zombie.
	 */
	PIG_ZOMBIE( 0x010B24 ),

	/**
	 * Entity Type value for a slime.
	 */
	SLIME( 0x000B25 ),

	/**
	 * Entity Type value for an enderman.
	 */
	ENDERMAN( 0x000B26 ),

	/**
	 * Entity Type value for a silverfish.
	 */
	SILVERFISH( 0x040B27 ),

	/**
	 * Entity Type value for a cavespider.
	 */
	CAVESPIDER( 0x040B28 ),

	/**
	 * Entity Type value for a ghast.
	 */
	GHAST( 0x000B29 ),

	/**
	 * Entity Type value for a lava slime.
	 */
	LAVA_SLIME( 0x000B2A ),

	/**
	 * Entity Type value for a blaze.
	 */
	BLAZE( 0x000B2B ),

	/**
	 * Entity Type value for a zombified villager.
	 */
	ZOMBIE_VILLAGER( 0x30B2C ),

	/**
	 * Entity Type value for a player.
	 */
	PLAYER( 0x00013F ),

	/**
	 * Entity Type value for a fishing hook floating on water.
	 */
	FISHING_HOOK( 0x00004D ),

	/**
	 * Entity Type value for an arrow.
	 */
	ARROW( 0x000050 ),

	/**
	 * Entity Type value for a snowball which has been thrown.
	 */
	SNOWBALL( 0x000051 ),

	/**
	 * Entity Type value for an egg which has been thrown.
	 */
	THROWN_EGG( 0x000052 ),

	/**
	 * Entity Type value for a painting hanging on a wall.
	 */
	PAINTING( 0x000053 ),

	/**
	 * Entity Type value for a rideable minecart.
	 */
	MINECART_RIDEABLE( 0x080054 ),

	/**
	 * Entity Type value for a large fireball as thrown by ghasts.
	 */
	LARGE_FIREBALL( 0x000055 ),

	/**
	 * Entity Type value for a potion which has been thrown.
	 */
	THROWN_POTION( 0x000056 ),

	/**
	 * Entity Type value for a rideable boat.
	 */
	BOAT_RIDEABLE( 0x00005A ),

	/**
	 * Entity Type value for a lightning strike.
	 */
	LIGHTNING( 0x00005D ),

	/**
	 * Entity Type value for a small fireball as thrown by blazes.
	 */
	SMALL_FIREBALL( 0x00005E );

	private final int id;

    /**
     * New enum value for EntityType
     *
     * @param id The MC:PE id of this EntityType
     */
	EntityType( int id ) {
		this.id = id;
	}

	/**
	 * Gets the complete ID of this entity type.
	 *
	 * @return The complete ID of this entity type
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Checks whether or not the entity associated with this entity type is alive.
	 *
	 * @return Whether or not entities of this entity type are alive
	 */
	public boolean isAlive() {
		return ( this.id & 0x100 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type has got AI
	 * behaviour attached to itself.
	 *
	 * @return Whether or not entities of this entity type have got an AI behaviour
	 */
	public boolean hasAI() {
		return ( this.id & 0x200 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type behave
	 * friendly towards players.
	 *
	 * @return Whether or not entities of this entity type behave friendly towards players
	 */
	public boolean isFriendly() {
		return ( this.id & 0x400 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type is a mob, i.e.
	 * behaves aggressive towards players.
	 *
	 * @return Whether or not entities of this entity type are mobs
	 */
	public boolean isMob() {
		return ( this.id & 0x800 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type is an animal.
	 *
	 * @return Whether or not entities of this entity type are animals
	 */
	public boolean isAnimal() {
		return ( this.id & 0x1000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type can swim.
	 *
	 * @return Whether or not entities of this entity type can swim
	 */
	public boolean canSwim() {
		return ( this.id & 0x2000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type may be tamed.
	 *
	 * @return Whether or not entities of this entity type may be tamed
	 */
	public boolean isTameable() {
		return ( this.id & 0x4000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type can fly.
	 * However, this flag is only set for bats and not for blazes nor ghasts as
	 * one could assume.
	 *
	 * @return Whether or not entities of this entity type can fly
	 */
	public boolean canFly() {
		return ( this.id & 0x8000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type is an undead monster.
	 *
	 * @return Whether or not entities of this entity type are undead monsters
	 */
	public boolean isUndead() {
		return ( this.id & 0x10000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type burns in daylight.
	 *
	 * @return Whether or not entities of this entity type burn in daylight
	 */
	public boolean burnsInDaylight() {
		return ( this.id & 0x20000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type can climb.
	 *
	 * @return Whether or not entities of this entity type can climb
	 */
	public boolean canClimb() {
		return ( this.id & 0x40000 ) != 0;
	}

	/**
	 * Checks whether or not the entity associated with this entity type is a vehicle.
	 * However, this flag is only set for rideable minecarts and not for boats.
	 *
	 * @return Whether or not entities of this entity type are vehicles
	 */
	public boolean isVehicle() {
		return ( this.id & 0x80000 ) != 0;
	}

}
