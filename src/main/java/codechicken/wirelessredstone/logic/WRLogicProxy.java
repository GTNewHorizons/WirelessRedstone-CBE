package codechicken.wirelessredstone.logic;

import static codechicken.wirelessredstone.logic.WirelessRedstoneLogic.itemwireless;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;

import codechicken.lib.data.MCDataInput;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TMultiPart;
import codechicken.wirelessredstone.core.WirelessRedstoneCore;
import cpw.mods.fml.common.registry.GameRegistry;

public class WRLogicProxy implements IPartFactory2 {

    public void init() {
        MultiPartRegistry.registerPartFactory(this, new String[] { "wrcbe-tran", "wrcbe-recv", "wrcbe-jamm" });

        MultipartGenerator.registerPassThroughInterface("codechicken.wirelessredstone.core.ITileWireless");
        MultipartGenerator.registerPassThroughInterface("codechicken.wirelessredstone.core.ITileReceiver");
        MultipartGenerator.registerPassThroughInterface("codechicken.wirelessredstone.core.ITileJammer");
        // until CC proper integration
        // MultipartGenerator.registerPassThroughInterface("dan200.computer.api.IPeripheral");

        itemwireless = new ItemWirelessPart().setCreativeTab(CreativeTabs.tabRedstone);
        GameRegistry.registerItem(itemwireless, "wirelessLogic");

        addRecipies();
    }

    private static void addRecipies() {
        GameRegistry.addRecipe(
                new ShapedOreRecipe(
                        new ItemStack(itemwireless, 1, 0),
                        "t  ",
                        "srr",
                        "fff",
                        't',
                        WirelessRedstoneCore.wirelessTransceiver,
                        's',
                        "obsidianRod",
                        'f',
                        new ItemStack(Blocks.stone_slab, 1, 0),
                        'r',
                        Items.redstone));

        GameRegistry.addRecipe(
                new ShapedOreRecipe(
                        new ItemStack(itemwireless, 1, 1),
                        "d  ",
                        "srr",
                        "fff",
                        'd',
                        WirelessRedstoneCore.recieverDish,
                        's',
                        "obsidianRod",
                        'f',
                        new ItemStack(Blocks.stone_slab, 1, 0),
                        'r',
                        Items.redstone));

        GameRegistry.addRecipe(
                new ItemStack(itemwireless, 1, 2),
                "p  ",
                "srr",
                "fff",
                'p',
                WirelessRedstoneCore.blazeTransceiver,
                's',
                Items.blaze_rod,
                'f',
                new ItemStack(Blocks.stone_slab, 1, 0),
                'r',
                Items.redstone);
    }

    @Override
    public TMultiPart createPart(String name, NBTTagCompound nbt) {
        return newPart(name);
    }

    @Override
    public TMultiPart createPart(String name, MCDataInput packet) {
        return newPart(name);
    }

    // Matches the legacy IPartFactory adapter, which answered previews with the client part.
    @Override
    public TMultiPart createPartForClientPreview(String name, NBTTagCompound nbt) {
        return newPart(name);
    }

    private TMultiPart newPart(String name) {
        if (name.equals("wrcbe-tran")) return new TransmitterPart();
        if (name.equals("wrcbe-recv")) return new ReceiverPart();
        if (name.equals("wrcbe-jamm")) return new JammerPart();
        return null;
    }
}
