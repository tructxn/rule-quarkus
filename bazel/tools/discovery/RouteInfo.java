package io.quarkus.bazel.discovery;

/**
 * Information about a discovered route.
 */
public class RouteInfo {
    private final String className;
    private final String methodName;
    private final String path;
    private final String httpMethod;
    
    public RouteInfo(String className, String methodName, String path, String httpMethod) {
        this.className = className;
        this.methodName = methodName;
        this.path = path;
        this.httpMethod = httpMethod;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
}
