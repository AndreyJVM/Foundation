package org.valross.foundation.assembler.code;

import org.valross.constantine.Constant;
import org.valross.foundation.assembler.error.IncompatibleBranchError;
import org.valross.foundation.assembler.tool.CodeBuilder;
import org.valross.foundation.assembler.tool.ProgramRegister;
import org.valross.foundation.assembler.tool.ProgramStack;
import org.valross.foundation.assembler.vector.U2;
import org.valross.foundation.assembler.vector.U4;
import org.valross.foundation.assembler.vector.UVec;
import org.valross.foundation.detail.Type;
import org.valross.foundation.detail.TypeHint;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class Branch implements CodeElement {

    protected Handle handle = new Handle();
    protected TypeHint[] stack, register;
    protected TypeHint[] stackSnapshot, registerSnapshot;

    public Frame.Map toStackMap() {
        return new Frame.Map(registerSnapshot != null ? registerSnapshot : register, stackSnapshot != null ?
            stackSnapshot : stack);
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public byte[] binary() {
        return new byte[0];
    }

    @Override
    public void write(OutputStream stream) {
    }

    @Override
    public void insert(CodeBuilder builder) {
        this.handle.setVector(builder);
        CodeElement.super.insert(builder);
    }

    @Override
    public void notify(CodeBuilder builder) {
        this.handle.setVector(builder);
        if (builder.trackFrames()) this.checkFrame(builder.stack(), builder.register());
        if (builder.trackStack()) {
            this.snapshot();
            builder.stack().reframe(this.stack);
            builder.register().reframe(this.register);
        }
    }

    @Override
    public byte code() {
        return -1;
    }

    public void snapshot() {
        this.stackSnapshot = stack.clone();
        for (int i = 0; i < stackSnapshot.length; i++) {
            if (stackSnapshot[i] != null) stackSnapshot[i] = (TypeHint) stackSnapshot[i].constant();
        }
        this.registerSnapshot = register.clone();
        for (int i = 0; i < registerSnapshot.length; i++) {
            if (registerSnapshot[i] != null) registerSnapshot[i] = (TypeHint) registerSnapshot[i].constant();
        }
    }

    public Branch register(TypeHint... locals) {
        this.register = locals;
        return this;
    }

    public Branch stack(TypeHint... stack) {
        this.stack = stack;
        return this;
    }

    @Override
    public Constant constant() {
        return UVec.of(this.binary());
    }

    protected Handle getHandle() {
        return handle;
    }

    public int expectedIndex() {
        return handle.index();
    }

    protected UVec getJump(CodeElement source) {
        final int target = this.getHandle().index();
        int index = 0;
        for (CodeElement element : this.handle.vector.code) {
            if (element == source) break;
            else index += element.length();
        }
        final short jump = (short) (target - index);
        return U2.valueOf(jump);
    }

    protected U4 getWideJump(CodeElement source) {
        final int target = this.getHandle().index();
        int index = 0;
        for (CodeElement element : this.handle.vector.code) {
            if (element == source) break;
            else index += element.length();
        }
        final long jump = (target - index);
        return new U4(jump);
    }

    public void checkFrame(ProgramStack stack, ProgramRegister register) {
        if (this.stack == null || this.stack.length == 0) {
            this.stack = stack.toArray();
        } else if (!stack.isEmpty() && !this.isCompatible(this.stack, stack.toArray()))
            throw new IncompatibleBranchError("Expected stack to be " + this.printTable(this.stack)
                + " entering  branch " + this.getHandle().index()
                + " but found " + this.printTable(stack.toArray()));
        if (this.register == null || this.register.length == 0) {
            this.register = register.toArray();
        } else if (this.startsWith(register.toArray(), this.register)) {
            return; // we are chopping, todo check this is okay
        } else if (!register.isEmpty() && !this.isCompatible(this.register, register.toArray()))
            throw new IncompatibleBranchError("Expected register to be " + this.printTable(this.register)
                + " entering branch " + this.getHandle().index()
                + " but found " + this.printTable(register.toArray()));
    }

    private boolean startsWith(TypeHint[] theirs, TypeHint[] ours) {
        return this.isCompatible(ours, Arrays.copyOf(theirs, ours.length));
    }

    private boolean isCompatible(TypeHint[] ours, TypeHint[] theirs) {
        if (ours == theirs) return true;
        final int length = ours.length;
        if (theirs.length != length) return false;
        for (int i = 0; i < length; i++) {
            final TypeHint our = ours[i], their = theirs[i];
            if (Objects.equals(our, their) || Objects.equals(their, our)) continue;
            // Known null is assignable to any type, so it's okay if they change the type to void
            if (our != null && !our.isPrimitive() && Objects.equals(their, Type.VOID_WRAPPER)) continue;
            if (their != null && !their.isPrimitive() && Objects.equals(our, Type.VOID_WRAPPER)) continue;
            return false;
        }
        return true;
    }

    private String printTable(TypeHint[] array) {
        return '[' + String.join(", ", Arrays.stream(array).map(TypeHint::getTypeName).toList()) + ']';
    }

    @Override
    public String toString() {
        if (handle.vector == null) return "Branch";
        return "Branch[index=" + handle.index() + ", stack=" + Arrays.toString(stack) + ", register=" + Arrays.toString(register) + "]";
    }

    public static class ImplicitBranch extends Branch {

        public ImplicitBranch(Type... parameters) {
            this.register = parameters;
        }

    }

    public static class UnconditionalBranch extends Branch {

    }

    public static class UnreachableBranch extends Branch {

    }

    protected class Handle implements UVec {

        protected CodeBuilder builder;
        protected CodeVector vector;

        protected Handle(CodeBuilder builder) {
            this.setVector(builder);
        }

        protected Handle() {
            this(null);
        }

        public void setVector(CodeBuilder builder) {
            this.builder = builder;
            if (builder != null) vector = builder.vector();
            else vector = null;
        }

        public int index() {
            if (vector == null) return -1;
            int index = 0;
            for (CodeElement element : vector.code) {
                if (element == Branch.this) return index;
                else index += element.length();
            }
            return -1;
        }

        public boolean wide() {
            return false; // wide jumps aren't supported by the jvm (yet)
            // return this.index() > 65565;
        }

        @Override
        public void write(OutputStream stream) throws IOException {
            final short value = (short) this.index();
            stream.write((value >>> 8));
            stream.write(value);
        }

        @Override
        public int length() {
            return this.wide() ? 4 : 2;
        }

        @Override
        public byte[] binary() {
            final short value = (short) this.index();
            return new byte[] {(byte) (value >>> 8), (byte) (value)};
        }

        @Override
        public Constant constant() {
            return UVec.of(this.binary());
        }

        public Branch branch() {
            return Branch.this;
        }

    }

}
