package com.seaofnodes.simple.node.cpus.riscv;

import com.seaofnodes.simple.*;
import com.seaofnodes.simple.codegen.CodeGen;
import com.seaofnodes.simple.codegen.RegMask;
import com.seaofnodes.simple.node.*;
import java.io.ByteArrayOutputStream;

public class CallRRISC extends CallNode implements MachNode{
    CallRRISC( CallNode call ) { super(call); }

    @Override public String label() { return op(); }
    @Override public RegMask regmap(int i) {
        // Todo: float or int?
        return i==_inputs._len
            ? riscv.RMASK                // Function call target
            : riscv.callInMask(tfp(),i); // Normal argument
    }
    @Override public RegMask outregmap() { return null; }

    // Encoding is appended into the byte array; size is returned
    @Override public int encoding(ByteArrayOutputStream bytes) {
        throw Utils.TODO();
    }

    @Override public void asm(CodeGen code, SB sb) {
        sb.p(code.reg(fptr())).p("  ");
        for( int i=0; i<nargs(); i++ )
            sb.p(code.reg(arg(i))).p("  ");
        sb.unchar(2);
    }

    @Override public String op() { return "callr"; }

}
