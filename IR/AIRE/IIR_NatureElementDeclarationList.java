// IIR_NatureElementDeclarationList.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The predefined <code>IIR_NatureElementDeclarationList</code> class
 * represents ordered sets containing zero or more 
 * <code>IIR_NatureElementDeclaration</code>s.  Element declaration lists
 * appear as public data elements within record type definitions.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_NatureElementDeclarationList.java,v 1.3 1998-10-11 01:24:59 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_NatureElementDeclarationList extends IIR_List
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_NATURE_ELEMENT_DECLARATION_LIST).
     * @return <code>IR_Kind.IR_NATURE_ELEMENT_DECLARATION_LIST</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_NATURE_ELEMENT_DECLARATION_LIST; }
    //CONSTRUCTOR:
    public IIR_NatureElementDeclarationList() { }
    //METHODS:  
    public void prepend_element(IIR_NatureElementDeclaration element)
    { super._prepend_element(element); }
    public void append_element(IIR_NatureElementDeclaration element)
    { super._append_element(element); }
    public boolean 
	insert_after_element(IIR_NatureElementDeclaration existing_element,
			     IIR_NatureElementDeclaration new_element)
    { return super._insert_after_element(existing_element, new_element); }
    public boolean
	insert_before_element(IIR_NatureElementDeclaration existing_element,
			      IIR_NatureElementDeclaration new_element)
    { return super._insert_before_element(existing_element, new_element); }
    public boolean remove_element(IIR_NatureElementDeclaration existing_element)
    { return super._remove_element(existing_element); }
    public IIR_NatureElementDeclaration 
	get_successor_element(IIR_NatureElementDeclaration element)
    { return (IIR_NatureElementDeclaration)super._get_successor_element(element); }
    public IIR_NatureElementDeclaration 
	get_predecessor_element(IIR_NatureElementDeclaration element)
    { return (IIR_NatureElementDeclaration)super._get_predecessor_element(element); }
    public IIR_NatureElementDeclaration get_first_element()
    { return (IIR_NatureElementDeclaration)super._get_first_element(); }
    public IIR_NatureElementDeclaration get_nth_element(int index)
    { return (IIR_NatureElementDeclaration)super._get_nth_element(index); }
    public IIR_NatureElementDeclaration get_last_element()
    { return (IIR_NatureElementDeclaration)super._get_last_element(); }
    public int get_element_position(IIR_NatureElementDeclaration element)
    { return super._get_element_position(element); }
    //MEMBERS:  

// PROTECTED:
} // END class

