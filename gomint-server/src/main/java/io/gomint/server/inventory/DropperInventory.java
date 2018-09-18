/*
 * Copyright (c) 2018, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.inventory;

import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.network.PlayerConnection;
import io.gomint.server.network.type.WindowType;

/**
 * @author geNAZt
 * @version 1.0
 */
public class DropperInventory extends ContainerInventory {

    public DropperInventory( InventoryHolder owner ) {
        super( owner, 9 );
    }

    @Override
    public WindowType getType() {
        return WindowType.DISPENSER;
    }

    @Override
    public void onOpen( EntityPlayer player ) {

    }

    @Override
    public void onClose( EntityPlayer player ) {

    }

    @Override
    public void sendContents( PlayerConnection playerConnection ) {

    }

    @Override
    public void sendContents( int slot, PlayerConnection playerConnection ) {

    }

}
