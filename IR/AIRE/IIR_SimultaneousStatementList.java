// IIR_SimultaneousStatementList.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The prdefined <code>IIR_SimultaneousStatementList</code> class represents
 * ordered sets containing zero or more
 * <code>IIR_SimultaneousStatement</code>s.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_SimultaneousStatementList.java,v 1.1 1998-10-10 07:53:44 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_SimultaneousStatementList extends IIR_List
{

// PUBLIC:
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    //IR_KIND = IR_SIMULTANEOUS_STATEMENT_LIST
    //CONSTRUCTOR:
    public IIR_SimultaneousStatementList() { }
    //METHODS:  
    public void prepend_element(IIR_SimultaneousStatement element)
    { super._prepend_element(element); }
    public void append_element(IIR_SimultaneousStatement element)
    { super._append_element(element); }
    public boolean 
	insert_after_element(IIR_SimultaneousStatement existing_element,
			     IIR_SimultaneousStatement new_element)
    { return super._insert_after_element(existing_element, new_element); }
    public boolean
	insert_before_element(IIR_SimultaneousStatement existing_element,
			      IIR_SimultaneousStatement new_element)
    { return super._insert_before_element(existing_element, new_element); }
    public boolean remove_element(IIR_SimultaneousStatement existing_element)
    { return super._remove_element(existing_element); }
    public IIR_SimultaneousStatement 
	get_successor_element(IIR_SimultaneousStatement element)
    { return (IIR_SimultaneousStatement)super._get_successor_element(element); }
    public IIR_SimultaneousStatement 
	get_predecessor_element(IIR_SimultaneousStatement element)
    { return (IIR_SimultaneousStatement)super._get_predecessor_element(element); }
    public IIR_SimultaneousStatement get_first_element()
    { return (IIR_SimultaneousStatement)super._get_first_element(); }
    public IIR_SimultaneousStatement get_nth_element(int index)
    { return (IIR_SimultaneousStatement)super._get_nth_element(index); }
    public IIR_SimultaneousStatement get_last_element()
    { return (IIR_SimultaneousStatement)super._get_last_element(); }
    public int get_element_position(IIR_SimultaneousStatement element)
    { return super._get_element_position(element); }
    //MEMBERS:  

// PROTECTED:
} // END class

