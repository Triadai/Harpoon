// CodeGen.spec, created Tue Jul 6 12:12:41 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.MIPS;

import harpoon.Backend.Maps.NameMap;
import harpoon.Backend.StrongARM.TwoWordTemp;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HMethod;
import harpoon.IR.Assem.Instr;
import harpoon.IR.Assem.InstrEdge;
import harpoon.IR.Assem.InstrMEM;
import harpoon.IR.Assem.InstrJUMP;
import harpoon.IR.Assem.InstrMOVE;
import harpoon.IR.Assem.InstrCALL;
import harpoon.IR.Assem.InstrLABEL;
import harpoon.IR.Assem.InstrDIRECTIVE;
import harpoon.IR.Assem.InstrFactory;
import harpoon.IR.Tree.TreeDerivation;
import harpoon.IR.Tree.Bop;
import harpoon.IR.Tree.Uop;
import harpoon.IR.Tree.Type;
import harpoon.IR.Tree.TEMP;
import harpoon.IR.Tree.Typed;
import harpoon.IR.Tree.PreciselyTyped;
import harpoon.IR.Tree.ExpList;
import harpoon.Util.Util;
import harpoon.Temp.TempList;
import harpoon.Temp.Temp;
import harpoon.Temp.LabelList;
import harpoon.Temp.Label;

import harpoon.IR.Tree.BINOP;
import harpoon.IR.Tree.CALL;
import harpoon.IR.Tree.INVOCATION;
import harpoon.IR.Tree.CJUMP;
import harpoon.IR.Tree.CONST;
import harpoon.IR.Tree.EXPR;
import harpoon.IR.Tree.JUMP;
import harpoon.IR.Tree.LABEL;
import harpoon.IR.Tree.MEM;
import harpoon.IR.Tree.MOVE;
import harpoon.IR.Tree.NAME;
import harpoon.IR.Tree.NATIVECALL;
import harpoon.IR.Tree.OPER;
import harpoon.IR.Tree.RETURN;
import harpoon.IR.Tree.TEMP;
import harpoon.IR.Tree.UNOP;
import harpoon.IR.Tree.SEQ;

import harpoon.Util.Collections.MultiMap;
import harpoon.Util.Collections.GenericMultiMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;


/**
 * <code>MIPS.CodeGen</code> is a code-generator for the MIPS II architecture.
 * 
 * @see Kane, <U>MIPS Risc Architecture </U>
 * @author  Emmett Witchel <witchel@lcs.mit.edu>
 * @version $Id: CodeGen.spec,v 1.1.2.21 2000-10-11 18:22:17 witchel Exp $
 */
