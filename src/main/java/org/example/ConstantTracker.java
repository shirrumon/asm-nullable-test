package org.example;

import static org.objectweb.asm.Opcodes.*;

import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public class ConstantTracker extends Interpreter<ConstantTracker.ConstantValue> {
    static final ConstantValue NULL = new ConstantValue(BasicValue.REFERENCE_VALUE, null);
    public static final class ConstantValue implements Value {
        final Object value; // null if unknown or NULL
        final BasicValue type;
        ConstantValue(BasicValue type, Object value) {
            this.value = value;
            this.type = Objects.requireNonNull(type);
        }
        @Override public int getSize() { return type.getSize(); }
        @Override public String toString() {
            Type t = type.getType();
            if(t == null) return "uninitialized";
            return this == NULL? null:
                    value == null ? null: (String) value;
        }
        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(this == NULL || obj == NULL || !(obj instanceof ConstantValue))
                return false;
            ConstantValue that = (ConstantValue)obj;
            return Objects.equals(this.value, that.value)
                    && Objects.equals(this.type, that.type);
        }
        @Override
        public int hashCode() {
            if(this == NULL) return ~0;
            return (value==null? 7: value.hashCode())+type.hashCode()*31;
        }
    }

    BasicInterpreter basic = new BasicInterpreter(ASM5) {
        @Override public BasicValue newValue(Type type) {
            return type!=null && (type.getSort()==Type.OBJECT || type.getSort()==Type.ARRAY)?
                    new BasicValue(type): super.newValue(type);
        }
        @Override public BasicValue merge(BasicValue a, BasicValue b) {
            if(a.equals(b)) return a;
            if(a.isReference() && b.isReference())
                return BasicValue.REFERENCE_VALUE;
            return BasicValue.UNINITIALIZED_VALUE;
        }
    };

    public ConstantTracker() {
        super(ASM5);
    }

    @Override
    public ConstantValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        switch(insn.getOpcode()) {
            case ACONST_NULL: return NULL;
            case ICONST_M1: case ICONST_0: case ICONST_1: case ICONST_2:
            case ICONST_3: case ICONST_4: case ICONST_5:
                return new ConstantValue(BasicValue.INT_VALUE, insn.getOpcode()-ICONST_0);
            case LCONST_0: case LCONST_1:
                return new ConstantValue(BasicValue.LONG_VALUE, (long)(insn.getOpcode()-LCONST_0));
            case FCONST_0: case FCONST_1: case FCONST_2:
                return new ConstantValue(BasicValue.FLOAT_VALUE, (float)(insn.getOpcode()-FCONST_0));
            case DCONST_0: case DCONST_1:
                return new ConstantValue(BasicValue.DOUBLE_VALUE, (double)(insn.getOpcode()-DCONST_0));
            case BIPUSH: case SIPUSH:
                return new ConstantValue(BasicValue.INT_VALUE, ((IntInsnNode)insn).operand);
            case LDC:
                return new ConstantValue(basic.newOperation(insn), ((LdcInsnNode)insn).cst);
            default:
                BasicValue v = basic.newOperation(insn);
                return v == null? null: new ConstantValue(v, null);
        }
    }

    @Override
    public ConstantValue copyOperation(AbstractInsnNode insn, ConstantValue value) {
        return value;
    }

    @Override
    public ConstantValue newValue(Type type) {
        BasicValue v = basic.newValue(type);
        return v == null? null: new ConstantValue(v, null);
    }

    @Override
    public ConstantValue unaryOperation(AbstractInsnNode insn, ConstantValue value) throws AnalyzerException {
        BasicValue v = basic.unaryOperation(insn, value.type);
        return v == null? null: new ConstantValue(v, insn.getOpcode()==CHECKCAST? value.value: null);
    }

    @Override
    public ConstantValue binaryOperation(AbstractInsnNode insn, ConstantValue a, ConstantValue b) throws AnalyzerException {
        BasicValue v = basic.binaryOperation(insn, a.type, b.type);
        return v == null? null: new ConstantValue(v, null);
    }

    @Override
    public ConstantValue ternaryOperation(AbstractInsnNode insn, ConstantValue a, ConstantValue b, ConstantValue c) {
        return null;
    }

    @Override
    public ConstantValue naryOperation(AbstractInsnNode insn, List<? extends ConstantValue> values) throws AnalyzerException {
        List<BasicValue> unusedByBasicInterpreter = null;
        BasicValue v = basic.naryOperation(insn, unusedByBasicInterpreter);
        return v == null? null: new ConstantValue(v, null);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, ConstantValue value, ConstantValue expected) {}

    @Override
    public ConstantValue merge(ConstantValue a, ConstantValue b) {
        if(a == b) return a;
        BasicValue t = basic.merge(a.type, b.type);
        return t.equals(a.type) && (a.value==null&&a!=NULL || Objects.equals(a.value, b.value))? a:
                t.equals(b.type) &&  b.value==null&&b!=NULL? b: new ConstantValue(t, null);
    }
}