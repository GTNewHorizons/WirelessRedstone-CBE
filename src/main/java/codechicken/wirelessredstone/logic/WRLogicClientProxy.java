package codechicken.wirelessredstone.logic;

import static codechicken.wirelessredstone.logic.WirelessRedstoneLogic.*;

import net.minecraftforge.client.MinecraftForgeClient;

import codechicken.core.ClientUtils;

public class WRLogicClientProxy extends WRLogicProxy {

    @Override
    public void init() {
        super.init();
        ClientUtils.enhanceSupportersList("WR-CBE|Logic");

        MinecraftForgeClient.registerItemRenderer(itemwireless, new ItemWirelessRenderer());
    }
}
