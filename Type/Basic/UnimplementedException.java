package harpoon.Type.Basic;

/**
 * An unimplementedException is thrown when we try to use a feature
 * that a particular datatype does not support.
 *
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: UnimplementedException.java,v 1.1 1998-07-29 00:56:49 cananian Exp $
 * @see Datatype
 */
public class UnimplementedException extends Exception {
  /** Constructs an UnimplementedException with no detail message. */
  UnimplementedException() { super(); }
  /** Constructs an UnimplementedException with the specified detail message.
   *  @param s the detail message.
   */
  UnimplementedException(String s) { super(s); }
}
