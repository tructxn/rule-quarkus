package io.quarkus.bazel.generator;

/**
 * Represents a generated class with its bytecode.
 */
public class GeneratedClass {
    private final String className;
    private final byte[] bytecode;
    
    public GeneratedClass(String className, byte[] bytecode) {
        this.className = className;
        this.bytecode = bytecode;
    }
    
    public String getClassName() {
        return className;
    }
    
    public byte[] getBytecode() {
        return bytecode;
    }
}
