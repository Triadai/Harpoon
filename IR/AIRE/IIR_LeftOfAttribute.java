// IIR_LeftOfAttribute.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_LeftOfAttribute</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_LeftOfAttribute.java,v 1.3 1998-10-11 01:24:58 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_LeftOfAttribute extends IIR_Attribute
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_LEFT_OF_ATTRIBUTE).
     * @return <code>IR_Kind.IR_LEFT_OF_ATTRIBUTE</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_LEFT_OF_ATTRIBUTE; }
    //CONSTRUCTOR:
    public IIR_LeftOfAttribute() { }

    //METHODS:  
    //MEMBERS:  

// PROTECTED:
} // END class

