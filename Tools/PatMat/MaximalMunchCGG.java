// MaximalMunchCGG.java, created Thu Jun 24 18:07:16 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Tools.PatMat;

import harpoon.Util.Util;
import harpoon.IR.Tree.Type;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

/**
 * <code>MaximalMunchCGG</code> finds an optimal tiling of
 * instructions on a Tree-IR.
 *
 * This Code Generator Generator produces Code Generators that use the
 * Maximal Munch algorithm to find an optimal tiling for an input
 * tree.  See Appel "Modern Compiler Implementation in Java", Section
 * 9.1 for a description of Maximal Munch.
 * 
 * TODO: replace all Class references with full names (so there's no
 * chance of a name conflict in whatever import statements that the
 * user includes in their .spec file (just make static Strings in this
 * file to reference the full name
 *
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id: MaximalMunchCGG.java,v 1.1.2.17 1999-07-29 01:49:15 pnkfelix Exp $ */
public class MaximalMunchCGG extends CodeGeneratorGenerator {


    private static final String TREE_BINOP = "harpoon.IR.Tree.BINOP";
    private static final String TREE_CALL = "harpoon.IR.Tree.CALL";
    private static final String TREE_CJUMP = "harpoon.IR.Tree.CJUMP";
    private static final String TREE_CONST = "harpoon.IR.Tree.CONST";
    private static final String TREE_EXP = "harpoon.IR.Tree.EXP";
    private static final String TREE_JUMP = "harpoon.IR.Tree.JUMP";
    private static final String TREE_LABEL = "harpoon.IR.Tree.LABEL";
    private static final String TREE_MEM = "harpoon.IR.Tree.MEM";
    private static final String TREE_MOVE = "harpoon.IR.Tree.MOVE";
    private static final String TREE_NAME = "harpoon.IR.Tree.NAME";
    private static final String TREE_NATIVECALL = "harpoon.IR.Tree.NATIVECALL";
    private static final String TREE_OPER = "harpoon.IR.Tree.OPER";
    private static final String TREE_RETURN = "harpoon.IR.Tree.RETURN";
    private static final String TREE_SEQ = "harpoon.IR.Tree.SEQ";
    private static final String TREE_TEMP = "harpoon.IR.Tree.TEMP";
    private static final String TREE_THROW = "harpoon.IR.Tree.THROW";
    private static final String TREE_UNOP = "harpoon.IR.Tree.UNOP";

    private static final String TREE_Tree = "harpoon.IR.Tree.Tree";
    private static final String TREE_ExpList = "harpoon.IR.Tree.ExpList";
    private static final String TREE_Exp = "harpoon.IR.Tree.Exp";
    private static final String TREE_Stm = "harpoon.IR.Tree.Stm";
    private static final String TREE_TreeVisitor = "harpoon.IR.Tree.TreeVisitor";

    private static final String TEMP_Label = "harpoon.Temp.Label";
    private static final String TEMP_Temp = "harpoon.Temp.Temp";

    /** Creates a <code>MaximalMunchCGG</code>. 
	<BR> <B>requires:</B> <OL>
	     <LI> <code>s</code> follows the standard template for
	          defining a machine specification.  
	     <LI> <code>className</code> is a legal Java identifier
	          string for a class.
	     <LI> For each node-type in the <code>Tree</code> IR,
	          there exists a single-node tile pattern (TODO: I
		  took this 'requirement' straight from Appel, but it
		  can't REALLY be that strict.  Find a tighter
		  requirement that we can actually satisfy)
	     <LI> if <code>s</code> contains Java statements that rely
	          on knowledge about the class to be produced (such as
		  a Constructor implementation) then the class named
		  must match the <code>className</code> parameter.
	     </OL>
	<BR> <B>effects:</B> Creates a new
             <code>MaximalMunchCGG</code> and associates the
	     machine specification <code>s</code> with the newly
	     constructed <code>MaximalMunchCGG</code>.
	@see <A HREF="doc-files/instr-selection-tool.html">Standard Specification Template</A>
    */
    public MaximalMunchCGG(Spec s, String className) {
        super(s, className);
    }

