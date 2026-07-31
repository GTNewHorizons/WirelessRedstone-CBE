package codechicken.wirelessredstone.core;

import static codechicken.wirelessredstone.core.WirelessRedstoneCore.LOGGER_CORE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import codechicken.core.CommonUtils;
import codechicken.lib.vec.BlockCoord;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

public class RedstoneEtherFrequency {

    /** Returned for dimensions with no nodes on this frequency. Callers only ever read from them. */
    private static final TreeMap<BlockCoord, Boolean> NO_TRANSMITTERS = new TreeMap<>();
    private static final TreeSet<BlockCoord> NO_RECEIVERS = new TreeSet<>();

    private boolean powered;
    private final int freq;
    private final RedstoneEther ether;

    // null until this frequency is actually used in some dimension. fastutil allocates its backing arrays eagerly,
    // so holding empty ones for all 5000 frequencies would cost more than the boxing they save.
    private Int2ObjectOpenHashMap<DimensionalNodeTracker> nodetrackers;
    private Int2IntOpenHashMap activeDimensions;

    private final ArrayList<WirelessTransmittingDevice> transmittingdevices = new ArrayList<>();

    private boolean useTemporarySet = false;

    private int colour = -1;
    private String name = "";

    public RedstoneEtherFrequency(RedstoneEther ether, int freq) {
        this.ether = ether;
        this.freq = freq;
    }

    public void remEther(int dimension) {
        if (nodetrackers != null) nodetrackers.remove(dimension);
    }

    private DimensionalNodeTracker getTracker(int dimension) {
        return nodetrackers == null ? null : nodetrackers.get(dimension);
    }

    /**
     * Trackers are created on demand
     *
     * @return null if the dimension isn't loaded
     */
    private DimensionalNodeTracker getOrCreateTracker(int dimension) {
        DimensionalNodeTracker tracker = getTracker(dimension);
        if (tracker == null) {
            World world = ether.getWorld(dimension);
            if (world == null) return null;

            tracker = new DimensionalNodeTracker(world);
            if (nodetrackers == null) nodetrackers = new Int2ObjectOpenHashMap<>(2);
            nodetrackers.put(dimension, tracker);
        }
        return tracker;
    }

    public boolean isOn() {
        return powered;
    }

    public void saveFreq(int dimension) {
        DimensionalNodeTracker nodetracker = getTracker(dimension);
        if (nodetracker == null || !nodetracker.isdirty) {
            return;
        }

        SaveManager.getInstance(dimension)
                .saveFreq(freq, getActiveTransmittersInDim(dimension), nodetracker.transmittermap, getDimensionHash());
        nodetracker.isdirty = false;
    }

    public void addReceiver(World world, BlockCoord node, int dimension) {
        DimensionalNodeTracker tracker = getOrCreateTracker(dimension);
        if (tracker == null) return;

        if (useTemporarySet) {
            tracker.temporarySet.add(new DelayedModification(node, 1));
            return;
        }

        tracker.receiverset.add(node);
        updateReceiver(world, node, isOn());
    }

    public void remTransmitter(World world, BlockCoord node, int dimension) {
        DimensionalNodeTracker tracker = getTracker(dimension);
        if (tracker == null) return;

        if (useTemporarySet) {
            tracker.temporarySet.add(new DelayedModification(node, 2));
            return;
        }

        Boolean wason = tracker.transmittermap.remove(node);

        if (wason != null && wason) {
            decrementActiveTransmitters(dimension);
            tracker.setDirty();
        }
    }

    public void remReceiver(World world, BlockCoord node, int dimension) {
        DimensionalNodeTracker tracker = getTracker(dimension);
        if (tracker == null) return;

        if (useTemporarySet) {
            tracker.temporarySet.add(new DelayedModification(node, 0));
            return;
        }

        tracker.receiverset.remove(node);
    }

    public void loadTransmitter(BlockCoord node, int dimension) {
        DimensionalNodeTracker tracker = getOrCreateTracker(dimension);
        if (tracker == null) return;

        tracker.transmittermap.put(node, true);
        incrementActiveTransmitters(dimension);
    }

