package codechicken.wirelessredstone.core;

import net.minecraft.entity.Entity;

import codechicken.lib.vec.Vector3;

public interface ITileJammer {

    public void jamTile(ITileWireless tile);

    public void jamEntity(Entity entity);

    public Vector3 getFocalPoint();
}
