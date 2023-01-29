package codechicken.wirelessredstone.core;

import net.minecraft.entity.EntityLivingBase;

import codechicken.lib.vec.Vector3;

public interface WirelessTransmittingDevice {

    public Vector3 getPosition();

    public int getDimension();

    public int getFreq();

    EntityLivingBase getAttachedEntity();
}
