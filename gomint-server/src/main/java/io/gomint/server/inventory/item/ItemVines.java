package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 106 )
 public class ItemVines extends ItemStack implements io.gomint.inventory.item.ItemVines {



    @Override
    public String getBlockId() {
        return "minecraft:vine";
    }

    @Override
    public ItemType getType() {
        return ItemType.VINES;
    }

}
