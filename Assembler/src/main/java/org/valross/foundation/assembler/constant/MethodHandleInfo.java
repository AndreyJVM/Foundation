package org.valross.foundation.assembler.constant;

import org.valross.constantine.RecordConstant;
import org.valross.foundation.assembler.Data;
import org.valross.foundation.assembler.tool.PoolReference;
import org.valross.foundation.assembler.vector.U1;
import org.valross.foundation.assembler.vector.UVec;
import org.valross.foundation.detail.Member;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.constant.Constable;

public record MethodHandleInfo(ConstantType<MethodHandleInfo, Member.Invocation> tag, Member.Invocation invocation,
                               U1 reference_kind,
                               PoolReference reference_index)
    implements ConstantPoolInfo, Data, UVec, RecordConstant {

    @Override
    public UVec info() {
        return UVec.of(reference_kind, reference_index);
    }

    @Override
    public boolean is(Constable object) {
        return invocation.equals(object);
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        ConstantPoolInfo.super.write(stream);
    }

    @Override
    public int sort() {
        return 55;
    }

    @Override
    public Member.Invocation unpack() {
        final ConstantPoolInfo info = reference_index.ensure();
        final boolean isInterface = info.tag() == ConstantPoolInfo.INTERFACE_METHOD_REFERENCE;
        final Member member = (Member) info.unpack();
        return invocation != null ? invocation : member.dynamicInvocation(reference_kind.intValue(), isInterface);
    }

    @Override
    public int length() {
        return 4;
    }

}
