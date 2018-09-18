/*
 * Copyright (c) 2018, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world.converter.anvil.tileentity.v1_8;

import io.gomint.math.Location;
import io.gomint.server.entity.tileentity.JukeboxTileEntity;
import io.gomint.server.inventory.item.ItemStack;
import io.gomint.server.inventory.item.Items;
import io.gomint.taglib.NBTTagCompound;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * @author geNAZt
 * @version 1.0
 */
public class RecordPlayerConverter extends BasisConverter<JukeboxTileEntity> {

    public RecordPlayerConverter( Items items, Object2IntMap<String> itemConverter ) {
        super( items, itemConverter );
    }

    @Override
    public JukeboxTileEntity readFrom( NBTTagCompound compound ) {
        // Read position
        Location position = getPosition( compound );
        ItemStack itemStack = getItemStack( compound.getCompound( "RecordItem", false ) );

        return new JukeboxTileEntity( itemStack, position );
    }

}