// All calling conventions and endian layout comes from observing gcc
// for vpekoe.  This is standard for cc on MIPS IRIX64 lion 6.2 03131016 IP19.
// as well.
// MIPS is big endian by default.  This also applies to long longs and
// doubles (for which we are using software fp.  The most sinifigant
// 32 bit quantity is at the lower address (and in v0, as opposed to
// v1) 
%%

    private void collectCallInfo(harpoon.IR.Tree.Data data) {}
    private void collectCallInfo(harpoon.IR.Tree.Code code) {
       final class CallVisitor extends harpoon.IR.Tree.TreeVisitor {
          public void visit(harpoon.IR.Tree.Tree treee){
             harpoon.Util.Util.assert(false, "Should never visit generic harpoon.IR.Tree.Treein CallVisitor");
          } // end visit(harpoon.IR.Tree.Tree)
          public void visit(harpoon.IR.Tree.Stm stm) {
             harpoon.IR.Tree.INVOCATION inv = null;
             if(stm instanceof harpoon.IR.Tree.CALL) {
                inv = (harpoon.IR.Tree.INVOCATION)stm;
             }
             if(stm instanceof harpoon.IR.Tree.NATIVECALL) {
                inv = (harpoon.IR.Tree.INVOCATION)stm;
             }
             if(inv != null) {
                stack.callInfo(inv);
             }
          } // end visit(harpoon.IR.Tree.Stm)
          public void visit(harpoon.IR.Tree.SEQ treee){
             treee.getLeft().accept(this);
             treee.getRight().accept(this);
          }
       }
       CallVisitor visitor = new CallVisitor();
       harpoon.IR.Tree.Tree t = (harpoon.IR.Tree.Tree) code.getRootElement();
       t.accept(visitor);
    }

    // InstrFactory to generate Instrs from
    private InstrFactory instrFactory;

    // Track information about the activation frame for this method.
    private StackInfo stack;
    

    /*final*/ RegFileInfo regfile;
    private Temp  v0, v1, a0, a1, a2, a3, t0, t1, t2, 
       t3, t4, t5, t6, t7, t8, t9, FP, SP, LR;
    Comparator regComp;
    private /*final*/ Temp call_use[];
    /* The maximum def set for a call */
    private Temp call_def_full[];
    /* The def set we use in our support functions */
    private  Temp call_def_builtin[];

    // whether to generate stabs debugging information in output (-g flag)
    // Currently broken on MIPS
    private static final boolean stabsDebugging = true;
    private static final boolean stabsLineDebugging = false;

    // NameMap for calling C functions.
    NameMap nameMap;

    // whether to generate a.out-style or elf-style segment directives
    private final boolean is_elf;
    // mipspro assember requires section attributes in .section directive
    // GNU as requires that they be absent
    // NB: Our output will not work with mipspro assemblers!
    private final boolean mipspro_assem = false;
    // whether to use soft-float or hard-float calling convention.
    private final boolean soft_float = false; // skiffs use hard-float

    public CodeGen(Frame frame, boolean is_elf) {
       super(frame);
	last = null;
	this.regfile = (RegFileInfo) frame.getRegFileInfo();
	this.nameMap = frame.getRuntime().nameMap;
	this.is_elf = is_elf;
    v0 = regfile.V0;
    v1 = regfile.V1;
    a0 = regfile.A0;
    a1 = regfile.A1;
    a2 = regfile.A2;
    a3 = regfile.A3;
	t0 = regfile.T0;
    t1 = regfile.T1;
    t2 = regfile.T2;
    t3 = regfile.T3;
    t4 = regfile.T4;
    t5 = regfile.T5;
    t6 = regfile.T6;
    t7 = regfile.T7;
    t8 = regfile.T8;
    t9 = regfile.T9;
	FP = regfile.FP; // s8, but C on IRIX/MIPS doesn't use an fp
	SP = regfile.SP; // reg 29
	LR = regfile.LR; // reg 31
    call_use = new Temp[] {a0, a1, a2, a3, SP, FP};
    call_def_full = new Temp[] {v0, v1, a0, a1, a2, a3, t0, t1, 
                                t2, t3, t4, t5, t6, t7, t8, t9, 
                                LR};
    call_def_builtin = new Temp[] {v0, v1, a0, a1, a2, a3, t0, t1, t2,
                                   LR};


    // allow sorting of registers so that stm and ldm work correctly.
    final Map regToNum = new HashMap();
    for (int i=0; i<regfile.reg.length; i++)
       regToNum.put(regfile.reg[i], new Integer(i));
    regComp = new Comparator() {
          public int compare(Object o1, Object o2) {
             Util.assert(regToNum.keySet().contains(o1),
                         o1+" not in regToNum's keys");
             Util.assert(regToNum.keySet().contains(o2),
                         o2+" not in regToNum's keys");
             return ((Integer)regToNum.get(o1)).intValue() -
                ((Integer)regToNum.get(o2)).intValue();
          }
       };
    }



    /** Sub-class to represent delay-slots.
     * <code>optimize()</code> uses this class information to determine that
     * it should rearrange code to try to eliminate these instructions.
     * @author Emmett Witchel
     */
    private class InstrDELAYSLOT extends Instr {
       // a nop to fill the delay slot
       public InstrDELAYSLOT(InstrFactory inf, HCodeElement source) {
          super(inf, source, "nop  # delay slot");
       }
    }
    private String GetLdSuffix(MEM root) {
       String suffix = "";
       if(root.isSmall()) {
          switch(root.bitwidth()) {
          case 8:
             if(root.signed())
                suffix = "b";
             else
                suffix = "bu";
             break;
          case 16:
             if(root.signed())
                suffix = "h";
             else
                suffix = "hu";
             break;
          default:
             Util.assert(false, "Constant offset memory load wants to load bitwidth that is not 8, 16");       
          }
       } else {
          suffix = "w";
       }
       return suffix;
    }
    private String GetStSuffix(MOVE root) {
       String suffix = "";
       if(((MEM)root.getDst()).isSmall()) {
          switch(((MEM)root.getDst()).bitwidth()) {
          case 8:
             suffix = "b";
             break;
          case 16:
             suffix = "h";
             break;
          default:
             Util.assert(false, "Constant offset memory load wants to load bitwidth that is not 8, 16, or 32");       
          }
       } else {
          suffix = "w";
       }
       return suffix;
    }

    /** The main Instr layer; nothing below this line should call
	emit(Instr) directly, unless they are constructing extensions
	of Instr.
    */
    private Instr emit(HCodeElement root, String assem,
                       Temp[] dst, Temp[] src,
                       boolean canFallThrough, List targets) {
       return emit(new Instr( instrFactory, root, assem,
                              dst, src, canFallThrough, targets));
    }		      

    /** Secondary emit layer; for primary usage by the other emit
	methods. 
    */
    private Instr emit2(HCodeElement root, String assem,
                        Temp[] dst, Temp[] src) {
	return emit(root, assem, dst, src, true, null);
    }
    /** Single dest Single source Emit Helper. */
    private Instr emit( HCodeElement root, String assem, 
		       Temp dst, Temp src) {
	return emit2(root, assem, new Temp[]{ dst }, new Temp[]{ src });
    }
    /** Single dest Two source Emit Helper. */
    private Instr emit( HCodeElement root, String assem, 
		       Temp dst, Temp src1, Temp src2) {
	return emit2(root, assem, new Temp[]{ dst },
			new Temp[]{ src1, src2 });
    }
    /** Single dest Null source Emit Helper. */
    private Instr emit( HCodeElement root, String assem, Temp dst) {
       return emit2(root, assem, new Temp[]{ dst }, null);
    }
    /** Null dest Null source Emit Helper. */
    private Instr emit( HCodeElement root, String assem ) {
	return emit2(root, assem, null, null);
    }

    /** Single dest Single source emit InstrMOVE helper */
    private Instr emitMOVE( HCodeElement root, String assem,
			   Temp dst, Temp src) {
       return emit(new InstrMOVE( instrFactory, root, assem+"         # move",
                                  new Temp[]{ dst },
			    new Temp[]{ src }));
    }			         

    /* Branching instruction emit helper. 
       Instructions emitted using this *can* fall through.
    */
    private Instr emit( HCodeElement root, String assem,
		       Temp[] dst, Temp[] src, Label[] targets ) {
        return emit(new Instr( instrFactory, root, assem,
			dst, src, true, Arrays.asList(targets)));
    }
    
    /* Branching instruction emit helper. 
       Instructions emitted using this *cannot* fall through.
    */
    private Instr emitNoFall( HCodeElement root, String assem,
		       Temp[] dst, Temp[] src, Label[] targets ) {
        return emit(new Instr( instrFactory, root, assem,
			dst, src, false, Arrays.asList(targets)));
    }

    /* Call instruction emit helper. 
       Instructions emitted using this *cannot* fall through.
    */
    private Instr emitCallNoFall( HCodeElement root, String assem,
		       Temp[] dst, Temp[] src, Label[] targets ) {
	List tlist = (targets==null?null:Arrays.asList(targets));
        return emit(new InstrCALL( instrFactory, root, assem,
				   dst, src, false, tlist));
    }

    private Instr emitNativeCall( HCodeElement root, String assem,
                                  Temp[] dst, Temp[] src, 
                                  boolean canFall, Label[] targets) {
       List tlist = (targets==null?null:Arrays.asList(targets));
       return emit(new InstrCALL( instrFactory, root, assem,
                                  dst, src, canFall, tlist));
    }

    /* InstrJUMP emit helper; automatically adds entry to
       label->branches map. */ 
    private Instr emitJUMP( HCodeElement root, String assem, Label l ) {
       return emit( new InstrJUMP( instrFactory, root, assem, l ));
    }
    private Instr emitCondBr( HCodeElement root, String assem, Temp srca,
                            Temp srcb, Label l ) {
       if(srca == null && srcb == null) {
          return emitJUMP(root, assem, l);
       } else if (srca == null) {
          return emit( root, assem, null, new Temp[] {srcb}, true,
                       Arrays.asList(new Label[] { l }) );
       } else if (srcb == null) {
          return emit( root, assem, null, new Temp[] {srca}, true,
                       Arrays.asList(new Label[] { l }) );
       } else {
          return emit( root, assem, null, new Temp[] {srca, srcb}, true,
                       Arrays.asList(new Label[] { l }) );
       }
    }

    /* InstrLABEL emit helper. */
    private Instr emitLABEL( HCodeElement root, String assem, Label l ) {
	return emit( new InstrLABEL( instrFactory, root, assem, l ));
    }	
    /* InstrLABEL emit helper. */
    private Instr emitNoFallLABEL( HCodeElement root, String assem, Label l ) {
	return emit( InstrLABEL.makeNoFall( instrFactory, root, assem, l ));
    }	

    /* InstrDIRECTIVE emit helper. */
    private Instr emitDIRECTIVE( HCodeElement root, String assem ) {
	return emit( new InstrDIRECTIVE( instrFactory, root, assem ));
    }
    /* InstrDIRECTIVE emit helper. */
    private Instr emitNoFallDIRECTIVE( HCodeElement root, String assem ) {
	return emit( InstrDIRECTIVE.makeNoFall( instrFactory, root, assem ));
    }
    private Instr emitDELAYSLOT(HCodeElement root) {
        return emit(new InstrDELAYSLOT(instrFactory, root));
    }
    private void emitLineDebugInfo(HCodeElement root) {
       if(stabsLineDebugging && stabsDebugging) {
          Label l = new Label();
          emit( new InstrLABEL(instrFactory, root, l.toString() + ":", l) );
          emit( new InstrDIRECTIVE(instrFactory, root, 
                                   ".stabn 68,0," + root.getLineNumber()
                                   + "," + l));
       }
    }		      


    private boolean is16BitOffset(long val) {
	// addressing mode two takes a 12 bit unsigned offset, with
	// an additional bit in the instruction word indicating whether
	// to add or subtract this offset.  This means that there
	// are two representations for zero offset: +0 and -0.
	long absval = (val<0)?-val:val;
	return (absval&(~0xFFFF))==0;
    }
    private boolean is16BitOffset(Number n) {
	if (n instanceof Double || n instanceof Float) return false;
	else return is16BitOffset(n.longValue());
    }

    // helper for operand2 shifts
    private boolean is5BitShift(long val) {
	return (val>=0) && (val<=31);
    }
    private boolean is5BitShift(Number n) {
	if (n instanceof Double || n instanceof Float) return false;
	else return is5BitShift(n.longValue());
    }
    private boolean isShiftOp(int op) {
	switch (op) {
	case Bop.SHL: case Bop.SHR: case Bop.USHR: return true;
	default: return false;
	}
    }
    private String shiftOp2Str(int op) {
	switch (op) {
	case Bop.SHL: return "sll";
	case Bop.SHR: return "sra";
	case Bop.USHR: return "srl";
	default: throw new Error("Illegal shift operation");
	}
    }
    // helper for comparison operations
    private boolean isCmpOp(int op) {
	switch (op) {
	case Bop.CMPEQ: case Bop.CMPNE:
	case Bop.CMPGT: case Bop.CMPGE:
	case Bop.CMPLT: case Bop.CMPLE: return true;
	default: return false;
	}
    }
    // Given a comparison operation, return the implementation in MIPS
    // assembler
    private String cmpOp2AsStr(int op) {
	switch (op) {
	case Bop.CMPEQ: return "seq";
	case Bop.CMPNE: return "sne";
	case Bop.CMPGT: return "sgt";
	case Bop.CMPGE: return "sge";
	case Bop.CMPLE: return "sle";
	case Bop.CMPLT: return "slt";
	default: throw new Error("Illegal compare operation");
	}
    }
    // Given a comparison operation, return the corresponding branch
    // in MIPS assembler
    private String cmpOp2BrStr(int op) {
	switch (op) {
	case Bop.CMPEQ: return "beq";
	case Bop.CMPNE: return "bne";
	case Bop.CMPGT: return "bgt";
	case Bop.CMPGE: return "bge";
	case Bop.CMPLE: return "ble";
	case Bop.CMPLT: return "blt";
	default: throw new Error("Illegal compare operation");
	}
    }

    /** simple tuple class to wrap some bits of info about the call prologue */
    private class CallState {
      /** set of registers used by parameters to the call. */
      final List callUses;
      CallState(List callUses) {
         this.callUses=callUses;
      }
    }

    /** Declare Void types for a0, a1, a2, a3, t0, LR in prep for a call. */
    private void declareCALL() {
       declare(a0, HClass.Void);
       declare(a1, HClass.Void);
       declare(a2, HClass.Void);
       declare(a3, HClass.Void);
       declare(t0, HClass.Void);
       declare(LR, HClass.Void);
    }
    private void declareCALLDefFull() {
       declare(v0, HClass.Void);
       declare(v1, HClass.Void);
       declare(a0, HClass.Void);
       declare(a1, HClass.Void);
       declare(a2, HClass.Void);
       declare(a3, HClass.Void);
       declare(t0, HClass.Void);
       declare(t1, HClass.Void);
       declare(t2, HClass.Void);
       declare(t3, HClass.Void);
       declare(t4, HClass.Void);
       declare(t5, HClass.Void);
       declare(t6, HClass.Void);
       declare(t7, HClass.Void);
       declare(t8, HClass.Void);
       declare(t9, HClass.Void);
       declare(LR, HClass.Void);
    }
    private void declareCALLDefBuiltin() {
       declare(v0, HClass.Void);
       declare(v1, HClass.Void);
       declare(a0, HClass.Void);
       declare(a1, HClass.Void);
       declare(a2, HClass.Void);
       declare(a3, HClass.Void);
       declare(t0, HClass.Void);
       declare(t1, HClass.Void);
       declare(t2, HClass.Void);
       declare(LR, HClass.Void);
    }
    private void DoLLCall(HCodeElement root,
                          Temp i, Temp j, Temp k, String func_name) {
       declare( a3, HClass.Void );
       declare( a2, HClass.Void );
       declare( a1, HClass.Void );
       declare( a0, HClass.Void );
       // an emitMOVE is not legal with the l/h modifiers
       Util.assert(j instanceof TwoWordTemp);
       Util.assert(k instanceof TwoWordTemp);
       emit( root, "move `d0, `s0h", a0, j );
       emit( root, "move `d0, `s0l", a1, j );
       emit( root, "move `d0, `s0h", a2, k );
       emit( root, "move `d0, `s0l", a3, k );
       declareCALLDefFull();
       emit2(root, "jal "+nameMap.c_function_name(func_name),
             call_def_full, call_use);
       Util.assert(i instanceof TwoWordTemp);
       emit( root, "move `d0h, `s0", i, v0 );
       emit( root, "move `d0l, `s0", i, v1 );
    }
    /** Source for long long arithmetic routines is in Flex_MIPS.S */
    private void DoLLShiftCall(HCodeElement root,
                               Temp i, Temp j, Temp k, String func_name) {
       declare( a2, HClass.Int );
       declare( a1, HClass.Void );
       declare( a0, HClass.Void );
       declareCALLDefBuiltin();
       // an emitMOVE is not legal with the l/h modifiers
       Util.assert(j instanceof TwoWordTemp);
       emit( root, "move `d0, `s0h", a0, j );
       emit( root, "move `d0, `s0l", a1, j );
       Util.assert((k instanceof TwoWordTemp) == false);
       emitMOVE( root, "move `d0, `s0", a2, k );
       declareCALLDefFull();
       emit2(root, "jal "+nameMap.c_function_name(func_name),
             // uses & stomps on these registers:
             call_def_full, call_use);
       Util.assert(i instanceof TwoWordTemp);
       emit( root, "move `d0h, `s0", i, v0 );
       emit( root, "move `d0l, `s0", i, v1 );
    }
    /** For now all float and double operations are in software.  Each
     routine has an assembly jacket function which now calls a C
     function.  Eventually, we can implement them in assembly using
     fewer registers if we want*/
    private void DoFCall(HCodeElement root,
                         Temp i, Temp j, Temp k, String func_name) {
       /* call auxillary fp routines */
       declare(a0, HClass.Float);
       declare(a1, HClass.Float);
       emitMOVE( root, "move `d0, `s0", a1, k );
       emitMOVE( root, "move `d0, `s0", a0, j );
       declareCALLDefFull();
       emit2( root, "jal "+nameMap.c_function_name(func_name),
              call_def_full, new Temp[] {a0, a1, SP, FP} );
       emitMOVE( root, "move `d0, `s0", i, v0 );
    }
    private void DoFCall(HCodeElement root,
                         Temp i, Temp j, String func_name) {
       /* call auxillary fp routines */
       declare(a0, HClass.Float);
       emitMOVE( root, "move `d0, `s0", a0, j );
       declareCALLDefFull();
       emit2( root, "jal "+nameMap.c_function_name(func_name),
              call_def_full, new Temp[] {a0, a1, SP, FP} );
       emitMOVE( root, "move `d0, `s0", i, v0 );
    }
    private void DoDCall(HCodeElement root,
                         Temp i, Temp j, Temp k, String func_name) {
       /* call auxillary fp routines */
       declare( a3, HClass.Void );
       declare( a2, HClass.Void );
       declare( a1, HClass.Void );
       declare( a0, HClass.Void );
       // not certain an emitMOVE is legal with the l/h modifiers
       Util.assert(j instanceof TwoWordTemp);
       Util.assert(k instanceof TwoWordTemp);
       emit( root, "move `d0, `s0h", a2, k );
       emit( root, "move `d0, `s0l", a3, k );
       emit( root, "move `d0, `s0h", a0, j );
       emit( root, "move `d0, `s0l", a1, j );
       declareCALLDefFull();
       emit2(root, "jal "+nameMap.c_function_name(func_name),
             // uses & stomps on these registers:
             call_def_full, call_use);
       if(i instanceof TwoWordTemp) {
          emit( root, "move `d0h, `s0", i, v0 );
          emit( root, "move `d0l, `s0", i, v1 );
       } else {
          emitMOVE( root, "move `d0, `s0", i, v0 );
       }
    }
    private void DoDCall(HCodeElement root,
                         Temp i, Temp j, String func_name) {
       /* call auxillary fp routines */
       declare( a0, HClass.Void );
       if(j instanceof TwoWordTemp) {
          declare( a1, HClass.Void );
          // not certain an emitMOVE is legal with the l/h modifiers
          emit( root, "move `d0, `s0h", a0, j );
          emit( root, "move `d0, `s0l", a1, j );
       } else {
          emitMOVE( root, "move `d0, `s0", a0, j );
       }
       declareCALLDefFull();
       emit2(root, "jal "+nameMap.c_function_name(func_name),
             // uses & stomps on these registers:
             call_def_full, call_use);
       if(i instanceof TwoWordTemp) {
          emit( root, "move `d0h, `s0", i, v0 );
          emit( root, "move `d0l, `s0", i, v1 );
       } else {
          emitMOVE( root, "move `d0, `s0", i, v0 );
       }
    }

    private boolean isDoubleWord(Typed ty) {
       switch (ty.type()) {
       case Type.LONG: case Type.DOUBLE: return true;
       default: return false;
       }
    }
    /** Helper for setting up registers/memory with the strongARM standard
     *  calling convention.  Returns the stack offset necessary,
     *  along with a set of registers used by the parameters. */
    private CallState emitCallPrologue(INVOCATION ROOT, 
                                       TempList tlist,
                                       TreeDerivation td) {
       ExpList elist = ROOT.getArgs();
       List callUses = new ArrayList(call_use.length);

       // add all used registers to callUses list.
       for (int i=0; i<call_use.length; ++i) {
          callUses.add(call_use[i]);
       }

       int index = 0;
       for (TempList tl = tlist; tl != null; tl=tl.tail,
               elist=elist.tail, ++index) {
          switch(stack.argWhere(ROOT, index)) {
          case StackInfo.REGISTER:
             if(isDoubleWord(elist.head)) {
                Temp reg1 = stack.argReg(ROOT, index); 
                Temp reg2 = stack.argSecondReg( ROOT, index); 
                declare( reg1, HClass.Void);
                declare( reg2, HClass.Void);
                Util.assert(tl.head instanceof TwoWordTemp);
                emit( ROOT, "move `d0, `s0h", reg1, tl.head );
                emit( ROOT, "move `d0, `s0l", reg2, tl.head );
             } else {
                Temp reg = stack.argReg( ROOT, index); 
                declare( reg, td, elist.head );
                emitMOVE( ROOT, "move `d0, `s0", reg, tl.head );
             }
          break;
          case StackInfo.STACK:
             if(isDoubleWord(elist.head)) {
                declare (SP, HClass.Void);
                Util.assert(tl.head instanceof TwoWordTemp);
                emit(new InstrMEM( instrFactory, ROOT,
                                   "sw `s0h, " 
                                   + stack.argOffset(ROOT, index) 
                                   + "(`s1)",
                                   null,
                                   new Temp[]{ tl.head, SP })); 
                emit(new InstrMEM( instrFactory, ROOT,
                                   "sw `s0l, " 
                                   + stack.argSecondOffset(ROOT, index) 
                                   + "(`s1)",
                                   null,
                                   new Temp[]{ tl.head, SP })); 
             } else {
                declare( SP, HClass.Void );
                emit(new InstrMEM(
                   instrFactory, ROOT,
                   "sw `s0, " + stack.argOffset(ROOT, index)
                   + "(`s1)",
                   null,
                   new Temp[]{ tl.head, SP }));
             }
             break;
          case StackInfo.REGSTACKSPLIT:
             Util.assert(tl.head instanceof TwoWordTemp);
             // gcc and cc (mipspro) on mips don't do this
             Util.assert(false);
             break;
          default:
             Util.assert(false);
          }
       }

       declareCALL();
       if (ROOT.getRetval()!=null) declare( v0, td, ROOT.getRetval() );
       return new CallState(callUses);
    }
    /** Make a handler stub. */
    private void emitHandlerStub(INVOCATION ROOT, Temp retex, Label handler) {
       declare( retex, frame.getLinker().forName("java.lang.Throwable"));
       emitMOVE ( ROOT, "move `d0, `s0", retex, v0 );
       emitJUMP ( ROOT, "b "+handler+" # handler stub", handler);
       // I don't think we need this use/def info because all temps
       // are available at this point since we threw in the current
       // frame 
       //emit( ROOT, "b "+handler+" # handler stub",
       //      call_def_builtin, // clobbers
       //     call_use, 
       //     new Label[] { handler} );
    }
