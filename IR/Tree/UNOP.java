// UNOP.java, created Wed Jan 13 21:14:57 1999 by cananian
package harpoon.IR.Tree;

import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.CloningTempMap;
import harpoon.Util.Util;

/**
 * <code>UNOP</code> objects are expressions which stand for result of
 * applying some unary operator <i>o</i> to a subexpression.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>, based on
 *          <i>Modern Compiler Implementation in Java</i> by Andrew Appel.
 * @version $Id: UNOP.java,v 1.1.2.8 1999-02-24 22:22:23 duncan Exp $
 * @see Uop
 */
public class UNOP extends OPER {
    /** The subexpression to be operated upon. */
    public Exp operand;
    /** Constructor. */
    public UNOP(TreeFactory tf, HCodeElement source,
		int optype, int unop, Exp operand) {
	super(tf, source, optype, unop);
	this.operand = operand;
	Util.assert(Uop.isValid(unop));
    }
    // Unops defined in harpoon.IR.Tree.Uop
    public int type() {
	switch (op) {
	case Uop._2B: case Uop._2C: case Uop._2S: case Uop._2I:
	    return INT;
	case Uop._2L:
	    return LONG;
	case Uop._2F:
	    return FLOAT;
	case Uop._2D:
	    return DOUBLE;
	default:
	    return optype;
	}
    }


    public static Object evalValue(TreeFactory tf, int op, 
				 int optype, Object left) {
	switch(op) {
	case Uop.NEG:
	    switch (optype) {
	    case Type.INT:      return _i(-(_i(left)));
	    case Type.LONG:     return _l(-(_l(left)));
	    case Type.FLOAT:    return _f(-(_f(left)));
	    case Type.DOUBLE:   return _d(-(_d(left)));
	    case Type.POINTER: 
		throw new Error("Operation not supported");
	    }
	case Uop.NOT:
	    switch (optype) {
	    case Type.INT:      return _i(~(_i(left)));
	    case Type.LONG:     return _l(~(_l(left)));
	    case Type.FLOAT:
	    case Type.DOUBLE:
	    case Type.POINTER:
		throw new Error("Operation not supported");
	    }
	case Uop._2B:
	    switch (optype) {
	    case Type.INT:      return _i(_i(left)==0?0:1);
	    case Type.LONG:     return _i(_l(left)==0?0:1);
	    case Type.FLOAT:    return _i(_f(left)==0?0:1);
	    case Type.DOUBLE:   return _i(_d(left)==0?0:1);
	    case Type.POINTER: 
		throw new Error("Operation not supported");
	    }
	case Uop._2C:
	    switch (optype) {
	    case Type.INT:      return _i((char)_i(left));
	    case Type.LONG:     return _i((char)_l(left));
	    case Type.FLOAT:    return _i((char)_f(left));
	    case Type.DOUBLE:   return _i((char)_d(left));
	    case Type.POINTER: 
		throw new Error("Operation not supported");
	    }
	case Uop._2S: 
	    switch (optype) {
	    case Type.INT:      return _i((short)_i(left));
	    case Type.LONG:     return _i((short)_l(left));
	    case Type.FLOAT:    return _i((short)_f(left));
	    case Type.DOUBLE:   return _i((short)_d(left));
	    case Type.POINTER: 
		throw new Error("Operation not supported");
	    }
	case Uop._2I:
	    switch (optype) {
	    case Type.INT:      return left;
	    case Type.LONG:     return _i((int)_l(left));
	    case Type.FLOAT:    return _i((int)_f(left));
	    case Type.DOUBLE:   return _i((int)_d(left));
	    case Type.POINTER: 
		if (Type.isDoubleWord(tf, optype))
		    throw new Error("Operation not supported");
		else return _i((int)_i(left));
	    }
	case Uop._2L:
	    switch (optype) {
	    case Type.INT:      return _l((long)_i(left));
	    case Type.LONG:     return left;
	    case Type.FLOAT:    return _l((long)_f(left));
	    case Type.DOUBLE:   return _l((long)_d(left));
	    case Type.POINTER:  
		if (Type.isDoubleWord(tf, optype)) return left;
		else return _l((long)_i(left));
	    }
	case Uop._2F:
	    switch (optype) {
	    case Type.INT:      return _f((float)_i(left));
	    case Type.LONG:     return _f((float)_l(left));
	    case Type.FLOAT:    return left;
	    case Type.DOUBLE:   return _f((float)_d(left));
	    case Type.POINTER:  
		if (Type.isDoubleWord(tf, optype)) 
		    throw new Error("Operation not supported");
		else return _f((float)_i(left));
	    }
	case Uop._2D:
	    switch (optype) {
	    case Type.INT:      return _d((double)_i(left));
	    case Type.LONG:     return _d((double)_l(left));
	    case Type.FLOAT:    return _d((double)_f(left));
	    case Type.DOUBLE:   return left;
	    case Type.POINTER:  
		if (Type.isDoubleWord(tf, optype)) return left;
		else return _d((double)_i(left));
	    }
	default:
	    throw new Error("Unrecognized Uop");
	}
    }

    // wrapper functions.
    private static Integer _i(int i)     { return new Integer(i); }
    private static Long    _l(long l)    { return new Long(l);    }
    private static Float   _f(float f)   { return new Float(f);   }
    private static Double  _d(double d)  { return new Double(d);  }

    // unwrapper functions.
    private static int    _i(Object o) { return ((Integer)o).intValue(); }
    private static long   _l(Object o) { return ((Long)o)   .longValue(); }
    private static float  _f(Object o) { return ((Float)o)  .floatValue(); }
    private static double _d(Object o) { return ((Double)o) .doubleValue(); }

    public ExpList kids() { return new ExpList(operand, null); }
    public Exp build(ExpList kids) {
	return new UNOP(tf, this, optype, op, kids.head);
    }
    /** Accept a visitor */
    public void visit(TreeVisitor v) { v.visit(this); }

    public Tree rename(TreeFactory tf, CloningTempMap ctm) {
        return new UNOP(tf, this, optype, op, (Exp)operand.rename(tf, ctm));
    }

    public String toString() {
        return "UNOP<" + Type.toString(optype) + ">(" + Uop.toString(op) +
                ", #" + operand.getID() + ")";
    }
}

