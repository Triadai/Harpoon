// IIR_ConstantDeclaration.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_ConstantDeclaration</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_ConstantDeclaration.java,v 1.3 1998-10-11 01:24:55 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_ConstantDeclaration extends IIR_ObjectDeclaration
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_CONSTANT_DECLARATION).
     * @return <code>IR_Kind.IR_CONSTANT_DECLARATION</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_CONSTANT_DECLARATION; }
    
    //METHODS:  
    public void set_value(IIR value)
    { _value = value; }
 
    public IIR get_value()
    { return _value; }
 
    //MEMBERS:  

// PROTECTED:
    IIR _value;
} // END class