/** Emit a fixup table entry */
    private void emitCallFixup(INVOCATION ROOT, Label retaddr, Label handler) {
      // this '1f' and '1:' business is taking advantage of a GNU
      // Assembly feature to avoid polluting the global name space with
      // local labels
      // these may need to be included in the previous instr to preserve
      // ordering semantics, but for now this way they indent properly
       emitDIRECTIVE( ROOT, !is_elf?".rdata ": (mipspro_assem ? ".data\n.section .flex.fixup,1, 3, 4, 16" : ".data\n.section .flex.fixup"));
      emitDIRECTIVE( ROOT, "\t.word "+retaddr+", "+handler+" # (retaddr, handler)");
      emitDIRECTIVE( ROOT, !is_elf?".text": (mipspro_assem ? ".text\n.section .flex.code,1, 7, 4, 16" : ".text\n.section .flex.code"));
    }
    /** Finish up a CALL or NATIVECALL. */
    private void emitCallEpilogue(INVOCATION ROOT, boolean isNative,
                                  Temp retval, HClass type, 
                                  CallState cs) {
       if (ROOT.getRetval()==null) {
          // this is a void method.  don't bother to emit move.
       } else if (isNative && (!soft_float) &&
                  ROOT.getRetval().type()==Type.FLOAT) {
          // float retval passed in float registers.
          declare(retval, type); declare ( SP, HClass.Void );
          //emitMOVE( ROOT, "move `d0, `s0", retval, v0 );
          // XXX does not use SP, but null trips an assert in Instr.java:checkForNull
          emit( ROOT, "swc1 $f0, 0($sp)", SP, SP);
          emit2( ROOT, "lw   `d0, 0(`s0)", 
                 new Temp[]{retval,SP},new Temp[]{SP});
          //emit2(ROOT, "lw `d0, 4($sp)",new Temp[]{retval,SP},new Temp[]{SP});
       } else if (isNative && (!soft_float) &&
                  ROOT.getRetval().type()==Type.DOUBLE) {
          // double retval passed in float registers.
          declare(retval, type); declare ( SP, HClass.Void );
          Util.assert(retval instanceof TwoWordTemp);
          //emit( ROOT, "move `d0h, `s0", retval, v0 );
          // emit( ROOT, "move `d0l, `s0", retval, v1 );
          // sdc1 is mips2
          // XXX does not use SP, but null trips an assert in Instr.java:checkForNull
          emit( ROOT, "sdc1 $f0, 0($sp)", SP, SP);
          emit(ROOT, "lw `d0l, 4($sp)", retval, SP);
          emit(ROOT, "lw `d0h, 0($sp)", retval, SP);
       } else if (ROOT.getRetval().isDoubleWord()) {
          // not certain an emitMOVE is legal with the l/h modifiers
          declare(retval, type);
          Util.assert(retval instanceof TwoWordTemp);
          emit( ROOT, "move `d0h, `s0", retval, v0 );
          emit( ROOT, "move `d0l, `s0", retval, v1 );
       } else {
          declare(retval, type);
          emitMOVE( ROOT, "move `d0, `s0", retval, v0 );
       }  
    }

    private void LayoutInstr(Instr anchor, Instr[] instrs) {
       if(instrs.length == 0) return;
       instrs[0].layout(anchor.getPrev(), anchor);
       for(int i = 1; i < instrs.length; ++i) {
          instrs[i].layout(instrs[i-1], anchor);
       }
    }

    // Mandated by CodeGen generic class: perform entry/exit
    public Instr procFixup(HMethod hm, Instr instr,
                           int stackspace, Set usedRegisters) {
       InstrFactory inf = instrFactory; // convenient abbreviation.
       Label methodlabel = nameMap.label(hm);

       // Really we should be passed a SortedSet, but since we aren't,
       // we will make our own
       Temp[] usedRegArray =
          (Temp[]) usedRegisters.toArray(new Temp[usedRegisters.size()]);
       ArrayList usedRegArrList = new ArrayList(usedRegisters.size());
       for(int i = 0; i < usedRegisters.size(); ++i) {
          usedRegArrList.add(usedRegArray[i]);
       }
       Collections.sort(usedRegArrList, regComp);
       
       stack.regAllocUsedRegs(usedRegArrList);
       // XXX This should be stackstpace - usedRegArrList.size(), but
       // I am getting spill requests for saved registers
       stack.regAllocLocalWords(stackspace);
       int nregs = stack.calleeSaveTotal();

       // find method entry/exit stubs
       Instr last=instr;
       for (Instr il = instr; il!=null; il=il.getNext()) {
          if (il instanceof InstrENTRY) { // entry stub.
             Instr[] entry_instr = {
                new InstrDIRECTIVE(inf, il, ".align 2"),
                new InstrDIRECTIVE(inf, il, ".globl " + methodlabel.name),
                new InstrDIRECTIVE(inf, il, ".type " + methodlabel.name
                   + ",@function"),
                new InstrDIRECTIVE(inf, il, ".ent " +
                                   methodlabel.name+",#function"),
                new InstrLABEL(inf, il, methodlabel.name+":", methodlabel),
                new InstrDIRECTIVE(inf, il, ".frame $sp," +
                                   stack.frameSize() + ", $31"),
                new Instr(inf, il, "subu $sp, "+ stack.frameSize(), 
                          new Temp[] {SP}, new Temp[] {SP}),
                new Instr(inf, il, "sw $31, " + stack.getRAOffset() +"($sp)", 
                          null, new Temp[] { LR, SP }),
                new Instr(inf, il, "sw $30, " + stack.getFPOffset() +"($sp)", 
                          null, new Temp[] { FP, SP }),
                new Instr(inf, il, "addu $30, $sp, "+ stack.frameSize(),
                          new Temp[]{FP}, new Temp[]{SP} ),
             };
             ////// Now create instrs to save non-trivial callee saved regs
             Instr[] callee_save = new Instr [stack.calleeSaveTotal()];
             if(nregs > 0) {
                // make list of instructions saving callee-save registers we
                // gotta save.
                int j = 0;
                for (int i=0; i < stack.calleeSaveTotal(); i++) {
                   callee_save[j++] = 
                      new Instr(inf, il, "sw " 
                                + stack.calleeReg(i) + ", "
                                + stack.calleeSaveOffset(i)
                                + "($sp)        # callee save", 
                                null, new Temp[]{stack.calleeReg(i),SP});
                }
             }
             /// Layout function entry
             LayoutInstr(il, entry_instr);
             if(nregs > 0) {
                Util.assert(nregs == callee_save.length);
                // This puts all of the callee saving before the
                // creation of the frame pointer which is the last
                // instruction in entry_isntr.
                LayoutInstr(entry_instr[entry_instr.length-1], callee_save);
             }
             // fixup root if necessary
             if (il==instr) instr = entry_instr[entry_instr.length - 1];
             // if (stackspace==0) in7.remove(); // optimize
             il.remove(); il = entry_instr[entry_instr.length - 1];

          }
          if (il instanceof InstrEXIT) { // exit stub
             Instr[] exit_instr = {
                new Instr(inf, il, "lw $30, " + stack.getFPOffset() + "($sp)",
                          new Temp[] {FP}, new Temp[] {SP}),
                new Instr(inf, il, "lw $31, " + stack.getRAOffset() + "($sp)",
                          new Temp[] {LR}, new Temp[] {SP}),
                new Instr(inf, il, "addu $sp, " + stack.frameSize(),
                          new Temp[] {SP}, new Temp[] {SP}),
                new Instr(inf, il, "j  $31  # return",
                          null, new Temp[]{LR}),
             };
             Instr[] callee_restore = new Instr [nregs];
             if(stack.calleeSaveTotal() > 0) {
                // restore the regs we saved on entry
                int j = 0;
                for (int i=0; i < stack.calleeSaveTotal(); i++) {
                   callee_restore[j++] = 
                      new Instr(inf, il, "lw " 
                                + stack.calleeReg(i) + ", "
                                + stack.calleeSaveOffset(i) 
                                + "($sp)        # callee restore", 
                                new Temp[]{stack.calleeReg(i)}, 
                                new Temp[]{SP});
                }
             }

             if(nregs > 0) {
                LayoutInstr(il, callee_restore);
             }
             LayoutInstr(il, exit_instr);
             il.remove(); 
             il = exit_instr[exit_instr.length - 1];
          }
          last=il;
       }
       // add a size directive to the end of the function to let gdb
       // know how long it is.
       if (last!=null) { // best be safe.
          // Instructions show up in reverse order
          Instr in1 = new InstrDIRECTIVE(inf, last, "\t.end " +
                                         methodlabel.name);
          // I can't find a way to make the MIPSpro assembler
          // understand this.  It complains about the symbols not
          // having aboslute value
          Instr in2 = new InstrDIRECTIVE(inf, last, "\t.size " +
                                         methodlabel.name + ", . - " +
                                         methodlabel.name);
          in1.layout(last, last.getNext());
          Util.assert(mipspro_assem == false);
          in2.layout(last, in1);
          last=in2;
       }
       // stabs debugging information:
       if (stabsDebugging && !hm.getDeclaringClass().isArray()) {
          int lineno=-1;
          for (Instr il = instr; il!=null; il=il.getNext())
             // XXXX vpekoe-as does not support .stabd
             if (false && il.getLineNumber()!=lineno) {
                lineno = il.getLineNumber();
                Instr in1 = new InstrDIRECTIVE(inf, il, // line number
                                               "\t.stabd 68,0,"+lineno);
                in1.layout(il.getPrev(), il);
                if (il==instr) instr=in1;
             }
          Instr in1 = new InstrDIRECTIVE(inf, instr, // source path
                                         "\t.stabs \""+
                                         hm.getDeclaringClass().getPackage()
                                         .replace('.','/')+"/"+
                                         "\",100,0,0,"+methodlabel.name);
          Instr in2 = new InstrDIRECTIVE(inf, instr, // source file name
                                         "\t.stabs \""+instr.getSourceFile()+
                                         "\",100,0,0,"+methodlabel.name);
          Instr in3 = new InstrDIRECTIVE(inf, instr, // define void type
                                         "\t.stabs \"void:t19=19\",128,0,0,0"
             );
          // F1 instead of F19 looks like the vpekoe-gcc output
          Instr in4 = new InstrDIRECTIVE(inf, instr, // mark as function
                                         "\t.stabs \""+
                                         methodlabel.name.substring(1)+":F1"
                                         +"\",36,0,"+(nregs*4)+","+
                                         methodlabel.name);
          in1.layout(instr.getPrev(), instr);
          in2.layout(in1, instr);
          in3.layout(in2, instr);
          instr = in1;
          in4.layout(last, last.getNext());
          last = in4;
       }
       return instr;
    }

    public StackInfo getStackInfo() {
      return stack;
    }
    // now define our little InstrENTRY and InstrEXIT sub-types.
    private class InstrENTRY extends Instr {
       public InstrENTRY(InstrFactory inf, HCodeElement src) {
          // defines SP, FP providing reaching defs for
          // InstrEXIT uses.
          super(inf, src, "# --method entry point--",
                new Temp[] { SP, FP }, null);
       }
    }
    private class InstrEXIT extends Instr {
       public InstrEXIT(InstrFactory inf, HCodeElement hce) {
          // uses SP, FP, making them live in whole
          // procedure (so register allocator doesn't stomp
          // on them!)
          super(inf, hce, "# --method exit point--", null, 
                new Temp[]{ SP, FP }, false, null);
       }
    }

