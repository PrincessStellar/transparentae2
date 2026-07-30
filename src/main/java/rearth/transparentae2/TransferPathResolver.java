package rearth.transparentae2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.util.AECableType;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.pathfinding.IPathItem;
import appeng.parts.AEBasePart;
import appeng.parts.networking.CablePart;

final class TransferPathResolver {
    private TransferPathResolver() {
    }

    static ResolvedPath resolve(IGridNode endpoint, boolean controllerFirst) {
        var route = routeToController(endpoint);
        if (route.failure != null) {
            return ResolvedPath.failed(route.failure);
        }

        var points = new ArrayList<>(route.points);
        var hops = new ArrayList<>(route.hops);
        if (controllerFirst) {
            Collections.reverse(points);
            Collections.reverse(hops);
        }

        return ResolvedPath.of(new PathLeg(List.copyOf(points), List.copyOf(hops)));
    }

    static ResolvedPath resolveCrafting(IGridNode cpu, IGridNode provider) {
        var cpuRoute = routeToController(cpu);
        var providerRoute = routeToController(provider);

        if (cpuRoute.failure != null || providerRoute.failure != null) {
            return ResolvedPath.failed("CPU route="
                    + failureOrOk(cpuRoute)
                    + ", provider route="
                    + failureOrOk(providerRoute));
        }

        var providerPoints = new ArrayList<>(providerRoute.points);
        var providerHops = new ArrayList<>(providerRoute.hops);
        Collections.reverse(providerPoints);
        Collections.reverse(providerHops);

        return ResolvedPath.of(
                new PathLeg(cpuRoute.points, cpuRoute.hops),
                new PathLeg(List.copyOf(providerPoints), List.copyOf(providerHops)));
    }

    private static Route routeToController(IGridNode endpoint) {
        if (endpoint == null) {
            return Route.failed("endpoint has no grid node");
        }

        var pathing = endpoint.getGrid().getPathingService();
        if (pathing.isNetworkBooting()) {
            return Route.failed("network pathing is still booting");
        }
        if (pathing.getControllerState() != ControllerState.CONTROLLER_ONLINE) {
            return Route.failed("controller state is " + pathing.getControllerState());
        }
        if (!(endpoint instanceof IPathItem currentPath)) {
            return Route.failed("grid node does not expose AE2 controller-route data");
        }

        var points = new ArrayList<PathPoint>();
        var hops = new ArrayList<PathHop>();
        Set<IPathItem> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        var currentNode = endpoint;
        points.add(describe(currentNode));

        while (!(currentNode.getOwner() instanceof ControllerBlockEntity)) {
            if (!visited.add(currentPath)) {
                return Route.failed("cycle found in AE2 controller route");
            }

            final IPathItem parentRoute;
            try {
                parentRoute = currentPath.getControllerRoute();
            } catch (RuntimeException e) {
                return Route.failed("AE2 controller route is not available: " + e.getMessage());
            }

            if (!(parentRoute instanceof IGridConnection connection)) {
                return Route.failed("expected a grid connection after " + describe(currentNode).label);
            }

            var parentPath = ((IPathItem) connection).getControllerRoute();
            if (!(parentPath instanceof IGridNode parentNode)) {
                return Route.failed("expected a grid node after controller-route connection");
            }

            hops.add(new PathHop(connection.getUsedChannels(), connection.isInWorld()));
            points.add(describe(parentNode));
            currentNode = parentNode;
            currentPath = parentPath;
        }

        return new Route(List.copyOf(points), List.copyOf(hops), null);
    }

    private static String failureOrOk(Route route) {
        return route.failure == null ? "ok" : route.failure;
    }

    private static PathPoint describe(IGridNode node) {
        var owner = node.getOwner();
        if (owner instanceof BlockEntity blockEntity) {
            return describe(owner.getClass().getSimpleName(), blockEntity);
        }
        if (owner instanceof AEBasePart part) {
            var cableWidth = part instanceof CablePart cable ? minimumCableWidth(cable.getCableConnectionType()) : 0;
            return describe(owner.getClass().getSimpleName(), part.getBlockEntity(), cableWidth);
        }
        return new PathPoint(null, owner.getClass().getSimpleName() + " (no world position)", 0);
    }

    private static PathPoint describe(String type, BlockEntity blockEntity) {
        return describe(type, blockEntity, 0);
    }

    private static PathPoint describe(String type, BlockEntity blockEntity, int cableWidth) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return new PathPoint(null, type + " @ unloaded " + blockEntity.getBlockPos().toShortString(), cableWidth);
        }

        var position = GlobalPos.of(level.dimension(), blockEntity.getBlockPos());
        return new PathPoint(position, type + " @ " + level.dimension().identifier() + " "
                + blockEntity.getBlockPos().toShortString(), cableWidth);
    }

    private static int minimumCableWidth(AECableType cableType) {
        return switch (cableType) {
            case DENSE_COVERED, DENSE_SMART -> 10;
            case SMART -> 6;
            case GLASS, COVERED -> 4;
            default -> 0;
        };
    }

    record ResolvedPath(List<PathLeg> legs, String failure) {
        static ResolvedPath of(PathLeg... legs) {
            return new ResolvedPath(List.of(legs), null);
        }

        static ResolvedPath failed(String reason) {
            return new ResolvedPath(List.of(), reason);
        }

        boolean available() {
            return failure == null;
        }

        List<List<GlobalPos>> positions() {
            var result = new ArrayList<List<GlobalPos>>();
            for (var leg : legs) {
                var segment = new ArrayList<GlobalPos>();
                for (int pointIndex = 0; pointIndex < leg.points.size(); pointIndex++) {
                    var point = leg.points.get(pointIndex).position;
                    if (pointIndex > 0 && !leg.hops.get(pointIndex - 1).inWorld) {
                        addSegment(result, segment);
                        segment = new ArrayList<>();
                    }
                    if (point == null) {
                        addSegment(result, segment);
                        segment = new ArrayList<>();
                    } else {
                        segment.add(point);
                    }
                }
                addSegment(result, segment);
            }
            return List.copyOf(result);
        }

        int minimumCableWidth() {
            return legs.stream()
                    .flatMap(leg -> leg.points.stream())
                    .mapToInt(PathPoint::cableWidth)
                    .filter(width -> width > 0)
                    .min()
                    .orElse(4);
        }

        String format() {
            if (failure != null) {
                return "unavailable: " + failure;
            }
            if (legs.size() == 1) {
                return legs.getFirst().format();
            }
            return "CPU -> controller: "
                    + legs.get(0).format()
                    + " || controller -> provider: "
                    + legs.get(1).format();
        }

        private static void addSegment(List<List<GlobalPos>> result, List<GlobalPos> segment) {
            if (!segment.isEmpty()) {
                result.add(List.copyOf(segment));
            }
        }
    }

    private record Route(List<PathPoint> points, List<PathHop> hops, String failure) {
        static Route failed(String reason) {
            return new Route(List.of(), List.of(), reason);
        }
    }

    record PathLeg(List<PathPoint> points, List<PathHop> hops) {
        String format() {
            var result = new StringBuilder();
            for (int i = 0; i < points.size(); i++) {
                if (i > 0) {
                    var hop = hops.get(i - 1);
                    result.append(" --[used=")
                            .append(hop.usedChannels)
                            .append(", ")
                            .append(hop.inWorld ? "cable" : "internal/virtual")
                            .append("]--> ");
                }
                result.append(points.get(i).label);
            }
            return result.toString();
        }
    }

    record PathPoint(GlobalPos position, String label, int cableWidth) {
    }

    record PathHop(int usedChannels, boolean inWorld) {
    }
}
