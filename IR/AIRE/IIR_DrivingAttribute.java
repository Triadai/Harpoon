// IIR_DrivingAttribute.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_DrivingAttribute</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_DrivingAttribute.java,v 1.3 1998-10-11 01:24:56 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_DrivingAttribute extends IIR_Attribute
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_DRIVING_ATTRIBUTE).
     * @return <code>IR_Kind.IR_DRIVING_ATTRIBUTE</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_DRIVING_ATTRIBUTE; }
    //CONSTRUCTOR:
    public IIR_DrivingAttribute() { }
    //METHODS:  
    //MEMBERS:  

// PROTECTED:
} // END class