%%
%start with %{
	// *** METHOD PROLOGUE ***
	this.instrFactory = inf; // XXX this should probably move to superclass
    // Create a stack object and let it know about all calls made by
    // this method.  Shouldn't this be in the constructor?
    this.stack = new StackInfo(regfile);
    collectCallInfo(code);
}%
%end with %{
       // *** METHOD EPILOGUE *** 
   
}%
    /* this comment will be eaten by the .spec processor (unlike comments above) */
	
/* EXPRESSIONS */ 

/***********************************************************/
/* Arithmetic */

BINOP<p,i>(ADD, j, k) = i
%{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "addu `d0, `s0, `s1", i, j, k);
}%
BINOP<p,i>(ADD, j, CONST<p,i>(c)) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "addu `d0, `s0, "+c, i, j);
}%

BINOP<l>(ADD, j, k) = i %extra<i>{ extra }
%{
   Util.assert(i instanceof TwoWordTemp);
   emitLineDebugInfo(ROOT);
   emit( ROOT, "addu `d0l, `s0l, `s1l", i, j, k);
   emit( ROOT, "sltu `d0, `s0l, `s1l", extra, i, k);
   emit( ROOT, "addu `d0h, `s0, `s1h", i, extra, j);
   emit( ROOT, "addu `d0h, `s0h, `s1h", i, i, k);
}%

