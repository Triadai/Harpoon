package harpoon.ClassFile.Raw.Attribute;

import harpoon.ClassFile.Raw.*;
import harpoon.ClassFile.Raw.Constant.*;
/**
 * The <code>ConstantValue</code> attribute is a fixed-length
 * attribute used in the <code>attributes</code> table of the
 * <code>field_info</code> structures. A <code>ConstantValue</code>
 * attribute represents the value of a constant field that must be
 * (explicitly or implicitly) <code>static</code>; that is, the
 * <code>ACC_STATIC</code> bit in the <code>flags</code> item of the
 * <code>field_info</code> structure must be set.  The field is not
 * required to be <code>final</code>.  There can be no more than one
 * <code>ConstantValue</code> attribute in the <code>attributes</code>
 * table of a given <code>field_info</code> structure.  The constant
 * field represented by the <code>field_info</code> structure is
 * assigned the value referenced by its <code>ConstantValue</code>
 * attribute as part of its initialization.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: AttributeConstantValue.java,v 1.7 1998-08-01 22:55:16 cananian Exp $
 * @see Attribute
 * @see FieldInfo
 * @see ClassFile
 */
public class AttributeConstantValue extends Attribute {
  /** The value of the <code>constantvalue_index</code> must be a
      valid index into the <code>constant_pool</code> table.  The
      <code>constant_pool</code> entry at that index must give the
      constant value represented by this attribute. <p>  The
      <code>constant_pool</code> entry must be of a type appropriate
      to the field. */
  public int constantvalue_index;

  /** Constructor. */
  AttributeConstantValue(ClassFile parent, ClassDataInputStream in,
			 int attribute_name_index) throws java.io.IOException {
    super(parent, attribute_name_index);
    long attribute_length = in.read_u4();
    if (attribute_length != 2)
      throw new ClassDataException("ConstantValue attribute with length " +
				   attribute_length);

    constantvalue_index = in.read_u2();
  }
  /** Constructor. */
  public AttributeConstantValue(ClassFile parent, int attribute_name_index,
				int constantvalue_index) {
    super(parent, attribute_name_index);
    this.constantvalue_index = constantvalue_index;
  }

  public long attribute_length() { return 2; }
  
  // convenience.
  public Constant constantvalue_index()
  { return parent.constant_pool[constantvalue_index]; }

  /** Write to bytecode stream. */
  public void write(ClassDataOutputStream out) throws java.io.IOException {
    out.write_u2(attribute_name_index);
    out.write_u4(attribute_length());
    out.write_u2(constantvalue_index);
  }
}
