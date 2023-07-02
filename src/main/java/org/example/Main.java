package org.example;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class Main {
    public static void main(String[] args) throws IOException, AnalyzerException {
        ClassReader reader = new ClassReader("org.example.TestCase");
        ClassNode classNode = new ClassNode();
        reader.accept(classNode,0);

        ClassNullableChecker cnc = new ClassNullableChecker();
        cnc.checkAllNull();
    }
}