BINOP<f>(ADD, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i,j,k,"__addsf3");
}%

BINOP<d>(ADD, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, j, k, "__adddf3");
}%

    /*-------- SUBTRACT (which, in tree form, is (x + (-y) ) -------- */
BINOP<p,i>(ADD, j, UNOP<p,i>(NEG, k)) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "subu `d0, `s0, `s1", i, j, k);
}%
/* I am pretty sure I don't need a constant subtraction pattern, as
   addu a negative constant is just fine */ 
BINOP<p,i>(ADD, j, UNOP<p,i>(NEG, CONST<i>(c))) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "subu `d0, `s0, "+c, i, j);
}%

    // okay, back to regular subtractions.
BINOP<l>(ADD, j, UNOP<l>(NEG, k)) = i %extra<i>{ extra }
%{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "sltu `d0, `s0l, `s1l", extra, j, k);
   emit( ROOT, "subu `d0l, `s0l, `s1l", i, j, k);
   emit( ROOT, "subu `d0h, `s0h, `s1h", i, j, k);
   emit( ROOT, "subu `d0h, `s0h, `s1", i, i, extra);
}%

BINOP<f>(ADD, j, UNOP<f>(NEG, k)) = i %{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, j, k, "__subsf3");
}%
BINOP<d>(ADD, j, UNOP<d>(NEG, k)) = i %{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, j, k, "__subdf3");
}%

BINOP<p,i>(MUL, j, k) = i %{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "mul `d0, `s0, `s1", i, j, k );
}%

BINOP<p,i>(MUL, j, CONST<p,i>(c)) = i %{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "mul `d0, `s0, "+c, i, j );
}%

BINOP<l>(MUL, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoLLCall(ROOT, i, j, k, "__ll_mul");
}%

BINOP<f>(MUL, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, j, k, "__mulsf3");
}%

BINOP<d>(MUL, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, j, k, "__muldf3");
}%

BINOP<p,i>(DIV, j, k) = i %{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "div `d0, `s0, `s1", i, j, k );
}%

BINOP<p,i>(DIV, j, CONST<p,i>(c)) = i %{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "div `d0, `s0, "+c, i, j );
}%

BINOP<l>(DIV, j, k) = i %{
   emitLineDebugInfo(ROOT);
   // Call gcc internal function directly
   DoLLCall(ROOT, i, j, k, "__divdi3");
   // This is the c-wrapper version
   // DoLLCall(ROOT, i, j, k, "__ll_div");
}%

BINOP<f>(DIV, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, j, k, "__divsf3");
}%

BINOP<d>(DIV, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, j, k, "__divdf3");
}%

BINOP<i>(REM, j, k) = i %{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "rem `d0, `s0, `s1", i, j, k );
}%

BINOP<i>(REM, j, CONST<p,i>(c)) = i %{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "rem `d0, `s0, "+c, i, j );
}%

BINOP<l>(REM, j, k) = i %{
   emitLineDebugInfo(ROOT);
   // Optimize by calling gcc builtin
   DoLLCall(ROOT, i, j, k, "__moddi3");
   // This the the call to a C function
   // DoLLCall(ROOT, i, j, k, "__ll_rem");
}%

UNOP<i,p>(NEG, arg) = i
%{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "negu `d0, `s0", i, arg );
}% 
UNOP<f>(NEG, arg) = i
%{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, arg, "__f_neg");
}% 
UNOP<d>(NEG, arg) = i
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, arg, "__d_neg");
}% 
UNOP<l>(NEG, arg) = i %extra<i>{ extra }
%{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "subu `d0l, $0, `s0l", i, arg);
   emit( ROOT, "subu `d0h, $0, `s0h", i, arg);
   emit( ROOT, "sltu `d0, $0, `s0l", extra, arg );
   emit( ROOT, "subu `d0h, `s0h, `s1", i, i, extra);
}% 

/***********************************************************/
/* Logical */

BINOP<p,i>(AND, j, k) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "and `d0, `s0, `s1", i, j, k );
}%
BINOP<p,i>(AND, j, CONST<p,i>(c)) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "and `d0, `s0, "+c, i, j );
}%
BINOP<l>(AND, j, k) = i %{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "and `d0l, `s0l, `s1l", i, j, k );
    emit( ROOT, "and `d0h, `s0h, `s1h", i, j, k );
}%

BINOP<p,i>(OR, j, k) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "or `d0, `s0, `s1", i, j, k );
}%
BINOP<p,i>(OR, j, CONST<p,i>(c)) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "or `d0, `s0, "+c, i, j );
}%

BINOP<l>(OR, j, k) = i %{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "or `d0l, `s0l, `s1l", i, j, k );
    emit( ROOT, "or `d0h, `s0h, `s1h", i, j, k );
}%

BINOP<p,i>(XOR, j, k) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "xor `d0, `s0, `s1", i, j, k );
}%
BINOP<p,i>(XOR, j, CONST<p,i>(c)) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "xor `d0, `s0, "+c, i, j );
}%
BINOP<l>(XOR, j, k) = i %{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "xor `d0l, `s0l, `s1l", i, j, k );
    emit( ROOT, "xor `d0h, `s0h, `s1h", i, j, k );
}%

UNOP<i,p>(NOT, arg) = i
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "not `d0, `s0", i, arg );
}% 

UNOP<l>(NOT, arg) = i 
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "not `d0l, `s0l", i, arg );
    emit( ROOT, "not `d0h, `s0h", i, arg );
}% 

/***********************************************************/
/* Comparisons */

BINOP(cmpop, j, k) = i
%pred %( (ROOT.operandType()==Type.POINTER || ROOT.operandType()==Type.INT)
	 && isCmpOp(cmpop) )%
%{
   emitLineDebugInfo(ROOT);
   // Lets hear it for the MIPS assembler
   emit( ROOT, cmpOp2AsStr(cmpop) + " `d0, `s0, `s1 # cmpop", i, j, k );
}%

BINOP(CMPEQ, j, k) = i %extra<i>{ extra }
%pred %( ROOT.operandType()==Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "xor `d0, `s0h, `s1h", extra, j, k );
    emit( ROOT, "xor `d0, `s0l, `s1l", i, j, k );
    emit( ROOT, "or  `d0, `s0,  `s1", i, i, extra );
    emit( ROOT, "sltiu `d0, `s0, 1", i, i );
}%
BINOP(CMPNE, j, k) = i %extra<i>{ extra } /* void*/
%pred %( ROOT.operandType()==Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "xor `d0, `s0h, `s1h", extra, j, k );
    emit( ROOT, "xor `d0, `s0l, `s1l", i, j, k );
    emit( ROOT, "or  `d0, `s0,  `s1", i, i, extra );
    emit( ROOT, "sltu `d0, $0, `s0", i, i );
}%
BINOP(CMPLT, j, k) = i
%pred %( ROOT.operandType()==Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
   Label done = new Label();
   Label reg = new Label(); // Reg Alloc sanity pass sucks

   emit( ROOT, "slt `d0, `s0h, `s1h", i, j, k);
   emitCondBr( ROOT, "bne `s0h, `s1h, `L0", j, k, done);
   emitLABEL( ROOT, reg + ": ", reg );
   emit( ROOT, "sltu `d0, `s0l, `s1l", i, j, k);
   emitLABEL( ROOT, done + ": ", done );
}%
BINOP(CMPLE, j, k) = i
%pred %( ROOT.operandType()==Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
   Label done = new Label();
   Label reg = new Label(); // Reg Alloc sanity pass sucks

   emit( ROOT, "slt `d0, `s0h, `s1h", i, j, k);
   emitCondBr( ROOT, "bne `s0h, `s1h, `L0", j, k, done);
   emitLABEL( ROOT, reg + ": ", reg );
   emit( ROOT, "sleu `d0, `s0l, `s1l", i, j, k);
   emitLABEL( ROOT, done + ": ", done );
}%
BINOP(CMPGT, j, k) = i
%pred %( ROOT.operandType()==Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
   Label done = new Label();
   Label reg = new Label(); // Reg Alloc sanity pass sucks

   emit( ROOT, "sgt `d0, `s0h, `s1h", i, j, k);
   emitCondBr( ROOT, "bne `s0h, `s1h, `L0", j, k, done);
   emitLABEL( ROOT, reg + ": ", reg );
   emit( ROOT, "sgtu `d0, `s0l, `s1l", i, j, k);
   emitLABEL( ROOT, done + ": ", done );
}%
BINOP(CMPGE, j, k) = i
%pred %( ROOT.operandType()==Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
   Label done = new Label();
   Label reg = new Label(); // Reg Alloc sanity pass sucks

   emit( ROOT, "sgt `d0, `s0h, `s1h", i, j, k);
   emitCondBr( ROOT, "bne `s0h, `s1h, `L0", j, k, done);
   emitLABEL( ROOT, reg + ": ", reg );
   emit( ROOT, "sgeu `d0, `s0l, `s1l", i, j, k);
   emitLABEL( ROOT, done + ": ", done );
}%
  
