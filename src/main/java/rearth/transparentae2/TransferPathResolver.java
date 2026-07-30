package rearth.transparentae2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ControllerState;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.pathfinding.IPathItem;
import appeng.parts.AEBasePart;

final class TransferPathResolver {
    private TransferPathResolver() {
    }

    static String resolve(IGridNode endpoint, boolean controllerFirst) {
        var route = routeToController(endpoint);
        if (route.failure != null) {
            return "unavailable: " + route.failure;
        }

        var points = new ArrayList<>(route.points);
        var hops = new ArrayList<>(route.hops);
        if (controllerFirst) {
            Collections.reverse(points);
            Collections.reverse(hops);
        }

        return format(points, hops);
    }

    static String resolveCrafting(IGridNode cpu, IGridNode provider) {
        var cpuRoute = routeToController(cpu);
        var providerRoute = routeToController(provider);

        if (cpuRoute.failure != null || providerRoute.failure != null) {
            return "unavailable: CPU route="
                    + failureOrOk(cpuRoute)
                    + ", provider route="
                    + failureOrOk(providerRoute);
        }

        var providerPoints = new ArrayList<>(providerRoute.points);
        var providerHops = new ArrayList<>(providerRoute.hops);
        Collections.reverse(providerPoints);
        Collections.reverse(providerHops);

        return "CPU -> controller: "
                + format(cpuRoute.points, cpuRoute.hops)
                + " || controller -> provider: "
                + format(providerPoints, providerHops);
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

    private static String format(List<PathPoint> points, List<PathHop> hops) {
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

    private static String failureOrOk(Route route) {
        return route.failure == null ? "ok" : route.failure;
    }

    private static PathPoint describe(IGridNode node) {
        var owner = node.getOwner();
        if (owner instanceof BlockEntity blockEntity) {
            return new PathPoint(describe(owner.getClass().getSimpleName(), blockEntity));
        }
        if (owner instanceof AEBasePart part) {
            return new PathPoint(describe(owner.getClass().getSimpleName(), part.getBlockEntity()));
        }
        return new PathPoint(owner.getClass().getSimpleName() + " (no world position)");
    }

    private static String describe(String type, BlockEntity blockEntity) {
        var level = blockEntity.getLevel();
        var dimension = level == null ? "unloaded" : level.dimension().identifier().toString();
        return type + " @ " + dimension + " " + blockEntity.getBlockPos().toShortString();
    }

    private record Route(List<PathPoint> points, List<PathHop> hops, String failure) {
        static Route failed(String reason) {
            return new Route(List.of(), List.of(), reason);
        }
    }

    private record PathPoint(String label) {
    }

    private record PathHop(int usedChannels, boolean inWorld) {
    }
}
