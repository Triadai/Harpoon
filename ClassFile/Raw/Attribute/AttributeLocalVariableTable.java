package harpoon.ClassFile.Raw.Attribute;

import harpoon.ClassFile.Raw.*;
import harpoon.ClassFile.Raw.Constant.*;
/** 
 * The <code>LocalVariableTable</code> attribute is an optional
 * variable-length attribute of a <code>Code</code> attribute.  It may
 * be used by debuggers to determine the value of a given local variable
 * during the execution of a method.  If <code>LocalVariableTable</code>
 * attributes are present in the <code>attributes</code> table of a
 * given <code>Code</code> attribute, then they may appear in any
 * order.  There may be no more than one <code>LocalVariableTable</code>
 * attribute per local variable in the <code>Code</code> attribute.
 * <p>
 * This object represents a table mapping local variable indexes to
 * symbolic names.  This attribute is optional; typically it is
 * not included unless debugging flags are given to the compiler.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: AttributeLocalVariableTable.java,v 1.11 1998-08-02 08:20:31 cananian Exp $
 * @see "The Java Virtual Machine Specification, section 4.7.7"
 * @see AttributeCode
 * @see Attribute
 */
public class AttributeLocalVariableTable extends Attribute {
  /** Each entry in the <code>local_variable_table</code> array
      indicates a range of <code>code</code> offsets within which a
      local variable has a value.  It also indicates the index into
      the local variables of the current frame at which that local
      variable can be found. */
  public LocalVariableTable local_variable_table[];

  /** Constructor. */
  AttributeLocalVariableTable(ClassFile parent, ClassDataInputStream in,
			      int attribute_name_index) 
    throws java.io.IOException
  {
    super(parent, attribute_name_index);
    long attribute_length = in.read_u4();

    int local_variable_table_length = in.read_u2();
    local_variable_table = new LocalVariableTable[local_variable_table_length];
    for (int i=0; i<local_variable_table_length; i++)
      local_variable_table[i] = new LocalVariableTable(parent, in);

    if (attribute_length != attribute_length())
      throw new ClassDataException("LocalVariableTable with length "
				   + attribute_length);
  }
  /** Constructor. */
  public AttributeLocalVariableTable(ClassFile parent,int attribute_name_index,
				     LocalVariableTable local_variable_table[])
  {
    super(parent, attribute_name_index);
    this.local_variable_table = local_variable_table;
  }

  public long attribute_length() { 
    return 2 + 10*local_variable_table_length();
  }

  // Convenience.
  public int local_variable_table_length() 
  { return local_variable_table.length; }

  /** Write to bytecode stream. */
  public void write(ClassDataOutputStream out) throws java.io.IOException {
    out.write_u2(attribute_name_index);
    out.write_u4(attribute_length());

    out.write_u2(local_variable_table_length());
    for (int i=0; i< local_variable_table.length; i++)
      local_variable_table[i].write(out);
  }

  /** Get the (debugging) name of a local variable.
   * @param  index the index of the local variable to query.
   * @return the name of the index'th local variable, or null
   *         if none can be found.
   */
  public String localName(int index) {
    for (int i=0; i<local_variable_table.length; i++)
      if (local_variable_table[i].index == index)
	return local_variable_table[i].name();
    return null;
  }

  /** Pretty-print the contents of this attribute.
   *  @param indent the indentation level to use.
   */
  public void print(java.io.PrintWriter pw, int indent) {
    int in=indent;
    indent(pw, in, 
	   "LocalVariableTable attribute ["+local_variable_table.length+"]:");
    for (int i=0; i<local_variable_table.length; i++) {
      indent(pw, in+1, "#"+i+":");
      local_variable_table[i].print(pw, in+2);
    }
  }
}
