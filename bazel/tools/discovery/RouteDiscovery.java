package io.quarkus.bazel.discovery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Discovers routes from @Route annotations.
 */
public class RouteDiscovery {
    
    private static final DotName ROUTE_ANNOTATION = DotName.createSimple("io.quarkus.vertx.web.Route");
    private static final DotName ROUTE_BASE_ANNOTATION = DotName.createSimple("io.quarkus.vertx.web.RouteBase");
    
    public static DiscoveryResult discover(IndexView index) {
        List<RouteInfo> routes = discoverRoutes(index);
        return new DiscoveryResult(routes, index);
    }
    
    private static List<RouteInfo> discoverRoutes(IndexView index) {
        List<RouteInfo> routes = new ArrayList<>();
        
        try {
            Collection<AnnotationInstance> annotations = index.getAnnotations(ROUTE_ANNOTATION);
            
            for (AnnotationInstance annotation : annotations) {
                RouteInfo route = extractRouteInfo(annotation);
                if (route != null) {
                    routes.add(route);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to discover routes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return routes;
    }
    
    private static RouteInfo extractRouteInfo(AnnotationInstance annotation) {
        try {
            MethodInfo method = annotation.target().asMethod();
            ClassInfo classInfo = method.declaringClass();
            
            String path = extractPath(annotation);
            String basePath = extractBasePath(classInfo);
            String httpMethod = extractHttpMethod(annotation);
            String fullPath = basePath + path;
            
            return new RouteInfo(
                classInfo.name().toString(),
                method.name(),
                fullPath,
                httpMethod
            );
        } catch (Exception e) {
            System.err.println("Warning: Failed to extract route info: " + e.getMessage());
            return null;
        }
    }
    
    private static String extractPath(AnnotationInstance annotation) {
        AnnotationValue pathValue = annotation.value("path");
        return pathValue != null ? pathValue.asString() : "/";
    }
    
    private static String extractBasePath(ClassInfo classInfo) {
        AnnotationInstance routeBase = classInfo.classAnnotation(ROUTE_BASE_ANNOTATION);
        if (routeBase != null) {
            AnnotationValue basePathValue = routeBase.value("path");
            if (basePathValue != null) {
                return basePathValue.asString();
            }
        }
        return "";
    }
    
    private static String extractHttpMethod(AnnotationInstance annotation) {
        AnnotationValue methodsValue = annotation.value("methods");
        if (methodsValue != null) {
            return methodsValue.asEnumArray()[0];
        }
        return "GET";
    }
}