    /** Sets up a series of checks to ensure all of the values in the
	visited statement are of the appropriate type.
    */
    static class TypeStmRecurse extends Spec.StmVisitor {
	/** constantly updated boolean expression to match a tree's
	    types. */
	StringBuffer exp;
	
	/** constantly updated set of statements to initialize the
            identifiers that the action statements will reference. */
	StringBuffer initStms;

	/** constantly updated (and reset) with current statement
	    prefix throughout recursive calls. */
	String stmPrefix;

	/** number of nodes "munched" by this so far. */
	int degree;
	
	/** Indentation. */
	String indentPrefix;
	
	private void append(StringBuffer buf, String s) {
	    buf.append(indentPrefix + s + "\n");
	}

	TypeStmRecurse(String stmPrefix, String indentPrefix) {
	    // hack to make everything else additive
	    exp = new StringBuffer("true\n"); 
	    initStms = new StringBuffer();
	    degree = 0;
	    this.stmPrefix = stmPrefix;
	    this.indentPrefix = indentPrefix;
	}
	
	public void visit(Spec.Stm s) {
	    Util.assert(false, "StmRecurse should never visit Stm: " + s + 
			" Class:" + s.getClass());
	}
	
	public void visit(Spec.StmCall s) {
	    degree++;
	    
	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof " + TREE_CALL +" ");
	    
	    // look at func
	    TypeExpRecurse r = new 
		TypeExpRecurse("(("+TREE_CALL+") " +stmPrefix +").func", 
			       indentPrefix + "\t");
	    s.func.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() +indentPrefix+")");
	    initStms.append(r.initStms.toString());

	    // look at retex
	    // NOTE: THIS WILL BREAK WHEN WE UPDATE EXCEPTION HANDLING 
	    r = new TypeExpRecurse("(("+TREE_CALL+")"+stmPrefix + ").retex", 
				   indentPrefix + "\t");
	    s.retex.accept(r);
	    degree += r.degree;
	    append(exp, indentPrefix + "&& (" + r.exp.toString() + indentPrefix+")");
	    initStms.append(r.initStms.toString());

	    // look at retval
	    r = new TypeExpRecurse("(("+TREE_CALL+")"+stmPrefix + ").retval",
				   indentPrefix + "\t");
	    s.retval.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() + ")");
	    initStms.append(r.initStms.toString());

