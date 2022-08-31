package codechicken.wirelessredstone.addons;

import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.WirelessReceivingDevice;
import net.minecraft.entity.player.EntityPlayer;

public class Sniffer implements WirelessReceivingDevice {
    public Sniffer(EntityPlayer player) {
        owner = player;
    }

    public void updateDevice(int freq, boolean on) {
        if (RedstoneEther.get(false).canBroadcastOnFrequency(owner, freq)) {
            WRAddonSPH.sendUpdateSnifferTo(owner, freq, on);
        }
    }

    EntityPlayer owner;
}
