package mx.kenzie.foundation.detail;

import mx.kenzie.foundation.assembler.code.CodeElement;
import mx.kenzie.foundation.assembler.code.CodeVector;
import mx.kenzie.foundation.assembler.tool.InstructionReference;
import mx.kenzie.foundation.assembler.vector.UVec;
import org.valross.constantine.RecordConstant;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Something that resembles (or is indicative of) a Java class.
 * This might be: 1. the Type handle for an actual, loaded class, 2. a Type reference to an unloaded class,
 * 3. A theoretical but unknown type (e.g. TOP), 4. An un-initialised known type (e.g. NEW xyz...),
 * or 5. An un-initialised unknown type (THIS before super-constructor call).
 */
public interface TypeHint extends Descriptor, Type {

    static TypeHint top() {
        return Top.TOP;
    }

    static TypeHint uninitialisedThis(TypeHint real) {
        return new This(real);
    }

    static TypeHint uninitialised(mx.kenzie.foundation.detail.Type type, CodeElement allocation, CodeVector vector) {
        return new Uninitialised(type, new InstructionReference(vector, allocation));
    }

    static TypeHint uninitialised(mx.kenzie.foundation.detail.Type type, UVec offset) {
        return new Uninitialised(type, offset);
    }

    static TypeHint none() {
        return mx.kenzie.foundation.detail.Type.VOID_WRAPPER;
    }

    static int width(TypeHint... types) {
        int width = 0;
        for (TypeHint type : types) width += type.width();
        return width;
    }

    default int width() {
        return 1;
    }

    default boolean isPrimitive() {
        return false;
    }

    default boolean isInitialisedType() {
        return true;
    }

    default boolean isRealType() {
        return true;
    }

    default boolean isTypeKnown() {
        return true;
    }

    default mx.kenzie.foundation.detail.Type asType() {
        return mx.kenzie.foundation.detail.Type.of(this);
    }

    interface InitialisableType extends TypeHint {

        void initialise();

    }

    class Uninitialised implements TypeHint, InitialisableType {

        private final mx.kenzie.foundation.detail.Type type;
        private final UVec offset;
        private boolean initialised;

        public Uninitialised(mx.kenzie.foundation.detail.Type type, UVec offset) {
            this.type = type;
            this.offset = offset;
            this.initialised = false;
        }

        @Override
        public boolean isInitialisedType() {
            return initialised;
        }

        @Override
        public String descriptorString() {
            return type.descriptorString();
        }

        @Override
        public String getTypeName() {
            return type.getTypeName();
        }

        @Override
        public void initialise() {
            this.initialised = true;
        }

        @Override
        public mx.kenzie.foundation.detail.Type constant() {
            return type;
        }

        public mx.kenzie.foundation.detail.Type type() {
            return type;
        }

        public UVec offset() {
            return offset;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, offset);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Uninitialised) obj;
            return Objects.equals(this.type, that.type) && Objects.equals(this.offset, that.offset);
        }

        @Override
        public String toString() {
            return "Uninitialised[" + "initialised=" + initialised + ", " + "type=" + type + ", " + "offset=" + offset + ']';
        }

    }

    final class This implements TypeHint, TypeHint.InitialisableType, RecordConstant {

        private final mx.kenzie.foundation.detail.TypeHint real;
        private boolean initialised;

        This(TypeHint real) {
            this.real = real;
        }

        @Override
        public boolean isInitialisedType() {
            return initialised;
        }

        @Override
        public boolean isRealType() {
            return initialised;
        }

        @Override
        public String getTypeName() {
            return initialised ? real.getTypeName() : "this";
        }

        @Override
        public String descriptorString() {
            return initialised ? real.descriptorString() : "this";
        }

        @Override
        public void initialise() {
            this.initialised = true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(real);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (This) obj;
            return Objects.equals(this.real, that.real);
        }

        @Override
        public String toString() {
            return "This[" + "real=" + real + ']';
        }

    }

    record Guess(int width, boolean isPrimitive, boolean isInitialisedType, boolean isRealType, boolean isTypeKnown,
                 String descriptorString, String getTypeName) implements TypeHint, RecordConstant {}

}

record Top() implements TypeHint, RecordConstant {

    static final Top TOP = new Top();

    @Override
    public boolean isInitialisedType() {
        return false;
    }

    @Override
    public boolean isRealType() {
        return false;
    }

    @Override
    public boolean isTypeKnown() {
        return false;
    }

    @Override
    public String descriptorString() {
        return "T";
    }

}
