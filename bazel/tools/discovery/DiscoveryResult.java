package io.quarkus.bazel.discovery;

import org.jboss.jandex.IndexView;
import java.util.List;

/**
 * Result of annotation discovery process.
 */
public class DiscoveryResult {
    private final List<RouteInfo> routes;
    private final IndexView index;
    
    public DiscoveryResult(List<RouteInfo> routes, IndexView index) {
        this.routes = routes;
        this.index = index;
    }
    
    public List<RouteInfo> getRoutes() {
        return routes;
    }
    
    public IndexView getIndex() {
        return index;
    }
}
