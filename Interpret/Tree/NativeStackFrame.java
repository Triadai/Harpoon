// NativeStackFrame.java, created Mon Dec 28 17:22:54 1998 by cananian
package harpoon.Interpret.Tree;

import harpoon.ClassFile.HMethod;

/**
 * <code>NativeStackFrame</code>
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: NativeStackFrame.java,v 1.1.2.1 1999-03-27 22:05:09 duncan Exp $
 */
final class NativeStackFrame extends StackFrame {
    final HMethod method;
    NativeStackFrame(HMethod method) { this.method = method; }
    HMethod getMethod() { return method; }
    String  getSourceFile() { return "--native--"; }
    int     getLineNumber() { return 0; }
}
