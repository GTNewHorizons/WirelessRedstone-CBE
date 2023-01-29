package codechicken.wirelessredstone.core;

import net.minecraft.command.CommandHandler;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;

import codechicken.core.launch.CodeChickenCorePlugin;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = "WR-CBE|Core",
        version = WirelessRedstoneCore.version,
        dependencies = "required-after:CodeChickenCore@[" + CodeChickenCorePlugin.version
                + ",);required-after:ForgeMultipart",
        acceptedMinecraftVersions = "[1.7.10]",
        name = "WR-CBE Core")
public class WirelessRedstoneCore {

    public static Item obsidianStick;
    public static Item stoneBowl;
    public static Item retherPearl;
    public static Item wirelessTransceiver;
    public static Item blazeTransceiver;
    public static Item recieverDish;
    public static Item blazeRecieverDish;

    public static DamageSource damagebolt;
    public static final String channel = "WRCBE";
    public static final String version = "GRADLETOKEN_VERSION";

    @SidedProxy(
            clientSide = "codechicken.wirelessredstone.core.WRCoreClientProxy",
            serverSide = "codechicken.wirelessredstone.core.WRCoreProxy")
    public static WRCoreProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        CommandHandler commandManager = (CommandHandler) event.getServer().getCommandManager();
        commandManager.registerCommand(new CommandFreq());
    }
}
