// IIR_PathNameAttribute.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_PathNameAttribute</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_PathNameAttribute.java,v 1.3 1998-10-11 01:25:00 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_PathNameAttribute extends IIR_Attribute
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_PATH_NAME_ATTRIBUTE).
     * @return <code>IR_Kind.IR_PATH_NAME_ATTRIBUTE</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_PATH_NAME_ATTRIBUTE; }
    //CONSTRUCTOR:
    public IIR_PathNameAttribute( ) { }
    //METHODS:  
    //MEMBERS:  

// PROTECTED:
} // END class

