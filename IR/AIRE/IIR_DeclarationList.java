// IIR_DeclarationList.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The predefined <code>IIR_DeclarationList</code> class represents ordered
 * sets containing zero or more <code>IIR_Declaration<code>s (such
 * declarations broadly include declarations, specifications, and 
 * use clauses).  Such declaration lists are directly incorporated into
 * many other predefined IIR classes.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_DeclarationList.java,v 1.3 1998-10-11 01:24:55 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_DeclarationList extends IIR_List
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_DECLARATION_LIST).
     * @return <code>IR_Kind.IR_DECLARATION_LIST</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_DECLARATION_LIST; }
    //CONSTRUCTOR:
    public IIR_DeclarationList() { }
    //METHODS:  
    public void prepend_element(IIR_Declaration element)
    { super._prepend_element(element); }
    public void append_element(IIR_Declaration element)
    { super._append_element(element); }
    public boolean 
	insert_after_element(IIR_Declaration existing_element,
			     IIR_Declaration new_element)
    { return super._insert_after_element(existing_element, new_element); }
    public boolean
	insert_before_element(IIR_Declaration existing_element,
			      IIR_Declaration new_element)
    { return super._insert_before_element(existing_element, new_element); }
    public boolean remove_element(IIR_Declaration existing_element)
    { return super._remove_element(existing_element); }
    public IIR_Declaration 
	get_successor_element(IIR_Declaration element)
    { return (IIR_Declaration)super._get_successor_element(element); }
    public IIR_Declaration 
	get_predecessor_element(IIR_Declaration element)
    { return (IIR_Declaration)super._get_predecessor_element(element); }
    public IIR_Declaration get_first_element()
    { return (IIR_Declaration)super._get_first_element(); }
    public IIR_Declaration get_nth_element(int index)
    { return (IIR_Declaration)super._get_nth_element(index); }
    public IIR_Declaration get_last_element()
    { return (IIR_Declaration)super._get_last_element(); }
    public int get_element_position(IIR_Declaration element)
    { return super._get_element_position(element); }
    //MEMBERS:  

// PROTECTED:
} // END class

