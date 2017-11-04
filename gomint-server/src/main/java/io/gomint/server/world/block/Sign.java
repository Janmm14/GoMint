package io.gomint.server.world.block;

import io.gomint.server.entity.tileentity.SignTileEntity;
import io.gomint.server.entity.tileentity.TileEntity;
import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;
import io.gomint.world.block.BlockSign;

import java.util.ArrayList;
import java.util.List;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 63 )
public class Sign extends Block implements BlockSign {

    @Override
    public int getBlockId() {
        return 63;
    }

    @Override
    public long getBreakTime() {
        return 1500;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean needsTileEntity() {
        return true;
    }

    @Override
    TileEntity createTileEntity( NBTTagCompound compound ) {
        return new SignTileEntity( compound, this.world );
    }

    @Override
    public List<String> getLines() {
        SignTileEntity sign = this.getTileEntity();
        return new ArrayList<>( sign.getLines() );
    }

    @Override
    public void setLine( int line, String content ) {
        // Silenty fail when line is incorrect
        if ( line > 4 || line < 1 ) {
            return;
        }

        SignTileEntity sign = this.getTileEntity();
        sign.getLines().set( line - 1, content );
        this.updateBlock();
    }

    @Override
    public String getLine( int line ) {
        // Silenty fail when line is incorrect
        if ( line > 4 || line < 1 ) {
            return "";
        }

        SignTileEntity sign = this.getTileEntity();
        return sign.getLines().get( line - 1 );
    }

}
