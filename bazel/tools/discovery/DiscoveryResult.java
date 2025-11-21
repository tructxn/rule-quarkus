package io.quarkus.bazel.discovery;

import org.jboss.jandex.IndexView;
import java.util.List;

/**
 * Result of annotation discovery process.
 */
public class DiscoveryResult {
    private final List<RouteInfo> routes;
    private final List<BeanInfo> beans;
    private final IndexView index;
    
    public DiscoveryResult(List<RouteInfo> routes, List<BeanInfo> beans, IndexView index) {
        this.routes = routes;
        this.beans = beans;
        this.index = index;
    }
    
    public List<RouteInfo> getRoutes() {
        return routes;
    }
    
    public List<BeanInfo> getBeans() {
        return beans;
    }
    
    public IndexView getIndex() {
        return index;
    }
}