	    // initialize arglist
	    append(initStms, TREE_ExpList + " " + s.arglist + 
			   " = ((" + TREE_CALL + ")"+stmPrefix + ").args;");
	    
	}

	public void visit(Spec.StmCjump s) {
	    degree++;
	    
	    append(exp, "// check statement type");
	    append(exp, "&& (" + stmPrefix + " instanceof "+TREE_CJUMP+")");
	    
	    // look at test
	    TypeExpRecurse r = new
		TypeExpRecurse("(("+TREE_CJUMP+") " + stmPrefix + ").test",
			       indentPrefix + "\t");
	    s.test.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() + indentPrefix+")");
	    initStms.append(r.initStms.toString());

	    // code to initialize the target label identifiers so
	    // that they can be referred to.
	    append(initStms, TEMP_Label +" "+ s.f_label + 
			    " = (("+TREE_CJUMP+")"+stmPrefix+
			    ").iffalse;"); 
	    append(initStms, TEMP_Label +" "+ s.t_label +  
			    " = (("+TREE_CJUMP+")"+stmPrefix+
			    ").iftrue;");

	}

	public void visit(Spec.StmExp s) {
	    degree++;
	    
	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_EXP+"");
	    
	    // look at exp
	    TypeExpRecurse r = new 
		TypeExpRecurse("(("+TREE_EXP+")"+ stmPrefix + ").exp",
			       indentPrefix + "\t");
	    s.exp.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() + indentPrefix+")");
	    initStms.append(r.initStms.toString());
	}

	public void visit(Spec.StmJump s) {
	    degree++;

	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_JUMP+"");
	    
	    // look at exp
	    TypeExpRecurse r = new 
		TypeExpRecurse("(("+TREE_JUMP+")" + stmPrefix + ").exp",
			       indentPrefix + "\t");
	    s.exp.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() + indentPrefix+")");
	    initStms.append(r.initStms.toString());

	}

	public void visit(Spec.StmLabel s) {
	    degree++;
	    
	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_LABEL+" ");

	    // code to initialize the target label identifiers so that
	    // they can be referred to.
	    append(initStms, "String " + s.name + 
			    " = (("+TREE_LABEL+")"+stmPrefix+
			    ").label.toString();\n");
	}

	public void visit(Spec.StmMove s) {
	    degree++;

	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_MOVE+" ");
	    
	    // look at src
	    TypeExpRecurse r = new 
		TypeExpRecurse("(("+TREE_MOVE+") " + stmPrefix + ").src",
			       indentPrefix + "\t");
	    s.src.accept(r);
	    degree += r.degree;
	    append(exp, indentPrefix + "&& (" + r.exp.toString() + indentPrefix  +")");
	    initStms.append(r.initStms.toString());

	    // look at dst
	    r = new TypeExpRecurse("(("+TREE_MOVE+") " + stmPrefix + ").dst",
				   indentPrefix + "\t");
	    s.dst.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() + indentPrefix+")");
	    initStms.append(r.initStms.toString());
	}

	public void visit(Spec.StmNativeCall s) {
	    degree++;

	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_NATIVECALL+"");

	    // look at func
	    TypeExpRecurse r = new 
		TypeExpRecurse("(("+TREE_NATIVECALL+") " +stmPrefix +").func", 
			       indentPrefix + "\t");
	    s.func.accept(r);
	    degree += r.degree;
	    append(exp, indentPrefix + "&& (" + r.exp.toString() +indentPrefix+")");
	    initStms.append(r.initStms.toString());

	    // look at retex
	    // NOTE: THIS WILL BREAK WHEN WE UPDATE EXCEPTION HANDLING 
	    r = new TypeExpRecurse("(("+TREE_NATIVECALL+")"+stmPrefix + ").retex", 
				   indentPrefix + "\t");
	    s.retex.accept(r);
	    degree += r.degree;
	    append(exp, indentPrefix + "&& (" + r.exp.toString() +indentPrefix+ ")");
	    initStms.append(r.initStms.toString());

	    // look at retval
	    r = new TypeExpRecurse("(("+TREE_NATIVECALL+")"+stmPrefix + ").retval",
				   indentPrefix + "\t");
	    s.retval.accept(r);
	    degree += r.degree;
	    append(exp, "&& (" + r.exp.toString() +indentPrefix+ ")");
	    initStms.append(r.initStms.toString());
	    
	    // initialize arg list
	    append(initStms, TREE_ExpList +" "+ s.arglist + 
		   " = (("+TREE_NATIVECALL+")"+stmPrefix+").args;");
	}

	public void visit(Spec.StmReturn s) {
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_RETURN+"");

	    append(exp, "// check operand types");
	    boolean allowInt, allowLong, allowFloat, allowDouble, allowPointer;
	    allowDouble = s.types.contains(Type.DOUBLE);
	    allowFloat = s.types.contains(Type.FLOAT);
	    allowInt = s.types.contains(Type.INT);
	    allowLong = s.types.contains(Type.LONG);
	    allowPointer = s.types.contains(Type.POINTER);

	    String checkPrefix = "\t(("+TREE_RETURN+")" + stmPrefix + ").retval.type() ==";
	    append(exp, "&& ( ");
	    if(allowDouble) append(exp, checkPrefix + " Type.DOUBLE ||");
	    if(allowFloat) append(exp, checkPrefix + " Type.FLOAT ||");
	    if(allowInt) append(exp, checkPrefix + " Type.INT ||");
	    if(allowLong) append(exp, checkPrefix + " Type.LONG ||");
	    if(allowPointer) append(exp, checkPrefix + " Type.POINTER ||");
	    append(exp, "\tfalse )"); 
	    append(exp, "// end check operand types");
	    
	    // look at exp
	    TypeExpRecurse r = new
		TypeExpRecurse("(("+TREE_RETURN+") " + stmPrefix + ").retval",
			       indentPrefix + "\t");
	    s.retval.accept(r);
	    degree += r.degree;
	    append(exp, indentPrefix + "&& (" + r.exp.toString() +indentPrefix+")");
	    initStms.append(r.initStms.toString());
	}

	public void visit(Spec.StmSeq s) {
	    degree++;

	    append(exp, "// check statement type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_SEQ+"");
	    
	    // save state before outputting children-checking code
	    String oldPrefix = stmPrefix;
	    String oldIndent = indentPrefix;

	    indentPrefix = oldIndent + "\t";

	    append(exp, "// check left child"); 
	    stmPrefix = "(("+TREE_SEQ+") " + oldPrefix + ").left";
	    s.s1.accept(this);
	    append(exp, "// check right child");
	    stmPrefix = "(("+TREE_SEQ+") " + oldPrefix + ").right";
	    s.s2.accept(this);

	    // restore original state
	    indentPrefix = oldIndent;
	    stmPrefix = oldPrefix;
	}

	public void visit(Spec.StmThrow s) {
	    degree++;
	    
	    append(exp, "// check expression type");
	    append(exp, "&& " + stmPrefix + " instanceof "+TREE_THROW+"");

	    // look at exp
	    TypeExpRecurse r = new 
		TypeExpRecurse("(("+TREE_THROW+") " + stmPrefix + ").retex",
			       indentPrefix + "\t");
	    s.exp.accept(r); 
	    degree += r.degree;
	    append(exp, indentPrefix + "&& (" + r.exp.toString() +indentPrefix+ ")");
	    initStms.append(r.initStms.toString());
	}
    }



    /** Sets up a series of checks to ensure all of the values
	in the visited expression are of the appropriate
	type. 
    */
    static class TypeExpRecurse extends Spec.ExpVisitor {
	/** constantly updated boolean expression to match a tree's
	    types. */ 
	StringBuffer exp;
	
	/** constantly updated set of statements to initialize the
            identifiers that the action statements will reference. */
	StringBuffer initStms;

	/** constantly updated (and reset) with current expression
	    prefix throughout recursive calls. */ 
	String expPrefix;
		
	/** number of nodes "munched" by this so far. */
	int degree;
	
	/** Indentation. */
	String indentPrefix;

	/** Helper function to prettify resulting code. */
	private void append(StringBuffer buf, String s) {
	    buf.append(indentPrefix + s +"\n");
	}

	TypeExpRecurse(String expPrefix, String indentPrefix) {
	    // hack to make everything else additive
	    exp = new StringBuffer("true\n"); 	    
	    initStms = new StringBuffer();

	    degree = 0;
	    this.expPrefix = expPrefix;
	    this.indentPrefix = indentPrefix;
	}
	
	public void visit(Spec.Exp e) {
	    Util.assert(false, "ExpRecurse should never visit Exp: "+e + 
			" Class: " + e.getClass());
	}
	public void visit(final Spec.ExpBinop e) { 
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + expPrefix + " instanceof " + TREE_BINOP);

	    // My god, I can't tell if this visitor is good or bad for
	    // understanding the code!
	    e.opcode.accept(new Spec.LeafVisitor() {
		public void visit(Spec.Leaf l) {
		    Util.assert(false, "Should never visit generic Leaf in ExpBinop");
		}
		public void visit(Spec.LeafOp l) {
		    append(exp, "// check opcode");
		    append(exp, "&& ((" + TREE_BINOP + ")" + expPrefix + ").op == " + l.op);
		}
		public void visit(Spec.LeafId l) {
		    append(initStms, "int " + l.id + " = ((" + TREE_BINOP + ") " + 
			   expPrefix + ").op;");
		}
	    });

	    append(exp, "// check operand types");
	    boolean allowInt, allowLong, allowFloat, allowDouble, allowPointer;
	    allowDouble = e.types.contains(Type.DOUBLE);
	    allowFloat = e.types.contains(Type.FLOAT);
	    allowInt = e.types.contains(Type.INT);
	    allowLong = e.types.contains(Type.LONG);
	    allowPointer = e.types.contains(Type.POINTER);

	    String checkPrefix = "\t" + expPrefix + ".type() ==";
	    append(exp, "&& ( ");
	    if(allowDouble) append(exp, checkPrefix + " Type.DOUBLE ||");
	    if(allowFloat) append(exp, checkPrefix + " Type.FLOAT ||");
	    if(allowInt) append(exp, checkPrefix + " Type.INT ||");
	    if(allowLong) append(exp, checkPrefix + " Type.LONG ||");
	    if(allowPointer) append(exp, checkPrefix + " Type.POINTER ||");
	    append(exp, "\tfalse )");
	    append(exp, "// end check operand types");

	    // save state before outputing children-checking code
	    String oldPrefix = expPrefix;
	    String oldIndent = indentPrefix;

	    indentPrefix = oldIndent + "\t";
	    
	    append(exp, "// check left child");
	    expPrefix = "((" + TREE_BINOP + ")" + oldPrefix + ").left";
	    e.left.accept(this);
	    append(exp, "// check right child");  
	    expPrefix = "((" + TREE_BINOP + ")" + oldPrefix + ").right";
	    e.right.accept(this);
	    
	    // restore original state
	    indentPrefix = oldIndent;
	    expPrefix = oldPrefix;
		       
	}
	
	public void visit(Spec.ExpConst e) {
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + expPrefix + " instanceof " + TREE_CONST + " ");

	    append(exp, "// check operand types");
	    boolean allowInt, allowLong, allowFloat, allowDouble, allowPointer;
	    allowDouble = e.types.contains(Type.DOUBLE);
	    allowFloat = e.types.contains(Type.FLOAT);
	    allowInt = e.types.contains(Type.INT);
	    allowLong = e.types.contains(Type.LONG);
	    allowPointer = e.types.contains(Type.POINTER);

	    String checkPrefix = "\t" + expPrefix + ".type() ==";
	    append(exp, "&& ( ");
	    if(allowDouble) append(exp, checkPrefix + " Type.DOUBLE ||");
	    if(allowFloat) append(exp, checkPrefix + " Type.FLOAT ||");
	    if(allowInt) append(exp, checkPrefix + " Type.INT ||");
	    if(allowLong) append(exp, checkPrefix + " Type.LONG ||");
	    if(allowPointer) append(exp, checkPrefix + " Type.POINTER ||");
	    append(exp, "\tfalse )");
	    append(exp, "// end check operand types");

	    e.value.accept(new Spec.LeafVisitor() {
		public void visit(Spec.Leaf l) {
		    Util.assert(false, "Should never visit generic Leaf in ExpConst");
		}
		public void visit(Spec.LeafId l) {
		    append(initStms, "Number " + l.id + " = ((" + TREE_CONST + ") " + 
			   expPrefix + ").value;");
		}
		public void visit(Spec.LeafNumber l) {
		    append(exp, "// check that constant value matches");
		    append(exp, "&& ( " + expPrefix + ".isFloatingPoint()?");
		    append(exp, expPrefix + ".doubleValue() == " + l.number.doubleValue() + ":");
		    append(exp, expPrefix + ".longValue() == " + l.number.longValue() + ")");
		}
	    });
	}
	public void visit(Spec.ExpId e) {
	    // don't increase the munch-factor (ie
	    // 'degree') ( Spec.ExpId is strictly a way for a
	    // specification to refer back to items in the parsed
	    // tree)
	    append(exp, "// no check needed for ExpId children");
	    append(initStms, TEMP_Temp +" "+ e.id +" = munchExp(" + expPrefix + "); ");
	    return;
	}
	public void visit(Spec.ExpMem e) { 
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + expPrefix + " instanceof " + TREE_MEM + " ");

	    append(exp, "// check operand types");
	    boolean allowInt, allowLong, allowFloat, allowDouble, allowPointer;
	    allowDouble = e.types.contains(Type.DOUBLE);
	    allowFloat = e.types.contains(Type.FLOAT);
	    allowInt = e.types.contains(Type.INT);
	    allowLong = e.types.contains(Type.LONG);
	    allowPointer = e.types.contains(Type.POINTER);

	    String checkPrefix = "\t" + expPrefix + ".type() ==";
	    append(exp, "&& ( ");
	    if(allowDouble) append(exp, checkPrefix + " Type.DOUBLE ||");
	    if(allowFloat) append(exp, checkPrefix + " Type.FLOAT ||");
	    if(allowInt) append(exp, checkPrefix + " Type.INT ||");
	    if(allowLong) append(exp, checkPrefix + " Type.LONG ||");
	    if(allowPointer) append(exp, checkPrefix + " Type.POINTER ||");
	    append(exp, "\tfalse )");
	    append(exp, "// end check operand types");

	    // save state before outputing child-checking code
	    String oldPrefix = expPrefix;
	    String oldIndent = indentPrefix;

	    indentPrefix = oldIndent + "\t";

	    append(exp, "// check child");
	    expPrefix = "((" + TREE_MEM + ")" + oldPrefix + ").exp";
	    e.addr.accept(this);

	    // restore original state
	    indentPrefix = oldIndent;
	    expPrefix = oldPrefix;

	}
	public void visit(Spec.ExpName e) { 
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + expPrefix + " instanceof " + TREE_NAME + " ");
	    
	    append(initStms, TEMP_Label +" "+ e.name + " = ((" +TREE_NAME + ")"+
		   expPrefix + ").label;");
	    
	}
	public void visit(Spec.ExpTemp e) { 
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + expPrefix + " instanceof " + TREE_TEMP +" ");
	    
	    append(exp, "// check operand type");
	    boolean allowInt, allowLong, allowFloat, allowDouble, allowPointer;
	    allowDouble = e.types.contains(Type.DOUBLE);
	    allowFloat = e.types.contains(Type.FLOAT);
	    allowInt = e.types.contains(Type.INT);
	    allowLong = e.types.contains(Type.LONG);
	    allowPointer = e.types.contains(Type.POINTER);

	    String checkPrefix = "\t" + expPrefix + ".type() ==";
	    append(exp, "&& ( ");
	    if(allowDouble) append(exp, checkPrefix + " Type.DOUBLE ||");
	    if(allowFloat) append(exp, checkPrefix + " Type.FLOAT ||");
	    if(allowInt) append(exp, checkPrefix + " Type.INT ||");
	    if(allowLong) append(exp, checkPrefix + " Type.LONG ||");
	    if(allowPointer) append(exp, checkPrefix + " Type.POINTER ||");
	    append(exp, "\tfalse )");
	    append(exp, "// end check operand types");

	    append(initStms, TEMP_Temp +" "+ e.name + " = ((" +TREE_TEMP + ")"+
		   expPrefix + ").temp;");
	}
	public void visit(Spec.ExpUnop e) { 
	    degree++;

	    append(exp, "// check expression type");
	    append(exp, "&& " + expPrefix + " instanceof " + TREE_UNOP + " ");

	    e.opcode.accept(new Spec.LeafVisitor() {
		public void visit(Spec.Leaf l) {
		    Util.assert(false, "Should never visit generic Leaf in ExpUnop");
		}
		public void visit(Spec.LeafOp l) {
		    append(exp, "// check opcode");
		    append(exp, "&& ((" + TREE_UNOP + ")" + expPrefix + ").op == " + l.op);
		}
		public void visit(Spec.LeafId l) {
		    append(initStms, "int " + l.id + " = ((" + TREE_UNOP + ") " + 
			   expPrefix + ").op;");
		}
	    });

	    append(exp, "// check operand types");
	    boolean allowInt, allowLong, allowFloat, allowDouble, allowPointer;
	    allowDouble = e.types.contains(Type.DOUBLE);
	    allowFloat = e.types.contains(Type.FLOAT);
	    allowInt = e.types.contains(Type.INT);
	    allowLong = e.types.contains(Type.LONG);
	    allowPointer = e.types.contains(Type.POINTER);

	    String checkPrefix = "\t"+expPrefix + ".type() ==";
	    append(exp, "&& ( ");
	    if(allowDouble) append(exp, checkPrefix + " Type.DOUBLE ||");
	    if(allowFloat) append(exp, checkPrefix + " Type.FLOAT ||");
	    if(allowInt) append(exp, checkPrefix + " Type.INT ||");
	    if(allowLong) append(exp, checkPrefix + " Type.LONG ||");
	    if(allowPointer) append(exp, checkPrefix + " Type.POINTER ||");
	    append(exp, "\tfalse )"); 
	    append(exp, "// end check operand types");

	    
	    // save state before outputting child-checking code
	    String oldPrefix = expPrefix;
	    String oldIndent = indentPrefix;

	    indentPrefix = oldIndent + "\t";

	    append(exp, "// check child");
	    expPrefix = "((" + TREE_UNOP + ")" + oldPrefix + ").operand";
	    e.exp.accept(this);

	    // restore state
	    expPrefix = oldPrefix;
	    indentPrefix = oldIndent;
	}
    }	    

    /**javac 1.1 sez this class (which is only used inside the 
     * outputSelectionMethod below) has to be out here, instead of
     * inside the method.  Actually, it doesn't specifically, but I can't
     * figure out how to properly qualify the references to PredicateBuilder
     * below to make them work right with javac 1.1, which gets confused 
     * over whose inner class PredicateBuilder is, actually.  
     * jikes likes fine, either way. go figure. [CSA] */
    static class PredicateBuilder extends Spec.DetailVisitor {
	final StringBuffer predicate = new StringBuffer("true");
	public void visit(Spec.Detail d) { /* do nothing */ }
	public void visit(Spec.DetailPredicate d) { 
	    predicate.append("&& ("+d.predicate_string+")");
	}
    }
    /** Writes the Instruction Selection Method to <code>out</code>.
	<BR> <B>modifies:</B> <code>out</code>
	<BR> <B>effects:</B>
	     Generates Java source for a MaximalMunch instruction
	     selection method, not including method signature or
	     surrounding braces.  Outputs generated source to
	     <code>out</code>. 
	@param out Target output device for the Java source code.
    */
    public void outputSelectionMethod(final PrintWriter out) { 
	// traverse 'this.spec' to acquire spec information
	final List expMatchActionPairs = new LinkedList(); // list of RuleTuple
	final List stmMatchActionPairs = new LinkedList();

	final String expArg = "expArg";
	final String stmArg = "stmArg";
	final String indent = "\t\t\t";

	Spec.RuleVisitor srv = new Spec.RuleVisitor() {
	    public void visit(Spec.Rule r) {
		Util.assert(false, "SpecRuleVisitor should never visit Rule");
	    }
	    public void visit(Spec.RuleStm r) { 
		TypeStmRecurse recurse = 
		    new TypeStmRecurse(stmArg, indent + "\t");
		r.stm.accept(recurse);

		String typeCheck = recurse.exp.toString();
		int munchFactor = recurse.degree;

		PredicateBuilder makePred = new PredicateBuilder();
	        if (r.details!=null) r.details.accept(makePred);

		String matchStm = (indent + "if (" + typeCheck + indent + "){\n"+
				   recurse.initStms.toString() +
				   indent + "\t_matched_ = " + makePred.predicate.toString()+";\n" +
				   indent + TREE_Tree + " ROOT = " + stmArg + ";\n");

		//String matchStm = indent + "_matched_ = (" + typeCheck + indent + ");";
		stmMatchActionPairs.add( new RuleTuple
					 ( matchStm, 
					   r.action_str + 
					   indent + "}", recurse.degree ) );
	    }
	    public void visit(Spec.RuleExp r) { 
		TypeExpRecurse recurse = 
		    new TypeExpRecurse(expArg, indent + "\t");
		r.exp.accept(recurse);

		String typeCheck = recurse.exp.toString();
		int munchFactor = recurse.degree;
		
		PredicateBuilder makePred = new PredicateBuilder();
		if (r.details!=null) r.details.accept(makePred);
		
		// TODO: add PREDICATE CHECK to end of matchStm
		String matchStm = (indent + "if ("+typeCheck+indent+"){\n" +
				   recurse.initStms.toString() +
				   indent + "\t_matched_ = "+makePred.predicate.toString()+";\n" +
				   indent + TREE_Tree + " ROOT = " + expArg + ";\n");
		//String matchStm = indent + "_matched_ =  (" + typeCheck + indent + ");";
		expMatchActionPairs.add( new RuleTuple
					 ( matchStm, r.action_str + 
					   indent + "return " + r.result_id + ";\n" +
					   indent + "}", recurse.degree ) );
		
	    }

	};
	
	Spec.RuleList list = spec.rules;
	while(list != null) {
	    list.head.accept(srv);
	    list = list.tail;
	}
	
	Comparator compare = new RuleTupleComparator();
	Collections.sort(expMatchActionPairs, compare);
	Collections.sort(stmMatchActionPairs, compare);
	
	// Implement a recursive function by making a helper class to
	// visit the nodes
	out.println("\tfinal class CggVisitor extends "+TREE_TreeVisitor+" {");

	// for each rule for an exp we need to implement a
	// clause in munchExp()
	out.println("\t\t " + TEMP_Temp + " munchExp(" + TREE_Exp +" "+ expArg + ") {");
	
	out.println("\t\t\tboolean _matched_ = false;");

	Iterator expPairsIter = expMatchActionPairs.iterator();
	while(expPairsIter.hasNext()) {
	    RuleTuple triplet = (RuleTuple) expPairsIter.next();
	    out.println(triplet.matchStms);
	    out.println("\t\t\t\tif (_matched_) { // action code!");
	    out.println(triplet.actionStms);
	    
	    out.println("\t\t\t}");
	}
	
	out.println("\t\tharpoon.Util.Util.assert(false, \"Uh oh... "+
		    "maximal munch didn't match anything...SPEC file "+
		    "is not complete enough for this program\");");
	out.println("\t\treturn null; // doesn't matter, we're dead if we didn't match...");
	out.println("\t\t }"); // end munchExp
	

	// for each rule for a statement we need to implement a
	// clause in munchStm()
	out.println("\t\t void munchStm("+TREE_Stm + " " + stmArg + ") {");
	
	out.println("\t\t\tboolean _matched_ = false;");
	
	Iterator stmPairsIter = stmMatchActionPairs.iterator();
	while(stmPairsIter.hasNext()) {
	    RuleTuple triplet = (RuleTuple) stmPairsIter.next();
	    out.println(triplet.matchStms);
	    out.println("\t\t\t\tif (_matched_) { // action code!");
	    out.println(triplet.actionStms);
	    
	    out.println("\t\t\t}");
	}

	out.println("\t\t} // end munchStm");
	
	out.println("\t\tpublic void visit("+TREE_Tree+" treee){");
	out.println("\t\t\tharpoon.Util.Util.assert(false, "+
		    "\"Should never visit generic " + TREE_Tree + 
		    "in CggVisitor\");");
	out.println("\t\t} // end visit("+TREE_Tree+")");

	out.println("\t\tpublic void visit("+TREE_Stm+" treee){");
	out.println("\t\t\tmunchStm(treee);");
	out.println("\t\t} // end visit("+TREE_Stm+")");
	
	// BAD DOG!  Don't implement visit(TREE_Exp)...we should never
	// be munching those directly; only from calls to visit(TREE_Stm) 
	
	out.println("\t}"); // end CggVisitor
	
	
	
    }
    
    static class RuleTuple {
	final String matchStms, actionStms; final int degree;
	    
	/** Constructs a new <code>RuleTuple</code>.
	    @param matchStms A series of Java statements which will set 
	                    _matched_ to TRUE if expression matches
			    and initialize variables that could be
			    references in 'actionStms'.  Note that if the
			    matchStms start a new scope for the
			    variables to be initialized in (for
			    example, inside the then-clause of an
			    if-statement) then 'actionStms' must end
			    it, so that the RuleTuple is completely
			    selfcontained. 
	    @param actionStms A series of Java statements to execute if 
	                      _matched_ == TRUE after 'matchExp' executes 
	    @param degree Number of nodes that this rule "eats"
	*/
	RuleTuple(String matchStms, String actionStms, int degree) {
	    this.matchStms = matchStms;
	    this.actionStms = actionStms;
	    this.degree = degree;
	}
    }
    
    static class RuleTupleComparator implements Comparator {
	public int compare(Object o1, Object o2) {
	    RuleTuple r1 = (RuleTuple) o1;
	    RuleTuple r2 = (RuleTuple) o2;
	    return r1.degree - r2.degree;
	}
    }

}