    public void setTransmitter(World world, BlockCoord node, int dimension, boolean on) {
        DimensionalNodeTracker tracker = getOrCreateTracker(dimension);
        if (tracker == null) return;

        if (useTemporarySet) {
            tracker.temporarySet.add(new DelayedModification(node, 4 | (on ? 1 : 0)));
            return;
        }

        // if the put returned null the transmitter needed adding to the list
        Boolean wasnodeon = tracker.transmittermap.put(node, on);
        boolean newtransmitter = wasnodeon == null;

        if (!newtransmitter && (on == wasnodeon)) {
            return;
        }

        if (on) {
            incrementActiveTransmitters(dimension);
            tracker.setDirty();
        } else if (!newtransmitter) {
            decrementActiveTransmitters(dimension);
            tracker.setDirty();
        }
    }

    public void incrementActiveTransmitters(int dimension) {
        setActiveTransmittersInDim(dimension, getActiveTransmittersInDim(dimension) + 1);
    }

    public void decrementActiveTransmitters(int dimension) {
        setActiveTransmittersInDim(dimension, getActiveTransmittersInDim(dimension) - 1);
    }

    public void updateAllReceivers() {
        ((RedstoneEtherServer) ether).updateReceivingDevices(freq, powered);
        if (nodetrackers == null) return;

        // snapshot: updating a receiver can call back in and create a tracker for another dimension
        for (DimensionalNodeTracker tracker : nodetrackers.values().toArray(new DimensionalNodeTracker[0])) {
            int dimension = tracker.dimension;

            useTemporarySet = true;
            for (BlockCoord coord : tracker.receiverset) {
                updateReceiver(tracker.world, coord, powered);
            }
            useTemporarySet = false;

            while (tracker.temporarySet.size() > 0) {
                DelayedModification mod = tracker.temporarySet.removeFirst();

                if (mod.function == 0) remReceiver(tracker.world, mod.coord, dimension);
                else if (mod.function == 1) addReceiver(tracker.world, mod.coord, dimension);
                else if (mod.function == 2) remTransmitter(tracker.world, mod.coord, dimension);
                else if ((mod.function & 4) != 0)
                    setTransmitter(tracker.world, mod.coord, dimension, (mod.function & 1) != 0);
            }
        }
    }

    public void updateAllReceivers(DimensionalNodeTracker tracker) {}

    public void updateReceiver(World world, BlockCoord node, boolean on) {
        // receivers stay in the ether while their chunk is unloaded, and re-sync through addReceiver on world join
        if (!world.blockExists(node.x, node.y, node.z)) return;

        TileEntity tileentity = RedstoneEther.getTile(world, node);
        if (tileentity instanceof ITileReceiver) {
            ((ITileReceiver) tileentity).setActive(on);
        } else {
            LOGGER_CORE.info(
                    "Null Receiver at:{},{},{} in dim{}",
                    node.x,
                    node.y,
                    node.z,
                    CommonUtils.getDimension(world));
        }
    }

    public void setColour(int colourid) {
        colour = colourid;
        if (!ether.remote && !SaveManager.isLoading()) {
            if (colourid == -1) SaveManager.freqProp.removeProperty(freq + ".colour");
            else SaveManager.freqProp.setProperty(freq + ".colour", colour);
        }
    }

    public int getColourId() {
        return colour;
    }

    public void setName(String name) {
        this.name = name;
        if (!ether.remote && !SaveManager.isLoading()) {
            if (name == null || name.isEmpty()) SaveManager.freqProp.removeProperty(freq + ".name");
            else SaveManager.freqProp.setProperty(freq + ".name", name);
        }
    }

    public String getName() {
        return name;
    }

    public void setClean(int dimension) {
        DimensionalNodeTracker tracker = getTracker(dimension);
        if (tracker != null) tracker.isdirty = false;
    }

    public int nodeCount() {
        if (nodetrackers == null) return 0;

        int count = 0;
        for (DimensionalNodeTracker tracker : nodetrackers.values()) {
            count += tracker.transmittermap.size() + tracker.receiverset.size();
        }
        return count;
    }

