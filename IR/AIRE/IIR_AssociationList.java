// IIR_AssociationList.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The <code>IIR_AssociationList</code> class represents ordered sets
 * containing zero or more <code>IIR_AssociationElement</code>s.
 * Association lists are either used at an elaboration interface to
 * associate actuals with formals or to represent elements of an
 * aggregate value.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_AssociationList.java,v 1.1 1998-10-10 07:53:32 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_AssociationList extends IIR_List
{

// PUBLIC:
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    //IR_KIND = IR_ASSOCIATION_LIST
    //CONSTRUCTOR:
    public IIR_AssociationList() { }
    //METHODS:  
    public void prepend_element(IIR_AssociationElement element)
    { super._prepend_element(element); }
    public void append_element(IIR_AssociationElement element)
    { super._append_element(element); }
    public boolean 
	insert_after_element(IIR_AssociationElement existing_element,
			     IIR_AssociationElement new_element)
    { return super._insert_after_element(existing_element, new_element); }
    public boolean
	insert_before_element(IIR_AssociationElement existing_element,
			      IIR_AssociationElement new_element)
    { return super._insert_before_element(existing_element, new_element); }
    public boolean remove_element(IIR_AssociationElement existing_element)
    { return super._remove_element(existing_element); }
    public IIR_AssociationElement 
	get_successor_element(IIR_AssociationElement element)
    { return (IIR_AssociationElement)super._get_successor_element(element); }
    public IIR_AssociationElement 
	get_predecessor_element(IIR_AssociationElement element)
    { return (IIR_AssociationElement)super._get_predecessor_element(element); }
    public IIR_AssociationElement get_first_element()
    { return (IIR_AssociationElement)super._get_first_element(); }
    public IIR_AssociationElement get_nth_element(int index)
    { return (IIR_AssociationElement)super._get_nth_element(index); }
    public IIR_AssociationElement get_last_element()
    { return (IIR_AssociationElement)super._get_last_element(); }
    public int get_element_position(IIR_AssociationElement element)
    { return super._get_element_position(element); }

    //MEMBERS:  

// PROTECTED:
} // END class

