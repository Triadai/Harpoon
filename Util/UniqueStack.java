// UniqueStack.java, created Thu Sep 10 19:08:22 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util;

import java.util.EmptyStackException;
/**
 * The <code>UniqueStack</code> class represents a last-in-first-out
 * stack of <b>unique</b> objects.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: UniqueStack.java,v 1.3 1998-10-11 02:37:59 cananian Exp $
 */

public class UniqueStack extends UniqueVector implements Worklist {
    /** 
     * Pushes an item onto the top of this stack, if it is unique.
     * Otherwise, does nothing.
     * @param item the item to be pushed onto this stack.
     * @return the <code>item</code> argument.
     */
    public Object push(Object item) {
	addElement(item);
	return item;
    }
    /**
     * Removes the object at the top of this stack and returns that
     * object as the value of this function.
     * @return The object at the top of this stack.
     * @exception EmptyStackException if this empty.
     */
    public synchronized Object pop() {
	Object obj;
	int len = size();
	obj = peek();
	removeElementAt(len-1);
	return obj;
    }
    public Object pull() { return pop(); }

    /**
     * Looks at the object at the top of this stack without removing it
     * from the stack.
     * @return the object at the top of this stack.
     * @exception EmptyStackException if this stack is empty.
     */
    public synchronized Object peek() {
	int len = size();
	if (len==0) throw new EmptyStackException();
	return elementAt(len-1);
    }
    /**
     * Tests if this stack is empty.
     * @return <code>true</code> if this stack is empty;
     *         <code>false</code> otherwise.
     */
    public boolean empty() { return isEmpty(); }

    /**
     * Returns where an object is on this stack.
     * @param o the desired object.
     * @return the distance from the top of the stack where the object is
     *         located; the return value <code>-1</code> indicates that the
     *         object is not on the stack.
     */
    public synchronized int search(Object o) {
	int i = lastIndexOf(o);
	if (i >= 0) return size()-i;
	return -1;
    }

    /** Creates a <code>UniqueStack</code>. */
    public UniqueStack() {
        super();
    }
}
