package io.quarkus.bazel.indexer;

import io.quarkus.bazel.augmentor.AugmentationContext;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Builds Jandex index from JAR files.
 */
public class IndexBuilder {
    
    public static IndexView createIndex(AugmentationContext context) throws Exception {
        Indexer indexer = new Indexer();
        
        indexJars(context.getApplicationJars(), indexer);
        indexJars(context.getRuntimeJars(), indexer);
        
        return indexer.complete();
    }
    
    private static void indexJars(Iterable<Path> jars, Indexer indexer) throws Exception {
        for (Path jar : jars) {
            indexJar(jar, indexer);
        }
    }
    
    private static void indexJar(Path jarPath, Indexer indexer) throws Exception {
        try (FileInputStream fis = new FileInputStream(jarPath.toFile());
             JarInputStream jis = new JarInputStream(fis)) {
            
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    indexer.index(jis);
                }
            }
        }
    }
}
