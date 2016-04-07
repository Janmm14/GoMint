/*
 * Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.inventory;

import io.gomint.inventory.Inventory;
import io.gomint.inventory.ItemStack;

/**
 * @author geNAZt
 * @version 1.0
 */
public class ChestInventory implements Inventory {

    private ItemStack[] content = new ItemStack[27];

    @Override
    public ItemStack getContent( int slot ) {
        return ( slot > size() ) ? null : this.content[slot];
    }

    @Override
    public void setContent( int slot, ItemStack itemStack ) {
        if ( slot > size() ) return;
        this.content[slot] = itemStack;
    }

    @Override
    public int size() {
        return content.length;
    }

}
