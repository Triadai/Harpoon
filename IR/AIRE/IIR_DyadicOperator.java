// IIR_DyadicOperator.java, created by cananian
package harpoon.IR.AIRE;

/**
 * <code>IIR_DyadicOperator</code> 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: IIR_DyadicOperator.java,v 1.3 1998-10-11 00:32:19 cananian Exp $
 */

//-----------------------------------------------------------
public abstract class IIR_DyadicOperator extends IIR_Expression
{

// PUBLIC:
    public void accept(IIR_Visitor visitor ){visitor.visit(this);}
    
    
    //METHODS:  
    public void set_left_operand(IIR left_operand)
    { _left_operand = left_operand; }
 
    public IIR get_left_operand()
    { return _left_operand; }
 
    public void set_right_operand(IIR right_operand)
    { _right_operand = right_operand; }
 
    public IIR get_right_operand()
    { return _right_operand; }
 
    //MEMBERS:  

// PROTECTED:
    IIR _left_operand;
    IIR _right_operand;
} // END class

