// IIR_PortList.java, created by cananian
package harpoon.IR.AIRE;

/**
 * The <code>IIR_PortList</code> class represents ordered sets containing
 * zero or more <code>IIR_SignalInterfaceDeclaration</code>s.  Port
 * list classes appear as predefined data elements within 
 * <code>IIR_EntityDeclaration</code>s,
 * <code>IIR_BlockStatement</code>s, and
 * <code>IIR_ComponentDeclaration</code>s.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_PortList.java,v 1.3 1998-10-11 01:25:00 cananian Exp $
 */

//-----------------------------------------------------------
public class IIR_PortList extends IIR_List
{

// PUBLIC:
    /** Accept a visitor class. */
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    /**
     * Returns the <code>IR_Kind</code> of this class (IR_PORT_LIST).
     * @return <code>IR_Kind.IR_PORT_LIST</code>
     */
    public IR_Kind get_kind()
    { return IR_Kind.IR_PORT_LIST; }
    //CONSTRUCTOR:
    public IIR_PortList() { }
    //METHODS:  
    public void prepend_element(IIR_SignalInterfaceDeclaration element)
    { super._prepend_element(element); }
    public void append_element(IIR_SignalInterfaceDeclaration element)
    { super._append_element(element); }
    public boolean 
	insert_after_element(IIR_SignalInterfaceDeclaration existing_element,
			     IIR_SignalInterfaceDeclaration new_element)
    { return super._insert_after_element(existing_element, new_element); }
    public boolean
	insert_before_element(IIR_SignalInterfaceDeclaration existing_element,
			      IIR_SignalInterfaceDeclaration new_element)
    { return super._insert_before_element(existing_element, new_element); }
    public boolean remove_element(IIR_SignalInterfaceDeclaration existing_element)
    { return super._remove_element(existing_element); }
    public IIR_SignalInterfaceDeclaration 
	get_successor_element(IIR_SignalInterfaceDeclaration element)
    { return (IIR_SignalInterfaceDeclaration)super._get_successor_element(element); }
    public IIR_SignalInterfaceDeclaration 
	get_predecessor_element(IIR_SignalInterfaceDeclaration element)
    { return (IIR_SignalInterfaceDeclaration)super._get_predecessor_element(element); }
    public IIR_SignalInterfaceDeclaration get_first_element()
    { return (IIR_SignalInterfaceDeclaration)super._get_first_element(); }
    public IIR_SignalInterfaceDeclaration get_nth_element(int index)
    { return (IIR_SignalInterfaceDeclaration)super._get_nth_element(index); }
    public IIR_SignalInterfaceDeclaration get_last_element()
    { return (IIR_SignalInterfaceDeclaration)super._get_last_element(); }
    public int get_element_position(IIR_SignalInterfaceDeclaration element)
    { return super._get_element_position(element); }
    //MEMBERS:  

// PROTECTED:
} // END class

