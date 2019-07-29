package org.openkilda.persistence.tests;

import static java.lang.String.format;

import org.openkilda.model.IslStatus;
import org.openkilda.model.SwitchId;
import org.openkilda.model.SwitchStatus;
import org.openkilda.persistence.ferma.model.IslImpl;
import org.openkilda.persistence.ferma.model.SwitchImpl;
import org.openkilda.persistence.ferma.repositories.frames.IslFrame;
import org.openkilda.persistence.ferma.repositories.frames.SwitchFrame;

import com.syncleus.ferma.FramedGraph;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class TopologyBuilder {
    private final FramedGraph graph;
    private final int islandsCount;
    private final int regionsPerIsland;
    private final int switchesPerRegion;

    public TopologyBuilder(FramedGraph graph,
                           int islandsCount, int regionsPerIsland, int switchesPerRegion) {
        this.graph = graph;
        this.islandsCount = islandsCount;
        this.regionsPerIsland = regionsPerIsland;
        this.switchesPerRegion = switchesPerRegion;
    }

    public List<Island> buildCircles() {
        List<Island> islands = new ArrayList<>();

        IntStream.rangeClosed(1, islandsCount).forEach(i -> {
            Island newIsland = new Island();

            IntStream.rangeClosed(1, regionsPerIsland).forEach(r -> {
                Region newRegion = new Region();

                IntStream.rangeClosed(1, switchesPerRegion).forEach(s -> {
                    SwitchId switchId = new SwitchId(format("%02X:%02X:%02X", i, r, s));
                    SwitchFrame newSw = buildSwitch(switchId);

                    if (!newRegion.switches.isEmpty()) {
                        SwitchFrame prevSw = newRegion.switches.get(newRegion.switches.size() - 1);

                        buildIsl(prevSw, 1000 + s, newSw, 1000 + s);
                        buildIsl(newSw, 2000 + s, prevSw, 2000 + s);
                    }

                    newRegion.switches.add(newSw);
                });

                SwitchFrame first = newRegion.switches.get(0);
                SwitchFrame last = newRegion.switches.get(newRegion.switches.size() - 1);

                buildIsl(first, 3000, last, 3000);
                buildIsl(last, 3001, first, 3001);

                if (!newIsland.regions.isEmpty()) {
                    Region prevRegion = newIsland.regions.get(newIsland.regions.size() - 1);
                    SwitchFrame prevRegionSwitch = prevRegion.switches.get(prevRegion.switches.size() / 2);
                    SwitchFrame newRegionSwitch = newRegion.switches.get(0);

                    buildIsl(prevRegionSwitch, 3002, newRegionSwitch, 3002);
                    buildIsl(newRegionSwitch, 3003, prevRegionSwitch, 3003);
                }

                newIsland.regions.add(newRegion);
            });

            SwitchFrame firstRegionSwitch = newIsland.regions.get(0).switches.get(0);
            Region lastRegion = newIsland.regions.get(newIsland.regions.size() - 1);
            SwitchFrame lastRegionSwitch = lastRegion.switches.get(lastRegion.switches.size() / 2);

            buildIsl(firstRegionSwitch, 3000, lastRegionSwitch, 3000);
            buildIsl(lastRegionSwitch, 3001, firstRegionSwitch, 3001);

            islands.add(newIsland);
        });

        return islands;
    }

    public List<Island> buildStars(int raysPerRegion) {
        List<Island> islands = new ArrayList<>();

        IntStream.rangeClosed(1, islandsCount).forEach(i -> {
            Island newIsland = new Island();

            IntStream.rangeClosed(1, regionsPerIsland).forEach(r -> {
                Region newRegion = new Region();

                SwitchId eyeSwitchId = new SwitchId(format("%02X:%02X:FF:FF", i, r));
                SwitchFrame newRegionEyeSw = buildSwitch(eyeSwitchId);
                newRegion.switches.add(newRegionEyeSw);

                int switchesPerRay = switchesPerRegion / raysPerRegion;
                IntStream.rangeClosed(1, raysPerRegion).forEach(y -> {
                    List<SwitchFrame> raySwitches = new ArrayList<>();

                    IntStream.rangeClosed(1, switchesPerRay).forEach(s -> {
                        SwitchId switchId = new SwitchId(format("%02X:%02X:%02X:%02X", i, r, y, s));
                        SwitchFrame newSw = buildSwitch(switchId);

                        SwitchFrame prevSw = raySwitches.isEmpty() ? newRegionEyeSw : raySwitches.get(raySwitches.size() - 1);

                        buildIsl(prevSw, 1000 + s, newSw, 1000 + s);
                        buildIsl(newSw, 2000 + s, prevSw, 2000 + s);

                        raySwitches.add(newSw);
                    });

                    newRegion.switches.addAll(raySwitches);
                });

                if (!newIsland.regions.isEmpty()) {
                    SwitchFrame eyeRegionSwitch = newIsland.regions.get(0).switches.get(0);

                    buildIsl(eyeRegionSwitch, 3000, newRegionEyeSw, 3000);
                    buildIsl(newRegionEyeSw, 3001, eyeRegionSwitch, 3001);
                }

                newIsland.regions.add(newRegion);
            });

            islands.add(newIsland);
        });

        return islands;
    }

    public List<Island> buildMeshes() {
        List<Island> islands = new ArrayList<>();

        IntStream.rangeClosed(1, islandsCount).forEach(i -> {
            Island newIsland = new Island();

            IntStream.rangeClosed(1, regionsPerIsland).forEach(r -> {
                Region newRegion = new Region();

                IntStream.rangeClosed(1, switchesPerRegion).forEach(s -> {
                    SwitchId switchId = new SwitchId(format("%02X:%02X:%02X", i, r, s));
                    SwitchFrame newSw = buildSwitch(switchId);

                    int index = 100;
                    for (SwitchFrame prevSw : newRegion.switches) {
                        buildIsl(prevSw, 1000 + index + s, newSw, 1000 + index + s);
                        buildIsl(newSw, 2000 + index + s, prevSw, 2000 + index + s);
                        index += 100;
                    }

                    newRegion.switches.add(newSw);
                });

                int index = 100;
                for (Region prevRegion : newIsland.regions) {
                    SwitchFrame prevRegionSwitch = prevRegion.switches.get(prevRegion.switches.size() / 2);
                    SwitchFrame newRegionSwitch = newRegion.switches.get(0);

                    buildIsl(prevRegionSwitch, 3000 + index, newRegionSwitch, 3000 + index);
                    buildIsl(newRegionSwitch, 3001 + index, prevRegionSwitch, 3001 + index);
                }

                newIsland.regions.add(newRegion);
            });

            islands.add(newIsland);
        });

        return islands;
    }

    public List<Island> buildTree(int branchesPerTreeKnot) {
        List<Island> islands = new ArrayList<>();

        IntStream.rangeClosed(1, islandsCount).forEach(i -> {
            Island newIsland = new Island();

            IntStream.rangeClosed(1, regionsPerIsland).forEach(r -> {
                Region newRegion = new Region();

                String eyeSwitchId = format("%02X:%02X", i, r);
                SwitchFrame newRegionEyeSwitch = buildSwitch(new SwitchId(eyeSwitchId));
                newRegion.switches.add(newRegionEyeSwitch);

                buildSubTree(newRegion, newRegionEyeSwitch, eyeSwitchId, branchesPerTreeKnot, switchesPerRegion - 1);

                if (!newIsland.regions.isEmpty()) {
                    List<SwitchFrame> lastRegionSwitches = newIsland.regions.get(newIsland.regions.size() - 1).switches;
                    SwitchFrame lastRegionSwitch = lastRegionSwitches.get(lastRegionSwitches.size() - 1);

                    buildIsl(lastRegionSwitch, 3000, newRegionEyeSwitch, 3000);
                    buildIsl(newRegionEyeSwitch, 3001, lastRegionSwitch, 3001);
                }

                newIsland.regions.add(newRegion);
            });

            islands.add(newIsland);
        });

        return islands;
    }

    private void buildSubTree(Region region, SwitchFrame knotSwitch, String knotSwitchId,
                              int branchesPerTreeKnot, int switchesLeft) {
        if (switchesLeft <= 0) {
            return;
        }

        int switchesPerKnot = switchesLeft / branchesPerTreeKnot;

        for (int b = 1; b <= Math.min(switchesPerKnot, branchesPerTreeKnot); b++) {
            String switchId = format("%02X:%s", b, knotSwitchId);
            SwitchFrame newSw = buildSwitch(new SwitchId(switchId));
            region.switches.add(newSw);

            buildIsl(knotSwitch, 1000 + b, newSw, 1000 + b);
            buildIsl(newSw, 2000 + b, knotSwitch, 2000 + b);

            if (switchesPerKnot > 1) {
                buildSubTree(region, newSw, switchId, branchesPerTreeKnot, switchesPerKnot - 1);
            }
        }
    }

    private SwitchFrame buildSwitch(SwitchId switchId) {
        SwitchFrame result = SwitchFrame.addNew(graph, SwitchImpl.builder()
                .switchId(switchId).status(SwitchStatus.ACTIVE).build());
        graph.tx().commit();
        return result;
    }

    private void buildIsl(SwitchFrame srcSwitch, int srcPort, SwitchFrame destSwitch, int destPort) {
        IslFrame.addNew(graph, IslImpl.builder()
                .srcSwitch(srcSwitch).srcPort(srcPort)
                .destSwitch(destSwitch).destPort(destPort)
                .cost(1).availableBandwidth(1).status(IslStatus.ACTIVE).build());
        graph.tx().commit();
    }

    @Value
    public static class Island {
        List<Region> regions = new ArrayList<>();
    }

    @Value
    public static class Region {
        List<SwitchFrame> switches = new ArrayList<>();
    }
}