BINOP(cmpop, j, k) = i
%pred %( ROOT.operandType()==Type.FLOAT && isCmpOp(cmpop) )%
%{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, j, k, "__f_" + cmpOp2AsStr(cmpop));
}%

BINOP(cmpop, j, k) = i
%pred %( ROOT.operandType()==Type.DOUBLE && isCmpOp(cmpop) )%
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, j, k, "__d_" + cmpOp2AsStr(cmpop));
}%

/***********************************************************/
/* Shift */

BINOP<p,i>(shiftop, j, k) = i 
%pred %( isShiftOp(shiftop) )%
%{
   emitLineDebugInfo(ROOT);
    // java lang spec says shift should occur according to
    // 'least significant five bits' of k; MIPS does this correctly
   emit( ROOT, shiftOp2Str(shiftop)+" `d0, `s0, `s1", i, j, k);
}%

BINOP<p,i>(shiftop, j, CONST(c)) = i 
%pred %( isShiftOp(shiftop) && is5BitShift(c) )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, shiftOp2Str(shiftop) + " `d0, `s0, "+c, i, j);
}%

BINOP<l>(SHL, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoLLShiftCall(ROOT, i, j, k, "__ll_lshift");
}%

BINOP<l>(SHR, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoLLShiftCall(ROOT, i, j, k, "__ll_rshift");
}%

BINOP<l>(USHR, j, k) = i %{
   emitLineDebugInfo(ROOT);
   DoLLShiftCall(ROOT, i, j, k, "__ull_rshift");
}%

/***********************************************************/
/* Constants */

CONST<l,d>(c) = i 
%{
   long val = (ROOT.type()==Type.LONG) ? ROOT.value.longValue()
   : Double.doubleToLongBits(ROOT.value.doubleValue());

   emitLineDebugInfo(ROOT);
   emit( ROOT, "li `d0h, " + (int)(val>>>32) + " # hi long const", i);
   emit( ROOT, "li `d0l, " + (int)val + " # lo long const", i);
}% 

CONST<i,f>(c) = i 
%{
    int val = (ROOT.type()==Type.INT) ? ROOT.value.intValue()
	: Float.floatToIntBits(ROOT.value.floatValue());

   emitLineDebugInfo(ROOT);
    String comment = "";
    if(ROOT.type() == Type.FLOAT) {
       comment = "  # float " + ROOT.value.floatValue();
    }
    emit(ROOT, "li `d0, " + val + comment, i);
}%


CONST<p>(c) = i %{
    // the only CONST of type Pointer we should see is NULL
    Util.assert(c==null);
   emitLineDebugInfo(ROOT);
    emit( ROOT, "li `d0, 0   # null pointer", i);
}%


/***********************************************************/
/* Memory Access */

MEM<s:8,u:8,s:16,u:16,i,p,f>(e) = i %{ 
   String suffix = GetLdSuffix(ROOT);
   emitLineDebugInfo(ROOT);
   emit(new InstrMEM(instrFactory, ROOT,
                     "l"+suffix+" `d0, 0(`s0)",
                     new Temp[]{ i }, new Temp[]{ e }));   
}%

MEM<s:8,u:8,s:16,u:16,p,i,f>(BINOP<p>(ADD, j, CONST<i,p>(c))) = i
%{
   String suffix = GetLdSuffix(ROOT);
   emitLineDebugInfo(ROOT);
   emit(new InstrMEM(instrFactory, ROOT,
                     "l"+suffix+" `d0, " + c + "(`s0)",
                     new Temp[]{ i }, new Temp[]{ j }));
}%
MEM<l,d>(e) = i %{
   emitLineDebugInfo(ROOT);
    emit(new InstrMEM(instrFactory, ROOT,
                      "lw `d0l, 4(`s0)",
                      new Temp[]{ i }, new Temp[]{ e }));
    emit(new InstrMEM(instrFactory, ROOT,
                      "lw `d0h, 0(`s0)",
                      new Temp[]{ i }, new Temp[]{ e }));
}%

MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(dst), src)
%{
   String suffix = GetStSuffix(ROOT);
   emitLineDebugInfo(ROOT);
   emit(new InstrMEM(instrFactory, ROOT, "s" + suffix +" `s0, 0(`s1)",
                     null, new Temp[] {src, dst}));
}%

MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(BINOP<p>(ADD, j, CONST<i,p>(c))), src)
%{ 
   String suffix = GetStSuffix(ROOT);
   emitLineDebugInfo(ROOT);
   emit(new InstrMEM(instrFactory, ROOT,
		      "s"+suffix+" `s0, "+c+"(`s1)",
		      null, new Temp[]{ src, j }));   
}%

MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(BINOP<p>(ADD, j, CONST<i,p>(c))), NAME(src))
   %extra<p>{ extra } /*void*/
%{ 
   String suffix = GetStSuffix(ROOT);
   declare( extra, HClass.Void );
   emitLineDebugInfo(ROOT);
   emit(new Instr( instrFactory, ROOT,
                   "la `d0, " + src,
                   new Temp[]{ extra }, null ));
   emit(new InstrMEM(instrFactory, ROOT, "s" + suffix +" `s0, "
                     + c + "(`s1)        # tmp + c <= NAME(src) ",
                     null, new Temp[]{ extra, j }));
}%

MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(dst), NAME(src)) %extra<p>{ extra }/*void*/
%{ 
   String suffix = GetStSuffix(ROOT);
   declare( extra, HClass.Void );
   emitLineDebugInfo(ROOT);
   emit(new Instr( instrFactory, ROOT,
                   "la `d0, " + src,
                   new Temp[]{ extra }, null ));
   emit(new InstrMEM(instrFactory, ROOT, "s" + suffix 
                     +" `s0, (`s1)        # dst <= NAME(src)",
                     null, new Temp[] {extra, dst}));
}%
MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(NAME(dst)), src) %extra<p>{ extra }/*void*/
%{ 
   String suffix = GetStSuffix(ROOT);
   declare( extra, HClass.Void );
   emitLineDebugInfo(ROOT);
   emit(new Instr( instrFactory, ROOT,
                   "la `d0, " + dst,
                   new Temp[]{ extra }, null ));
   emit(new InstrMEM(instrFactory, ROOT, "s" + suffix 
                     +" `s0, (`s1)        # NAME(dst) <= src ",
                     null, new Temp[] {src, extra}));
}%

MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(NAME(dst)), NAME(src))
%extra<p>{ extra0, extra1 }/*void*/
%{ 
   String suffix = GetStSuffix(ROOT);
   emitLineDebugInfo(ROOT);
   declare( extra0, HClass.Void );
   declare( extra1, HClass.Void );
   emit(ROOT, "la `d0, " + src, extra0);
   emit(ROOT, "la `d0, " + src, extra1);
   emit(new InstrMEM(instrFactory, ROOT, "s" + suffix +" `s0, (`s1)",
                     null, new Temp[] {extra0, extra1}));
}%

MOVE(MEM<s:8,u:8,s:16,u:16,p,i,f>(NAME(dst)), CONST<i,p>(c)) 
   %extra<i>{ extra0, extra1 }/*void*/
%{ 
   emitLineDebugInfo(ROOT);
   String suffix = GetStSuffix(ROOT);
   if(c==null) {
      emit(ROOT, "li `d0, 0", extra0);
   } else {
      emit(ROOT, "li `d0, " + c, extra0);
   }
   emit(ROOT, "la `d0, " + dst, extra1);
   emit(new InstrMEM(instrFactory, ROOT, "s" + suffix +" `s0, (`s1)",
                     null, new Temp[] { extra0, extra1 }));
}%


// longs as always are big endian
MOVE(MEM<l,d>(dst), src) %{
   emitLineDebugInfo(ROOT);
   emit(new InstrMEM(instrFactory, ROOT, "sw `s0l, 4(`s1)",
                     null, new Temp[]{ src, dst }));   
   emit(new InstrMEM(instrFactory, ROOT, "sw `s0h, 0(`s1)",
                     null, new Temp[]{ src, dst }));   
}%

MOVE<p,i,f>(dst, src) %{
   emitLineDebugInfo(ROOT);
    declare( dst, code.getTreeDerivation(), ROOT.getSrc());
    emitMOVE( ROOT, "move `d0, `s0", dst, src );
}%

MOVE<d,l>(TEMP(dst), src) %{
   emitLineDebugInfo(ROOT);
    Util.assert( dst instanceof TwoWordTemp, "why is dst: "+dst + 
		 " a normal Temp? " + harpoon.IR.Tree.Print.print(ROOT));

    Util.assert(src instanceof TwoWordTemp, "why is src: "+src + 
		 " a normal Temp? " + harpoon.IR.Tree.Print.print(ROOT));

    declare( dst, code.getTreeDerivation(),  ROOT.getSrc() );
    // not certain an emitMOVE is legal with the l/h modifiers
    emit( ROOT, "move `d0l, `s0l", dst, src );
    emit( ROOT, "move `d0h, `s0h", dst, src );
}%



