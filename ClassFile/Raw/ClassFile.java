package harpoon.ClassFile.Raw;

/**
 * Represents a java bytecode class file.
 * <p>Drawn from <i>The Java Virtual Machine Specification</i>.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ClassFile.java,v 1.6 1998-07-31 06:21:55 cananian Exp $
 */

public class ClassFile {
  // descriptions taken from The Java Virtual Machine Specification, sect 4.1
  /** The magic item supplies the magic number identifying the
      <code>class</code> file format; it has the value 0xCAFEBABE. */
  static final long MAGIC=0xcafebabeL;

  /** The minor version number of the compiler that produced this
      <code>class</code> file. */
  int minor_version;
  /** The major version number of the compiler that produced this
      <code>class</code> file. */
  int major_version;

  /** The <code>constant_pool</code> is a table of variable-length
      structures representing various string constants, class names,
      field names, and other constants that are referred to within the
      <code>ClassFile</code> structure and its substructures.
      
      The first entry of the <code>constant_pool</code> table,
      <code>constant_pool[0]</code>, is reserved for internal use by a
      Java Virtual Machine Implementation.  That entry is <i>not</i>
      present in the <code>class</code> file. */
  Constant constant_pool[];
  /** The value of the <code>access_flags</code> item is a mask of
      modifiers used with class and interface declarations. */
  AccessFlags access_flags;

  /** The value of the <code>this_class</code> item must be a valid
      index into the <code>constant_pool</code> table.  The
      <code>constant_pool</code> entry at that index must be a
      <code>CONSTANT_Class_info</code> structure representing the
      class or interface defined by this <code>class</code> file. */
  int this_class;
  /** For a class, the value of the <code>super_class</code> item
      either must be zero or must be a valid index into the
      <code>constant_pool</code> table.  If the value of the
      <code>super_class</code> item is nonzero, the
      <code>constant_pool</code> entry at that index must be a
      <code>CONSTANT_Class_info</code> structure representing the
      superclass of the class defined by this <code>class</code>
      file.  Neither the superclass nor any of its superclasses may be
      a <code>final</code> class.
      <p>
      If the value of <code>super_class</code> is zero, then this
      <code>class</code> file must represent that class
      <code>java.lang.Object</code>, the only class or interface
      without a superclass.
      <p>
      For an interface, the value of <code>super_class</code> must
      always be a valid index into the <code>constant_pool</code>
      table.  The <code>constant_pool</code> entry at that index must
      be a <code>CONSTANT_Class_info</code> structure representing the
      class <code>java.lang.Object</code>. */
  int super_class;

  /** Each value in the <code>interfaces</code> array must be a valid
      index into the <code>constant_pool</code> table.  The
      <code>constant_pool</code> entry at each value of
      <code>interfaces[i]</code> must be a
      <code>CONSTANT_Class_info</code> structure representing an
      interface which is a direct superinterface of this class or
      interface type, in the left-to-right order given in the source
      for the type. */
  int interfaces[];

  /** Each value in the <code>fields</code> table must be a
      variable-length <code>field_info</code> structure giving a
      complete description of a field in the class or interface type.
      The <code>fields</code> table includes only those fields that
      are declared by this class or interface.  It does not include
      items representing fields that are inherited from superclasses
      or superinterfaces. */
  FieldInfo fields[];
  /** Each value in the <code>methods</code> table must be a
      variable-length <code>method_info</code> structure giving a
      complete description of and Java Virtual Machine code for a
      method in the class or interface.
      <p>
      The <code>method_info</code> structures represent all methods,
      both instance methods and, for classes, class
      (<code>static</code>) methods, declared by this class or
      interface type.  The <code>methods</code> table only includes
      those items that are explicitly declared by this class.
      Interfaces have only the single method
      <code>&lt;clinit&gt;</code>, the interface initialization
      method.  The <code>methods</code> table does not include items
      representing methods that are inherited from superclasses or
      superinterfaces. */
  MethodInfo methods[];
  /** Each value of the <code>attributes</code> table must be a
      variable-length attribute structure. A <code>ClassFile</code>
      structure can have any number of attributes associated with it.
      <p>
      The only attribute defined by this specification for the
      <code>attributes</code> table of a <code>ClassFile</code>
      structure is the <code>SourceFile</code> attribute. */
  Attribute attributes[];

