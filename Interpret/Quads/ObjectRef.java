// ObjectRef.java, created Mon Dec 28 00:29:30 1998 by cananian
package harpoon.Interpret.Quads;

import harpoon.ClassFile.*;
import harpoon.Util.Util;

/**
 * <code>ObjectRef</code> is an object reference in the interpreter.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ObjectRef.java,v 1.1.2.2 1998-12-30 04:39:40 cananian Exp $
 */
class ObjectRef  {
    /** The type of the object. */
    final HClass type;
    /** Fields in this instance of the object. */
    FieldValueList fields;
    /** A pointer to the static state, so we can finalize. */
    final StaticState ss;
    /** A monitor lock. */
    //boolean lock;
    /** Native method closure. */
    Object closure;

    /** create a new objectref with default field values.
     * @exception InterpretedThrowable
     *            if class initializer throws an exception.  */
    ObjectRef(StaticState ss, HClass type) {
	this.ss = ss; this.type = type; this.fields = null;
	/*this.lock = false;*/ this.closure = null;
	// load class into StaticState, if needed.
	if (!ss.isLoaded(type)) ss.load(type);
	// then initialize our fields, too
	for (HClass sc=type; sc!=null; sc=sc.getSuperclass()) {
	    HField[] fl = sc.getDeclaredFields();
	    for (int i=0; i<fl.length; i++)
		if (!fl[i].isStatic())
		    update(fl[i], defaultValue(fl[i]));
	}
	// yay, done.
    }
    Object get(HField f) {
	return FieldValueList.get(this.fields, f);
    }
    void update(HField f, Object value) {
	this.fields = FieldValueList.update(this.fields, f, value);
    }
    synchronized void lock() { /* FIXME */ }
    synchronized void unlock() { /* FIXME */ }

    Object getClosure() { return closure; }
    void putClosure(Object cl) { closure = cl; }

    protected void finalize() throws Throwable {
	// finalize the referenced object by evaluating its finalize method.
	try {
	    HMethod hm = type.getMethod("finalize", new HClass[0]);
	    Method.invoke(ss, hm, new Object[] { this } );
	} catch (InterpretedThrowable e) {
	    // ignore.
	} catch (NoSuchMethodError e) {
	    // no finalize method.
	}
	// finalize the actual object.
	super.finalize();
    }

    // UTILITY:
    static final Object defaultValue(HField f) {
	if (f.isConstant()) return f.getConstant();
	return defaultValue(f.getType());
    }
    static final Object defaultValue(HClass ty) {
	if (!ty.isPrimitive()) return null;
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