// Put a label in a register
NAME(id) = i %{
   emitLineDebugInfo(ROOT);
   emit(new Instr( instrFactory, ROOT,
                   "la `d0, " + id, 
                   new Temp[]{ i }, null ));
}%

MEM<f,i,p>(NAME(id)) = i %{
   emitLineDebugInfo(ROOT);
   emit(new Instr( instrFactory, ROOT,
                   "lw `d0, " + id, 
                   new Temp[]{ i }, null ));
}%
MEM<d,l>(NAME(id)) = i %{
   emitLineDebugInfo(ROOT);
    emit(new Instr( instrFactory, ROOT,
		    "lw `d0h, " + id, 
		    new Temp[]{ i }, null ));
    emit(new Instr( instrFactory, ROOT,
		    "lw `d0l, " + id + "+4", 
		    new Temp[]{ i }, null ));
}%


TEMP(t) = i %{ i=t; /* this case is basically handled entirely by the CGG */ }%


UNOP(I2B, arg) = i %pred %( ROOT.operandType() == Type.INT )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "sll `d0, `s0, 24", i, arg);
    emit( ROOT, "sra `d0, `s0, 24", i, i);
}%
UNOP(I2S, arg) = i %pred %( ROOT.operandType() == Type.INT )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "sll `d0, `s0, 16", i, arg);
    emit( ROOT, "sra `d0, `s0, 16", i, i);
}%
UNOP(I2C, arg) = i %pred %( ROOT.operandType() == Type.INT )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "sll `d0, `s0, 16", i, arg);
    emit( ROOT, "srl `d0, `s0, 16", i, i);
}%
UNOP(_2F, arg) = i %pred %( ROOT.operandType() == Type.INT )%
%{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, arg, "__i2f");
}%
UNOP(_2D, arg) = i %pred %( ROOT.operandType() == Type.INT )%
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, arg, "__i2d");
}%
UNOP(_2L, arg) = i %pred %( ROOT.operandType() == Type.INT )%
%{
   emitLineDebugInfo(ROOT);
    emit( ROOT, "move `d0l, `s0", i, arg );
    emit( ROOT, "sra `d0h, `s0, 31", i, arg );
}%
/* Trivial pointer to integer */
UNOP<p>(_2I, arg) = i %pred %( ROOT.operandType() == Type.POINTER )%
%{
   emitLineDebugInfo(ROOT);
    emitMOVE( ROOT, "move `d0, `s0", i, arg );
}%


UNOP(_2I, arg) = i %pred %( ROOT.operandType() == Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "move `d0, `s0l", i, arg );
}%

UNOP(_2D, arg) = i %pred %( ROOT.operandType() == Type.LONG )%
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, arg, "__l2d");
}%


UNOP(_2I, arg) = i %pred %( ROOT.operandType() == Type.FLOAT )%
%{
   emitLineDebugInfo(ROOT);
   DoFCall(ROOT, i, arg, "__f2i");
}%
UNOP(_2D, arg) = i %pred %( ROOT.operandType() == Type.FLOAT )%
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, arg, "__f2d");
}%


UNOP(_2I, arg) = i %pred %( ROOT.operandType() == Type.DOUBLE )%
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, arg, "__d2i");
}%
UNOP(_2L, arg) = i %pred %( ROOT.operandType() == Type.DOUBLE )%
%{
   emitLineDebugInfo(ROOT);
   DoDCall(ROOT, i, arg, "__d2l");
}%


/***********************************************************/
/* Expressions */

EXPR(e) %{
			/* this is a statement that's just an
			   expression; just throw away 
			   calculated value */
}%

/***********************************************************/
/* Conditional jumps and jumps */

CJUMP(test, iftrue, iffalse) %{
   emitLineDebugInfo(ROOT);
   emit( ROOT, "beq `s0, 0, `L0",
         null, new Temp[]{ test },
         new Label[]{ iffalse });
   emitJUMP( ROOT, "b `L0", iftrue );
}%

CJUMP(BINOP(cmpop, j, k), iftrue, iffalse)
%pred %( isCmpOp(cmpop) &&
	 ( ((BINOP) ROOT.getTest()).operandType()==Type.POINTER ||
	   ((BINOP) ROOT.getTest()).operandType()==Type.INT ) )%
%{
   emitLineDebugInfo(ROOT);
   emit( ROOT, cmpOp2BrStr(cmpop) + " `s0, `s1, `L0",
         null, new Temp[] { j , k }, new Label[] { iftrue });
   emitJUMP( ROOT, "b `L0", iffalse );
}%

CJUMP(BINOP(cmpop, j, CONST<i,p>(c)), iftrue, iffalse)
   %pred %( isCmpOp(cmpop) )%
%{
   emitLineDebugInfo(ROOT);
   Util.assert(((BINOP) ROOT.getTest()).operandType()!=Type.POINTER ||
               c == null, "Can not compare a pointer to anything but null\n");
   if(((BINOP) ROOT.getTest()).operandType() == Type.POINTER &&
      c == null) { 
      emit( ROOT, cmpOp2BrStr(cmpop) + " `s0, $0, `L0 # null",
            null, new Temp[] { j }, new Label[] { iftrue });
   } else {
      emit( ROOT, cmpOp2BrStr(cmpop) + " `s0, " + c + ", `L0 ",
            null, new Temp[] { j }, new Label[] { iftrue });
   }
   emitJUMP( ROOT, "b `L0", iffalse );
}%

JUMP(NAME(l)) %{ // direct jump
   emitLineDebugInfo(ROOT);
    //FSK   emitNoFall (ROOT, "j " + l + "", null, null, new Label[] { l });
   emitJUMP(ROOT, "j " + l + "", l );
}%

JUMP(e) %{
   emitLineDebugInfo(ROOT);
    List labelList = LabelList.toList( ROOT.targets );
    emit(new Instr( instrFactory, ROOT, 
                    "j `s0",
                    new Temp[]{ },
                    new Temp[]{ e },
                    false, labelList ) {
          public boolean hasModifiableTargets(){ 
             return false; 
          }
       });
}%


LABEL(id) %{
    if (ROOT.exported) {
      emitLABEL( ROOT, "\t.globl "+ROOT.label+"\n"+
		    ROOT.label + ":", ROOT.label);
    } else {
      emitLABEL( ROOT, ROOT.label + ":", ROOT.label);
    }
}%

/***********************************************************/
/* Calls */

METHOD(params) %{
   emitLineDebugInfo(ROOT);
    // mark entry point.
    declare(SP, HClass.Void);
    declare(FP, HClass.Void);
    emit(new InstrENTRY( instrFactory, ROOT ));
    StackInfo param_stack = new StackInfo(regfile);
    param_stack.callInfo(ROOT);
    // move arguments to temporaries.
    // skip param[0], which is the explicit 'exceptional return address'
    for (int i=1; i<params.length; i++) {
       declare(params[i], code.getTreeDerivation(), ROOT.getParams(i));
       switch(param_stack.argWhere(ROOT, i-1)) {
       case StackInfo.REGISTER:
          if (ROOT.getParams(i).isDoubleWord()) {
             emit( ROOT, "move `d0h, `s0", params[i],
                       param_stack.argReg(ROOT, i-1));
             emit( ROOT, "move `d0l, `s0", params[i],
                   param_stack.argSecondReg(ROOT, i-1));
          } else {
             emitMOVE( ROOT, "move `d0, `s0", params[i],
                       param_stack.argReg(ROOT, i-1));
          }
          break;
       case StackInfo.STACK:
          if(ROOT.getParams(i).isDoubleWord()) {
             declare (SP, HClass.Void);
             emit(new InstrMEM( instrFactory, ROOT,
                                "lw `d0h, " 
                                + param_stack.argOffset(ROOT, i-1)
                                + "(`s0)        # arg "
                                + (i-1) + "h",
                                new Temp[]{ params[i] },
                                new Temp[]{ FP })); 
             emit(new InstrMEM( instrFactory, ROOT,
                                "lw `d0l, " 
                                + param_stack.argSecondOffset(ROOT, i-1)
                                + "(`s0)        # arg "
                                + (i-1) + "l",
                                new Temp[]{ params[i] },
                                new Temp[]{ FP })); 
          } else {
             declare( SP, HClass.Void );
             emit(new InstrMEM(
                instrFactory, ROOT,
                "lw `d0, " + param_stack.argOffset(ROOT, i-1)
                + "(`s0)         # arg " + (i-1),
                new Temp[]{ params[i] },
                new Temp[]{ FP }));
          }
          break;
       case StackInfo.REGSTACKSPLIT:
          Util.assert(false);
       }
    }
    param_stack = null; // free
}%

THROW(val, handler) %{
   emitLineDebugInfo(ROOT);
   // ignore handler, as our runtime does clever things instead.
   declare( v0, code.getTreeDerivation(), ROOT.getRetex() );
   // What about functions that return long long?
   emitMOVE( ROOT, "move `d0, `s0", v0, val );
   declareCALLDefBuiltin();
   declare(a0, HClass.Void);
   declare(a1, HClass.Void);
   declare(a2, HClass.Void);
   declare(a3, HClass.Void);
   declare(t4, HClass.Void);
   // $31 contains the instruction after the return point
   // of the function that might have excepted
   emit( ROOT, "jal "+nameMap.c_function_name("_lookup_handler")+
         " # hi mom ",
         new Temp[] {a0, a1, a2, a3, t4, LR}, //clobbers
		 new Temp[] {FP}, true, null);  // v0 and FP are preserved
   // mark exit point.
   emit(new InstrEXIT( instrFactory, ROOT ));
}%

