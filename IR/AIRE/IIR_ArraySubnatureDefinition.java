// IIR_ArraySubnatureDefinition.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The <code>IIR_ArraySubnatureDefinition</code> class represents predefined
 * methods, subprograms, and public data elements describing array
 * subnatures.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_ArraySubnatureDefinition.java,v 1.1 1998-10-10 09:21:37 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_ArraySubnatureDefinition extends IIR_ArrayNatureDefinition
{

// PUBLIC:
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    //IR_KIND = IR_ARRAY_SUBNATURE_DEFINITION
    //CONSTRUCTOR:
    public IIR_ArraySubnatureDefinition() { }
    //METHODS:  
    public void set_across_tolerance(IIR across_tolerance)
    { _across_tolerance = across_tolerance; }
 
    public IIR get_across_tolerance()
    { return _across_tolerance; }
 
    public void set_through_tolerance(IIR through_tolerance)
    { _through_tolerance = through_tolerance; }
 
    public IIR get_through_tolerance()
    { return _through_tolerance; }
 
    public void set_base_nature(IIR_ArrayNatureDefinition base_nature)
    { _base_nature = base_nature; }
    public IIR_ArrayNatureDefinition get_base_nature()
    { return _base_nature; }
 
    //MEMBERS:  

// PROTECTED:
    IIR _across_tolerance;
    IIR _through_tolerance;
    IIR_ArrayNatureDefinition _base_nature;
} // END class