    public TreeSet<BlockCoord> getReceivers(int dimension) {
        DimensionalNodeTracker tracker = getTracker(dimension);
        return tracker == null ? NO_RECEIVERS : tracker.receiverset;
    }

    public TreeMap<BlockCoord, Boolean> getTransmitters(int dimension) {
        DimensionalNodeTracker tracker = getTracker(dimension);
        return tracker == null ? NO_TRANSMITTERS : tracker.transmittermap;
    }

    public void addTransmittingDevice(WirelessTransmittingDevice device) {
        if (transmittingdevices.add(device)) {
            incrementActiveTransmitters(device.getDimension());
        }
    }

    public void removeTransmittingDevice(WirelessTransmittingDevice device) {
        if (transmittingdevices.remove(device)) {
            decrementActiveTransmitters(device.getDimension());
        }
    }

    public List<WirelessTransmittingDevice> getTransmittingDevices() {
        return transmittingdevices;
    }

    public void putActiveTransmittersInList(int dimension, ArrayList<FreqCoord> txnodes) {
        DimensionalNodeTracker nodetracker = getTracker(dimension);
        if (nodetracker == null) return;

        for (Iterator<BlockCoord> iterator = nodetracker.transmittermap.keySet().iterator(); iterator.hasNext();) {
            BlockCoord node = iterator.next();
            if (nodetracker.transmittermap.get(node)) txnodes.add(new FreqCoord(node, freq));
        }
    }

    /*
     * public void putTransmittingDevicesInList(ArrayList<WirelessTransmittingDevice> txnodes) {
     * txnodes.addAll(transmittingdevices); }
     */

    public int getActiveTransmitters() {
        if (activeDimensions == null) return 0;

        int num = 0;
        for (IntIterator iterator = activeDimensions.values().iterator(); iterator.hasNext();) {
            num += iterator.nextInt();
        }
        return num;
    }

    public int getActiveTransmittersInDim(int dim) {
        return activeDimensions == null ? 0 : activeDimensions.get(dim);
    }

    /**
     * Note: Causes updates
     */
    public void setActiveTransmittersInDim(int dim, int num) {
        // an absent dimension already counts as zero, and SaveManager doesn't persist zeroes either. Only update
        // entries that exist, so dimensions that never had a transmitter on this frequency cost nothing.
        if (num != 0) {
            if (activeDimensions == null) activeDimensions = new Int2IntOpenHashMap(2);
            activeDimensions.put(dim, num);
        } else if (activeDimensions != null && activeDimensions.containsKey(dim)) {
            activeDimensions.put(dim, 0);
        }

        if ((num == 0) == powered) // powered and setting 0 or not powered and setting >0
        {
            boolean nowPowered = getActiveTransmitters() > 0;
            if (nowPowered != powered) {
                powered = nowPowered;
                updateAllReceivers();
            }
        }
    }

    public Int2IntMap getDimensionHash() {
        return activeDimensions == null ? Int2IntMaps.EMPTY_MAP : Int2IntMaps.unmodifiable(activeDimensions);
    }

    public static class DelayedModification {

        final BlockCoord coord;
        final int function;

        public DelayedModification(BlockCoord node, int i) {
            coord = node;
            function = i;
        }
    }

    public class DimensionalNodeTracker {

        public final TreeMap<BlockCoord, Boolean> transmittermap = new TreeMap<>();
        public final TreeSet<BlockCoord> receiverset = new TreeSet<>();
        public final LinkedList<DelayedModification> temporarySet = new LinkedList<>();
        public final World world;
        public final int dimension;
        private boolean isdirty = false;

        public DimensionalNodeTracker(World world2) {
            world = world2;
            dimension = CommonUtils.getDimension(world2);
        }

        public void setDirty() {
            if (!isdirty) ((RedstoneEtherServer) ether).addFreqToSave(RedstoneEtherFrequency.this, dimension);
            isdirty = true;
        }
    }
}
