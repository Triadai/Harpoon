// HCLibrary.java, created Mon Dec 28 21:01:12 1998 by cananian
package harpoon.Interpret.Quads;

import harpoon.ClassFile.*;

/**
 * <code>HCLibrary</code> is a simple superclass designed to bring the
 * <code>HClass</code> objects for common classes into class scope.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HCLibrary.java,v 1.1.2.1 1998-12-30 04:39:39 cananian Exp $
 */
class HCLibrary  {
    static final HClass 
	HCcharA = HClass.forDescriptor("[C"),
	HCstring = HClass.forName("java.lang.String"),
	HCstringA = HClass.forDescriptor("[Ljava/lang/String;"),
	HCclass = HClass.forName("java.lang.Class"),
	HCclassA = HClass.forDescriptor("[Ljava/lang/Class;"),
	HCobject  = HClass.forName("java.lang.Object"),
	HCsystem = HClass.forName("java.lang.System"),
	HCthrowable=HClass.forName("java.lang.Throwable"),
	HCfiledesc = HClass.forName("java.io.FileDescriptor"),
	HCfostream = HClass.forName("java.io.FileOutputStream"),
	HCrafile = HClass.forName("java.io.RandomAccessFile"),
	HCproperties = HClass.forName("java.util.Properties"),
	HCarraystoreE = HClass.forName("java.lang.ArrayStoreException"),
	HCarrayindexE = HClass.forName("java.lang.ArrayIndexOutOfBounds"+
				       "Exception"),
	HCclassnotfoundE=HClass.forName("java.lang.ClassNotFoundException"),
	HCillegalaccessE=HClass.forName("java.lang.IllegalAccessException"),
	HCinstantiationE=HClass.forName("java.lang.InstantiationException"),
	HCnullpointerE = HClass.forName("java.lang.NullPointerException"),
	HCnegativearrayE = HClass.forName("java.lang.NegativeArraySize"+
					  "Exception"),
	HCarithmeticE = HClass.forName("java.lang.ArithmeticException"),
	HCioE = HClass.forName("java.io.IOException"),
	HCsecurityE = HClass.forName("java.lang.SecurityException"),
	HCnosuchmethodErr=HClass.forName("java.lang.NoSuchMethodError"),
	HCunsatisfiedlinkErr=HClass.forName("java.lang.UnsatisfiedLinkError");
    
}
