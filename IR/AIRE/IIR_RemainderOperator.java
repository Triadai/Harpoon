// IIR_RemainderOperator.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_RemainderOperator</code>
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_RemainderOperator.java,v 1.3 1998-10-11 01:25:01 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_RemainderOperator extends IIR_DyadicOperator
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_REMAINDER_OPERATOR).
     * @return <code>IR_Kind.IR_REMAINDER_OPERATOR</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_REMAINDER_OPERATOR; }
    //CONSTRUCTOR:
    public IIR_RemainderOperator(){}
    //METHODS:  
    //MEMBERS:  

// PROTECTED:
} // END class

