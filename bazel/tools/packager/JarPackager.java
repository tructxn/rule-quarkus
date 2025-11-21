package io.quarkus.bazel.packager;

import io.quarkus.bazel.augmentor.AugmentationContext;
import io.quarkus.bazel.generator.GeneratedClass;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Packages augmented JAR with generated classes.
 */
public class JarPackager {
    
    public static void packageJar(
            AugmentationContext context,
            List<GeneratedClass> generatedClasses) throws Exception {
        
        Manifest manifest = ManifestBuilder.build(context);
        
        try (FileOutputStream fos = new FileOutputStream(context.getOutputJar().toFile());
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            
            copyApplicationClasses(jos, context.getApplicationJars());
            addGeneratedClasses(jos, generatedClasses);
        }
    }
    
    private static void copyApplicationClasses(JarOutputStream jos, List<Path> jars) throws Exception {
        Set<String> addedEntries = new HashSet<>();
        
        for (Path jar : jars) {
            copyJarEntries(jar, jos, addedEntries);
        }
    }
    
    private static void copyJarEntries(Path sourceJar, JarOutputStream target, Set<String> addedEntries) throws Exception {
        try (FileInputStream fis = new FileInputStream(sourceJar.toFile());
             JarInputStream jis = new JarInputStream(fis)) {
            
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                
                if (shouldSkipEntry(entryName, addedEntries)) {
                    continue;
                }
                
                copyEntry(jis, target, entry, entryName, addedEntries);
            }
        }
    }
    
    private static boolean shouldSkipEntry(String entryName, Set<String> addedEntries) {
        return entryName.equals("META-INF/MANIFEST.MF") || addedEntries.contains(entryName);
    }
    
    private static void copyEntry(
            JarInputStream jis,
            JarOutputStream target,
            JarEntry entry,
            String entryName,
            Set<String> addedEntries) throws Exception {
        
        try {
            JarEntry newEntry = new JarEntry(entryName);
            newEntry.setTime(entry.getTime());
            target.putNextEntry(newEntry);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = jis.read(buffer)) != -1) {
                target.write(buffer, 0, bytesRead);
            }
            
            target.closeEntry();
            addedEntries.add(entryName);
        } catch (Exception e) {
            System.err.println("Warning: Could not copy entry " + entryName + ": " + e.getMessage());
        }
    }
    
    private static void addGeneratedClasses(JarOutputStream jos, List<GeneratedClass> classes) throws Exception {
        for (GeneratedClass generatedClass : classes) {
            JarEntry entry = new JarEntry(generatedClass.getClassName());
            jos.putNextEntry(entry);
            jos.write(generatedClass.getBytecode());
            jos.closeEntry();
        }
    }
}
