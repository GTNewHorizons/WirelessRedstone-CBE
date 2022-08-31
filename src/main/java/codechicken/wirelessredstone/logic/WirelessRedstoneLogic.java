package codechicken.wirelessredstone.logic;

import codechicken.wirelessredstone.core.WirelessRedstoneCore;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.item.Item;

@Mod(
        modid = "WR-CBE|Logic",
        dependencies = "required-after:WR-CBE|Core;required-after:ForgeMultipart",
        acceptedMinecraftVersions = "[1.7.10]",
        name = "WR-CBE Logic",
        version = WirelessRedstoneCore.version)
public class WirelessRedstoneLogic {
    public static Item itemwireless;

    @SidedProxy(
            clientSide = "codechicken.wirelessredstone.logic.WRLogicClientProxy",
            serverSide = "codechicken.wirelessredstone.logic.WRLogicProxy")
    public static WRLogicProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {}

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
