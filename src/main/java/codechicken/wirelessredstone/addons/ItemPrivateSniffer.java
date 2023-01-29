package codechicken.wirelessredstone.addons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemPrivateSniffer extends Item {

    public ItemPrivateSniffer() {
        setMaxStackSize(1);
    }

    @SideOnly(Side.CLIENT)
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer) {
        if (world.isRemote) {
            WirelessRedstoneAddons.proxy.openPSnifferGui(entityplayer);
            RedstoneEtherAddons.client().addSniffer(entityplayer);
        }
        return itemstack;
    }
}
