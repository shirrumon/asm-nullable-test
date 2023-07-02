package org.example;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

public class ClassNullableChecker {
    public void checkAllNull() throws IOException, AnalyzerException {
        ClassReader reader = new ClassReader("org.example.TestCase");
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        List<MethodNode> methods = classNode.methods;

        for (MethodNode methodNode : methods) {
            InsnList m_instructionList = methodNode.instructions;

            for (int count = 0; count < m_instructionList.size(); count++) {
                AbstractInsnNode instruction = m_instructionList.get(count);
                excessiveNullCheck(classNode, methodNode, instruction);
            }
        }
    }

    private void excessiveNullCheck(
            ClassNode classNode,
            MethodNode methodNode,
            AbstractInsnNode instruction
    ) throws AnalyzerException {
        AbstractInsnNode previousInstruction = instruction.getPrevious();

        if (instruction.getOpcode() == IFNULL || instruction.getOpcode() == IFNONNULL) {
            String checkedValue = getFrameValue(classNode, methodNode, previousInstruction);
            if(checkedValue != null) {
                System.out.println("Excessive null check "+checkedValue);
            }
        }
    }

    private String getFrameValue(
            ClassNode classNode,
            MethodNode methodNode,
            AbstractInsnNode instruction
    ) throws AnalyzerException {
        String value = null;
        Analyzer<ConstantTracker.ConstantValue> analyzer = new Analyzer<>(new ConstantTracker());
        analyzer.analyze(classNode.name, methodNode);

        for (Frame<ConstantTracker.ConstantValue> frame : analyzer.getFrames()) {
            if (frame == null) continue;
            if (instruction.getOpcode() != Opcodes.ALOAD) continue;

            VarInsnNode vn = (VarInsnNode) instruction;
            ConstantTracker.ConstantValue var = frame.getLocal(vn.var);

            value = Objects.equals(var.type.toString(), "R") ? "realNull" : String.valueOf(var);
        }
        return value;
    }
}