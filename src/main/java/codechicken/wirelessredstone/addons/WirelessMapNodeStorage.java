package codechicken.wirelessredstone.addons;

import codechicken.wirelessredstone.core.FreqCoord;
import java.util.TreeSet;

public class WirelessMapNodeStorage {
    public void clear() {
        nodes.clear();
        devices.clear();
    }

    public TreeSet<FreqCoord> nodes = new TreeSet<FreqCoord>();
    public TreeSet<FreqCoord> devices = new TreeSet<FreqCoord>();
}
