// IIR_GroupDeclaration.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The predefined <code>IIR_GroupDeclaration</code> class represents
 * explicit, named collections of entities corresponding to
 * a group template declaration.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_GroupDeclaration.java,v 1.2 1998-10-10 09:21:38 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_GroupDeclaration extends IIR_Declaration
{

// PUBLIC:
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    //IR_KIND = IR_GROUP_DECLARATION
    //CONSTRUCTOR:
    public IIR_GroupDeclaration() { }
    //METHODS:  
    public void set_group_template(IIR_Name group_template_name)
    { _group_template_name = group_template_name; }
 
    public IIR_Name get_group_template_name()
    { return _group_template_name; }
 
    //MEMBERS:  
    public IIR_DesignatorList group_constituent_list;
    public IIR_AttributeSpecificationList attributes;

// PROTECTED:
    IIR_Name _group_template_name;
} // END class

