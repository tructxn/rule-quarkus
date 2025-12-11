package io.quarkus.bazel.discovery;

import java.util.List;

/**
 * Information about a discovered CDI bean.
 */
public class BeanInfo {
    private final String className;
    private final String scope;
    private final List<String> qualifiers;
    
    public BeanInfo(String className, String scope, List<String> qualifiers) {
        this.className = className;
        this.scope = scope;
        this.qualifiers = qualifiers;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getScope() {
        return scope;
    }
    
    public List<String> getQualifiers() {
        return qualifiers;
    }
}
