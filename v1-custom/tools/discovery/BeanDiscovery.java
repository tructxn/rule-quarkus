package io.quarkus.bazel.discovery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Discovers CDI beans from annotations.
 */
public class BeanDiscovery {
    
    private static final DotName APPLICATION_SCOPED = DotName.createSimple("jakarta.enterprise.context.ApplicationScoped");
    private static final DotName REQUEST_SCOPED = DotName.createSimple("jakarta.enterprise.context.RequestScoped");
    private static final DotName SESSION_SCOPED = DotName.createSimple("jakarta.enterprise.context.SessionScoped");
    private static final DotName DEPENDENT = DotName.createSimple("jakarta.enterprise.context.Dependent");
    private static final DotName SINGLETON = DotName.createSimple("jakarta.inject.Singleton");
    
    public static List<BeanInfo> discover(IndexView index) {
        List<BeanInfo> beans = new ArrayList<>();
        
        discoverBeansWithScope(index, APPLICATION_SCOPED, "ApplicationScoped", beans);
        discoverBeansWithScope(index, REQUEST_SCOPED, "RequestScoped", beans);
        discoverBeansWithScope(index, SESSION_SCOPED, "SessionScoped", beans);
        discoverBeansWithScope(index, DEPENDENT, "Dependent", beans);
        discoverBeansWithScope(index, SINGLETON, "Singleton", beans);
        
        return beans;
    }
    
    private static void discoverBeansWithScope(
            IndexView index,
            DotName scopeAnnotation,
            String scopeName,
            List<BeanInfo> beans) {
        
        try {
            Collection<AnnotationInstance> annotations = index.getAnnotations(scopeAnnotation);
            
            for (AnnotationInstance annotation : annotations) {
                if (annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.CLASS) {
                    ClassInfo classInfo = annotation.target().asClass();
                    BeanInfo bean = extractBeanInfo(classInfo, scopeName);
                    if (bean != null) {
                        beans.add(bean);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to discover beans with scope " + scopeName + ": " + e.getMessage());
        }
    }
    
    private static BeanInfo extractBeanInfo(ClassInfo classInfo, String scope) {
        try {
            String className = classInfo.name().toString();
            List<String> qualifiers = extractQualifiers(classInfo);
            
            return new BeanInfo(className, scope, qualifiers);
        } catch (Exception e) {
            System.err.println("Warning: Failed to extract bean info: " + e.getMessage());
            return null;
        }
    }
    
    private static List<String> extractQualifiers(ClassInfo classInfo) {
        List<String> qualifiers = new ArrayList<>();
        
        // For now, just return empty list
        // Full implementation would scan for @Qualifier annotations
        
        return qualifiers;
    }
}
