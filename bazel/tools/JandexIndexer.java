package io.quarkus.bazel.tools;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Jandex indexer tool for Bazel.
 *
 * Generates Jandex index files from JAR files for Quarkus build-time scanning.
 */
public class JandexIndexer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: JandexIndexer --output <output.idx> <input1.jar> [input2.jar ...]");
            System.exit(1);
        }

        String outputFile = null;
        boolean merge = false;

        int i = 0;
        while (i < args.length) {
            if ("--output".equals(args[i]) && i + 1 < args.length) {
                outputFile = args[i + 1];
                i += 2;
            } else if ("--merge".equals(args[i])) {
                merge = true;
                i++;
            } else {
                break;
            }
        }

        if (outputFile == null) {
            System.err.println("Error: --output is required");
            System.exit(1);
        }

        Indexer indexer = new Indexer();

        // Process all input JAR files
        for (; i < args.length; i++) {
            String jarPath = args[i];
            System.out.println("Indexing JAR: " + jarPath);
            indexJar(indexer, jarPath);
        }

        // Write the index
        Index index = indexer.complete();
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            IndexWriter writer = new IndexWriter(out);
            writer.write(index);
        }

        System.out.println("Index written to: " + outputFile);
        System.out.println("Indexed " + index.getKnownClasses().size() + " classes");
    }

    private static void indexJar(Indexer indexer, String jarPath) throws Exception {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        indexer.index(in);
                    }
                }
            }
        }
    }
}
