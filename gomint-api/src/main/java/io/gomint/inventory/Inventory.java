/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.inventory;

/**
 * @author geNAZt
 * @version 1.0
 */
public interface Inventory {

    /**
     * Gets the item out of this inventory
     *
     * @param slot  The slot which we want to lookup
     * @return The itemstack or null if the slot is empty
     */
    ItemStack getContent( int slot );

    /**
     * Set the item into the slot of this inventory
     *
     * @param slot      The slot in which we want to set this item
     * @param itemStack The item which we want to set into that slot
     */
    void setContent( int slot, ItemStack itemStack );

    /**
     * Get the size of this inventory
     *
     * @return The size of this inventory
     */
    int size();

}
