// Ref.java, created Mon Dec 28 00:29:30 1998 by cananian
package harpoon.Interpret.Tree;

import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HField;
import harpoon.ClassFile.HMethod;
import harpoon.Util.Util;

/**
 * <code>Ref</code> is an abstract superclass for object and array references.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Ref.java,v 1.1.2.2 1999-06-28 19:32:25 duncan Exp $
 */
abstract class Ref extends HCLibrary implements Cloneable {
    /** The type of the object. */
    final HClass type;
    /** A pointer to the static state, so we can finalize. */
    final StaticState ss;
    /** A monitor lock. */
    //boolean lock;
    /** Profiling information. */
    /*final*/ long creation_time;

    /** create a new ref.
     * @exception InterpretedThrowable
     *            if class initializer throws an exception.  */
    Ref(StaticState ss, HClass type) {
	this.ss = ss; this.type = type;
	/*this.lock = false;*/
	// load class into StaticState, if needed.
	if (type != null)
	    if (!ss.isLoaded(type)) ss.load(type);

	this.creation_time = ss.getInstructionCount();
	// yay, done.
    }

    synchronized void lock() { /* FIXME */ }
    synchronized void unlock() { /* FIXME */ }

    // arrays have a single final field.
    abstract Object get(HField f);

    public abstract Object clone();
   
    protected void finalize() throws Throwable {
	// profile
	ss.profile(this.type, this.creation_time, ss.getInstructionCount());
	// finalize the actual object.
	super.finalize();
    }

    // UTILITY:
    static final Object defaultValue(HField f) {
	if (f.isConstant()) return f.getConstant();
	return defaultValue(f.getType());
    }
    static final Object defaultValue(HClass ty) {
	if (!ty.isPrimitive()) return Method.TREE_NULL;
	if (ty == HClass.Boolean) return new Boolean(false);
	if (ty == HClass.Byte) return new Byte((byte)0);
	if (ty == HClass.Char) return new Character((char)0);
	if (ty == HClass.Double) return new Double(0);
	if (ty == HClass.Float) return new Float(0);
	if (ty == HClass.Int) return new Integer(0);
	if (ty == HClass.Long) return new Long(0);
	if (ty == HClass.Short) return new Short((short)0);
	throw new Error("Ack!  What kinda default value is this?!");
    }
}


