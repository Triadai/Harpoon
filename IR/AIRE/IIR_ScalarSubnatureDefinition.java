// IIR_ScalarSubnatureDefinition.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_ScalarSubnatureDefinition</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_ScalarSubnatureDefinition.java,v 1.5 1998-10-11 01:25:01 cananian Exp $
 */

//-----------------------------------------------------------
public abstract class IIR_ScalarSubnatureDefinition extends IIR_ScalarNatureDefinition
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_SCALAR_SUBNATURE_DEFINITION).
     * @return <code>IR_Kind.IR_SCALAR_SUBNATURE_DEFINITION</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_SCALAR_SUBNATURE_DEFINITION; }
    //CONSTRUCTOR:
    public IIR_ScalarSubnatureDefinition() { }
    //METHODS:  
    public void set_across_tolerance(IIR across_tolerance)
    { _across_tolerance = across_tolerance; }
 
    public IIR get_across_tolerance()
    { return _across_tolerance; }
 
    public void set_through_tolerance(IIR through_tolerance)
    { _through_tolerance = through_tolerance; }
 
    public IIR get_through_tolerance()
    { return _through_tolerance; }
 
    /* FIXME
    public IIR_ArrayNatureDefinition get_base_nature()
    { return _base_nature; }
    */

    //MEMBERS:  

// PROTECTED:
    IIR _across_tolerance;
    IIR _through_tolerance;
} // END class

