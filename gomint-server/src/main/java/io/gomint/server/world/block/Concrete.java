package io.gomint.server.world.block;

import io.gomint.inventory.item.ItemStack;
import io.gomint.server.world.block.helper.ToolPresets;
import io.gomint.world.block.BlockType;

import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:concrete" )
public class Concrete extends Block implements io.gomint.world.block.BlockConcrete {

    @Override
    public String getBlockId() {
        return "minecraft:concrete";
    }

    @Override
    public long getBreakTime() {
        return 2700;
    }

    @Override
    public float getBlastResistance() {
        return 9.0f;
    }

    @Override
    public BlockType getType() {
        return BlockType.CONCRETE;
    }

    @Override
    public boolean canBeBrokenWithHand() {
        return true;
    }

    @Override
    public Class<? extends ItemStack>[] getToolInterfaces() {
        return ToolPresets.PICKAXE;
    }

}
