package com.seaofnodes.simple.node;

import com.seaofnodes.simple.*;
import java.io.ByteArrayOutputStream;

public interface MachNode {

    // Easy access to the abstract Machine
    default Machine machine() { return CodeGen.CODE._mach; }

    // Run a post-instruction-selection action
    default void postSelect() { }

    // Register mask allowed on input i.  0 for no register.
    abstract public RegMask regmap(int i);
    // Register mask allowed as a result.  0 for no register.
    abstract public RegMask outregmap();
    // Multi-reg-defining machine op; idx comes from Proj.
    // Sometimes these Projs have no uses, and just exist to kill a register.
    default RegMask outregmap(int idx) { throw Utils.TODO(); }

    // If this op updates the same register as input 'idx', e.g. many X86 "R1 +=
    // R2" style ops.  This forces the output live range to equal the input live
    // range, so they both get the same register.

    // This can hold true for projections, if the instruction updates many
    // registers that it also takes in.  Note that ops also use the same
    // registers for both inputs and outputs but these are pinned and without any
    // choices (e.g. always RCX for both an input and output) and are better
    // served with just declaring as a fixed register.

    // Returns either 0 for not-two-adr or the updated input; for projections
    // this is their Multi's updated input.
    default int twoAddress( ) { return 0; }

    // Instructions cheaper to recreate than to spill, such as loading small constants
    default boolean isClone() { return false; }
    // Encoding is appended into the byte array; size is returned
    abstract public int encoding(ByteArrayOutputStream bytes);

    // Human-readable form appended to the SB.  Things like the encoding,
    // indentation, leading address or block labels not printed here.
    // Just something like "R17=[R18+12] // Load array base".
    // General form: "dst=src+src".
    default void asm(CodeGen code, SB sb) { throw Utils.TODO(); }

    // Human readable opcode; e.g. "ld4" or "call".
    default String op() { throw Utils.TODO(); }
}
