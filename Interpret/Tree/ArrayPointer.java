package harpoon.Interpret.Tree;

import harpoon.ClassFile.HClass;
import harpoon.Util.Tuple;

/**
 * The <code>ArrayPointer</code> class represents a pointer to an 
 * <code>ArrayRef</code> plus some offset.  This pointer can be dereferenced 
 * with <code>getValue()</code>, and the value at this location can be 
 * modified with <code>updateValue()</code>.
 * 
 * @author  Duncan Bryce <duncan@lcs.mit.edu>
 * @version $Id: ArrayPointer.java,v 1.1.2.1 1999-03-27 22:05:07 duncan Exp $
 */
class ArrayPointer extends Pointer {
    private boolean isDerived;
    
    // Constructor used to add two ArrayPointers
    private ArrayPointer(ArrayPointer base, long offset) {
        this((ArrayRef)base.getBase(), base.getOffset() + offset, true);
    }
    
    // Copy constructor
    private ArrayPointer(ArrayRef base, long offset, boolean isDerived) {
	super(new Object[] { base, new Long(offset) });
	this.isDerived = isDerived;
    }

    /** Class constructor.  
     */
    ArrayPointer(ArrayRef base, long offset) {
	this(base, offset, offset==0);
    }

    /** Adds the specified parameter to this <code>ArrayPointer</code>'s
     *  offset */
    public Pointer add(long offset) {
	return new ArrayPointer(this, offset);
    }

    /** Returns true if <code>obj</code> is an <code>ArrayPointer</code> 
     *  which points to the same location as this <code>ArrayPointer</code>.
     */
    public boolean equals(Object obj) {
	if (!(obj instanceof ArrayPointer)) return false;
	else {
	    ArrayPointer ptr = (ArrayPointer)obj;
	    return (getBase()==ptr.getBase()) &&
		(getOffset()==ptr.getOffset());
	}
    }

    /** Returns an ArrayRef representing the base of this ArrayPointer */
    public Object getBase()   { return proj(0); }

    /** Returns the offset of this ArrayPointer */
    public long   getOffset() { return ((Long)proj(1)).longValue(); }
    
    /** Returns the value obtained by dereferencing this 
     *  <code>ArrayPointer</code>.  This value is in non-nataive format.
     */
    Object getValue() {
        return Method.toNonNativeFormat(ArrayRef.get(this));
    }
    
    /** Sets the value at the memory location specified by this 
     *  <code>ArrayPointer</code> to the specified parameter. 
     *  This value should be in non-native format.
     */
    void updateValue(Object value) {
	HClass type = ((ArrayRef)getBase()).type.getComponentType();
	ArrayRef.update(this, Method.toNativeFormat(value, type));
    }

    /** Always returns false. */
    public boolean isConst()         { return false; }

    /** Returns true if this ArrayPointer is derived */
    public boolean isDerived()       { return isDerived; }

    /** Returns a human-readable representation of this 
     *  <code>ArrayPointer</code>
     */
    public String toString() { 
	StringBuffer sb = new StringBuffer("ArrayPtr: < ");
	sb.append(getBase().toString());
	sb.append(" , ");
	sb.append(getOffset());
	sb.append(" > ---> ");
	sb.append(getValue());
	return sb.toString();
    }

}