  void read(ClassDataInputStream in) throws java.io.IOException {
    int constant_pool_count;
    int interfaces_count;
    int fields_count;
    int methods_count;
    int attributes_count;

    long magic = in.read_u4();
    if (magic != MAGIC)
      throw new ClassDataException("Bad magic: " + Long.toHexString(magic));
    
    minor_version = in.read_u2();
    major_version = in.read_u2();

    constant_pool_count = in.read_u2();
    constant_pool = new Constant[constant_pool_count];
    // FIXME initialize constant_pool[0] (jvm dependant; ignore)
    for (int i=1; i<constant_pool_count; i++) {
      constant_pool[i] = Constant.read(this, in);
      if (constant_pool[i] instanceof ConstantLong ||
	  constant_pool[i] instanceof ConstantDouble)
	i++; // Long and Double constants take up two entries.
    }

    access_flags = new AccessFlags(in);

    this_class   = in.read_u2();
    super_class  = in.read_u2();

    interfaces_count = in.read_u2();
    interfaces = new int[interfaces_count];
    for (int i=0; i<interfaces_count; i++)
      interfaces[i] = in.read_u2();

    fields_count = in.read_u2();
    fields = new FieldInfo[fields_count];
    for (int i=0; i<fields_count; i++)
      fields[i] = new FieldInfo(this, in);

    methods_count = in.read_u2();
    methods = new MethodInfo[methods_count];
    for (int i=0; i<methods_count; i++)
      methods[i] = new MethodInfo(this, in);

    attributes_count = in.read_u2();
    attributes = new Attribute[attributes_count];
    for (int i=0; i<attributes_count; i++)
      attributes[i] = Attribute.read(this, in);
  }

  /** Write a class file object out to a java bytecode file. */
  public void write(ClassDataOutputStream out)
    throws java.io.IOException 
  {
    out.write_u4(MAGIC);
    out.write_u2(minor_version);
    out.write_u2(major_version);
    if (constant_pool.length > 0xFFFF)
      throw new ClassDataException("Constant Pool too large: " +
				   constant_pool.length);
    out.write_u2(constant_pool.length);
    for (int i=1; i<constant_pool.length; i++) {
      constant_pool[i].write(out);
      if (constant_pool[i] instanceof ConstantLong ||
	  constant_pool[i] instanceof ConstantDouble)
	i++; // Long and Double constants take up two entries.
    }

    access_flags.write(out);

    out.write_u2(this_class);
    out.write_u2(super_class);

    if (interfaces.length > 0xFFFF)
      throw new ClassDataException("Interfaces list too long: " +
				   interfaces.length);
    out.write_u2(interfaces.length);
    for (int i=0; i< interfaces.length; i++)
      out.write_u2(interfaces[i]);

    if (fields.length > 0xFFFF)
      throw new ClassDataException("Fields list too long: " + fields.length);
    out.write_u2(fields.length);
    for (int i=0; i< fields.length; i++)
      fields[i].write(out);

    if (methods.length > 0xFFFF)
      throw new ClassDataException("Methods list too long: " + methods.length);
    out.write_u2(methods.length);
    for (int i=0; i< methods.length; i++)
      methods[i].write(out);

    if (attributes.length > 0xFFFF)
      throw new ClassDataException("Attributes list too long: " +
				   attributes.length);
    out.write_u2(attributes.length);
    for (int i=0; i<attributes.length; i++)
      attributes[i].write(out);
    //done.
  }
  /** Write a class file object out to a java bytecode file. */
  public void write(java.io.OutputStream out) throws java.io.IOException {
    write(new ClassDataOutputStream(out));
  }

  /** Create a <code>ClassFile</code> object by reading data from a
      bytecode file. */
  public ClassFile(ClassDataInputStream in) throws java.io.IOException {
    read(in);
  }
  /** Create a <code>ClassFile</code> object by reading data from a
      bytecode file. */
  public ClassFile(java.io.InputStream in) throws java.io.IOException {
    read(new ClassDataInputStream(in));
  }

  // Interrogate the data structures. (convenience functions)

  /** Return the <code>CONSTANT_Class_info</code> entry in the
      <code>constant_pool</code> corresponding to the value of
      <code>this_class</code>. */
  public ConstantClass this_class() 
  { return (ConstantClass) constant_pool[this_class]; }
  /** Return the <code>CONSTANT_Class_info</code> entry in the
      <code>constant_pool</code> corresponding to the value of
      <code>super_class</code>, or <code>null</code> if
      <code>super_class</code> == 0. */
  public ConstantClass super_class()
  { return (super_class==0)?null:(ConstantClass) constant_pool[super_class]; }
  /** Return the <code>CONSTANT_Class_info</code> entry in the
      <code>constant_pool</code> corresponding to the value in
      <code>interfaces[i]</code>. */
  public ConstantClass interfaces(int i)
  { return (ConstantClass) constant_pool[interfaces[i]]; }
}
