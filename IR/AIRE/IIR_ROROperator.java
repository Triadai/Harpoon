// IIR_ROROperator.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_ROROperator</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_ROROperator.java,v 1.3 1998-10-11 01:25:00 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_ROROperator extends IIR_DyadicOperator
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_ROR_OPERATOR).
     * @return <code>IR_Kind.IR_ROR_OPERATOR</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_ROR_OPERATOR; }
    //CONSTRUCTOR:
    public IIR_ROROperator() { }
    //METHODS:  
    //MEMBERS:  

// PROTECTED:
} // END class

