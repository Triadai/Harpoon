package harpoon.ClassFile.Raw.Constant;

import harpoon.ClassFile.Raw.*;
import harpoon.Util.Util;
/**
 * The <code>CONSTANT_Utf8_info</code> structure is used to represent
 * constant string values. <p> UTF-8 strings are encoded so that
 * character sequences that contain only non-null ASCII characters can
 * be represented using only one byte per character, but characters of
 * up to 16 bits can be represented.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ConstantUtf8.java,v 1.11 1998-08-07 09:53:04 cananian Exp $
 * @see "The Java Virtual Machine Specification, section 4.4.7"
 * @see Constant
 */
public class ConstantUtf8 extends Constant {
  /** The value of the string constant */
  public String val;

  /** Constructor. */
  ConstantUtf8(ClassFile parent, ClassDataInputStream in) 
    throws java.io.IOException {
    super(parent);
    val = in.readUTF();
  }
  /** Constructor. */
  public ConstantUtf8(ClassFile parent, String val) {
    super(parent);
    this.val = val;
  }

  /** Write to a bytecode file. */
  public void write(ClassDataOutputStream out) throws java.io.IOException {
    out.write_u1(CONSTANT_Utf8);
    out.writeUTF(val);
  }

  /** Create a human-readable representation of this constant. */
  public String toString() {
    return "CONSTANT_Utf8: \"" + Util.escape(val) + "\"";
  }
}