// slow version when we don't know exactly which method we're calling.
CALL(retval, retex, func, arglist, handler)
   %pred %( !ROOT.isTailCall )%
%{
   CallState cs = emitCallPrologue(ROOT, arglist, code.getTreeDerivation());
   Label rlabel = new Label(), elabel = new Label();
   emitLineDebugInfo(ROOT);

   declareCALLDefFull();
   emit2(ROOT, "la `d0, " + rlabel + " # funky call",
         new Temp[]{ LR }, null );
   // call uses 'func' as `s0
   cs.callUses.add(0, func);
   emitCallNoFall( ROOT, "j `s0 ",
                   call_def_full,
                   (Temp[]) cs.callUses.toArray(new Temp[cs.callUses.size()]),
                   new Label[] { rlabel, elabel } );
   // make handler stub.
   emitLABEL( ROOT, elabel+":", elabel);
   emitHandlerStub(ROOT, retex, handler);
   // normal return
   emitLABEL( ROOT, rlabel+":", rlabel);
   emitCallEpilogue(ROOT, false, retval,
                    ((ROOT.getRetval()==null)?null:
                     code.getTreeDerivation().typeMap(ROOT.getRetval())), cs);
   // emit fixup table.
   emitCallFixup(ROOT, rlabel, elabel);
}%
// optimized version when we know exactly which method we're calling.
CALL(retval, retex, NAME(funcLabel), arglist, handler)
   %pred %( !ROOT.isTailCall )%
%{
   CallState cs = emitCallPrologue(ROOT, arglist, code.getTreeDerivation());
   Label rlabel = new Label(), elabel = new Label();

   emitLineDebugInfo(ROOT);
   declareCALLDefFull();
   emit2(ROOT, "la `d0, " + rlabel + " # funky call",
         new Temp[]{ LR }, null );
   emitCallNoFall( ROOT, "j "+funcLabel,
                   call_def_full,
                   (Temp[]) cs.callUses.toArray(new Temp[cs.callUses.size()]),
                   new Label[] { rlabel, elabel } );
   // make handler stub.
   emitLABEL( ROOT, elabel+":", elabel);
   emitHandlerStub(ROOT, retex, handler);
   // normal return
   emitLABEL( ROOT, rlabel+":", rlabel);
   emitCallEpilogue(ROOT, false, retval,
                    ((ROOT.getRetval()==null)?null:
                     code.getTreeDerivation().typeMap(ROOT.getRetval())), cs);
   // emit fixup table.
   emitCallFixup(ROOT, rlabel, elabel);
}%
// slow version when we don't know exactly which method we're calling.
NATIVECALL(retval, func, arglist) %{
   CallState cs = emitCallPrologue(ROOT, arglist,
                                   code.getTreeDerivation());
   emitLineDebugInfo(ROOT);
    // call uses 'func' as `s0
   cs.callUses.add(0, func);
   declareCALLDefFull();
   emitNativeCall( ROOT, "jal `s0",
                   call_def_full,
                   (Temp[]) cs.callUses.toArray(new Temp[cs.callUses.size()]),
                   true, null);
   // clean up.
   emitCallEpilogue(ROOT, true, retval, 
                    ((ROOT.getRetval()==null)?null:
                     code.getTreeDerivation().typeMap(ROOT.getRetval())), cs);
}%
// optimized version when we know exactly which method we're calling.
// eW this is not optimized as the more general version does not need
// to be pessimized as it does on StrongARM.
NATIVECALL(retval, NAME(funcLabel), arglist) 
%{
   CallState cs = emitCallPrologue(ROOT, arglist,
                                   code.getTreeDerivation());
   emitLineDebugInfo(ROOT);
   declareCALLDefFull();
   emitNativeCall( ROOT, "jal " + funcLabel,
                   call_def_full,
                   (Temp[]) cs.callUses.toArray(new Temp[cs.callUses.size()]),
                   true, null);
   // clean up.
   emitCallEpilogue(ROOT, true, retval, 
                    ((ROOT.getRetval()==null)?null:
                     code.getTreeDerivation().typeMap(ROOT.getRetval())), cs);
}%

RETURN<i,f,p>(val) %{
   emitLineDebugInfo(ROOT);
   declare( v0, code.getTreeDerivation(),  ROOT.getRetval() );
   emitMOVE( ROOT, "move `d0, `s0", v0, val );
   // mark exit point.
   emit(new InstrEXIT( instrFactory, ROOT ));
}%

RETURN<l,d>(val) %{
   emitLineDebugInfo(ROOT);
   // InstrMOVEs do not handle longs
   declare( v0, HClass.Void );
   declare( v1, HClass.Void );
   emit( ROOT, "move `d0, `s0h", v0, val);
   emit( ROOT, "move `d0, `s0l", v1, val);
   // mark exit point.
   emit(new InstrEXIT( instrFactory, ROOT ));
}%

/***********************************************************/
/* Data */

DATUM(CONST<i,f>(exp)) %{
   int i = (ROOT.getData().type()==Type.INT) ? exp.intValue()
      : Float.floatToIntBits(exp.floatValue());
   String lo = "0x"+Integer.toHexString(i);
   emitDIRECTIVE( ROOT, "\t.word "+lo+" # "+exp);
}%

DATUM(CONST<l,d>(exp)) %{
   long l = (ROOT.getData().type()==Type.LONG) ? exp.longValue()
      : Double.doubleToLongBits(exp.doubleValue());
   String lo = "0x"+Integer.toHexString((int)l);
   String hi = "0x"+Integer.toHexString((int)(l>>32));
   // Store doubles and longs big endian
   emitDIRECTIVE( ROOT, "\t.word "+hi+" # hi("+exp+")");
   emitDIRECTIVE( ROOT, "\t.word "+lo+" # lo("+exp+")");
}%

DATUM(CONST<p>(exp)) %{
   emitDIRECTIVE( ROOT, "\t.word 0 # null pointer constant");
}%

DATUM(CONST<s:8,u:8>(exp)) %{
   String chardesc = (exp.intValue()>=32 && exp.intValue()<127 
                      && exp.intValue()!=96 /* backquotes cause problems */
                      && exp.intValue()!=34 /* so do double quotes */) ?
      ("\t# char "+((char)exp.intValue())) : "";
   emitDIRECTIVE( ROOT, "\t.byte "+exp+chardesc);
}%

DATUM(CONST<s:16,u:16>(exp)) %{
   String chardesc = (exp.intValue()>=32 && exp.intValue()<127
                      && exp.intValue()!=96 /* backquotes cause problems */
                      && exp.intValue()!=34 /* so do double quotes */) ?
      ("\t# char "+((char)exp.intValue())) : "";
   emitDIRECTIVE( ROOT, "\t.short "+exp+chardesc);
}%

DATUM(NAME(l)) %{
   emitDIRECTIVE( ROOT, "\t.word "+l);
}%

ALIGN(n) %{
   emitDIRECTIVE( ROOT, "\t.balign "+n);
}%

SEGMENT(CLASS) %{
   emitDIRECTIVE( ROOT, !is_elf?".data": (mipspro_assem ? ".data\n.section .flex.class,1, 3, 4, 16" : ".data\n.section .flex.class"));

}%

SEGMENT(CODE) %{
   // gas 2.7 does not support naming the code section...not
   // sure what to do about this yet...
   // emitDIRECTIVE( ROOT, !is_elf?".code 32":".section code");
   emitDIRECTIVE( ROOT, !is_elf?".text ": (mipspro_assem ? ".text\n.section .flex.code,1, 7, 4, 16" : ".text\n.section .flex.code"));
}%

SEGMENT(GC) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".data\n.section .flex.gc,1, 3, 4, 16" : ".data\n.section .flex.gc"));
}%

SEGMENT(INIT_DATA) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.init_data,1, 3, 4, 16" : ".section .flex.init_data"));
}%

SEGMENT(STATIC_OBJECTS) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.static_objects,1, 3, 4, 16" : ".section .flex.static_objects"));
}%

SEGMENT(STATIC_PRIMITIVES) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.static_primitives,1, 3, 4, 16" : ".section .flex.static_primitives"));
}%

SEGMENT(STRING_CONSTANTS) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.string_constants,1, 3, 4, 16" : ".section .flex.string_constants" ));
}%

SEGMENT(STRING_DATA) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.string_data,1, 3, 4, 16" : ".section .flex.string_data" ));
}%

SEGMENT(REFLECTION_OBJECTS) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.reflection_objects,1, 3, 4, 16" : ".section .flex.reflection_objects"));
}%

SEGMENT(REFLECTION_DATA) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.reflection_data,1, 3, 4, 16" : ".section .flex.reflection_data"));
}%

SEGMENT(GC_INDEX) %{
   emitDIRECTIVE( ROOT, !is_elf?".data ": (mipspro_assem ? ".section .flex.gc_index,1, 3, 4, 16" : ".section .flex.gc_index"));
}%

SEGMENT(TEXT) %{
   emitDIRECTIVE( ROOT, !is_elf?".text": (mipspro_assem ? ".section .text,1, 7, 4, 16" : ".section .text"));
}%

SEGMENT(ZERO_DATA) %{
   // gas 2.7 does not allow BSS subsections...use .comm and .lcomm
   // for the variables to be initialized to zero
   // emitDIRECTIVE( ROOT, ".bss   \t#.section zero");
   emitDIRECTIVE(ROOT, !is_elf?".bss": (mipspro_assem ? ".section .flex.zero,1, 3, 4, 16" : ".section .flex.zero"));
}%
// Local Variables:
// mode:java
// End:
