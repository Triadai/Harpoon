// IIR_NatureDeclaration.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_NatureDeclaration</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_NatureDeclaration.java,v 1.5 1998-10-11 01:24:59 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_NatureDeclaration extends IIR_Declaration
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_NATURE_DECLARATION).
     * @return <code>IR_Kind.IR_NATURE_DECLARATION</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_NATURE_DECLARATION; }
    //CONSTRUCTOR:
    public IIR_NatureDeclaration() { }
    //METHODS:  
    public void set_nature(IIR_NatureDefinition nature)
    { _nature = nature; }
 
    public IIR_NatureDefinition get_nature()
    { return _nature; }
 
    //MEMBERS:  
    public IIR_AttributeSpecificationList attributes;

// PROTECTED:
    IIR_NatureDefinition _nature;
} // END class

