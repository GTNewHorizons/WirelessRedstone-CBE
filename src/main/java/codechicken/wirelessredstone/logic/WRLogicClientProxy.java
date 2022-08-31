package codechicken.wirelessredstone.logic;

import static codechicken.wirelessredstone.logic.WirelessRedstoneLogic.*;

import codechicken.core.ClientUtils;
import net.minecraftforge.client.MinecraftForgeClient;

public class WRLogicClientProxy extends WRLogicProxy {
    @Override
    public void init() {
        super.init();
        ClientUtils.enhanceSupportersList("WR-CBE|Logic");

        MinecraftForgeClient.registerItemRenderer(itemwireless, new ItemWirelessRenderer());
    }
}
