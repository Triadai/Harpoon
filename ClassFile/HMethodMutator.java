// HMethodMutator.java, created Mon Jan 10 20:08:06 2000 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

/**
 * <code>HMethodMutator</code> allows you to change properties of
 * an <code>HMethod</code>.
 * @see HMethod#getMutator
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HMethodMutator.java,v 1.1.2.2 2000-01-11 02:06:36 cananian Exp $
 */
public interface HMethodMutator {
    public void addModifiers(int m);
    public void setModifiers(int m);
    public void removeModifiers(int m);

    public void setReturnType(HClass returnType);
    /** Warning: use can cause method name conflicts in class. */
    public void setParameterTypes(HClass[] parameterTypes);
    /** Warning: use can cause method name conflicts in class. */
    public void setParameterType(int which, HClass type);

    public void setParameterNames(String[] parameterNames);
    public void setParameterName(int which, String name);

    public void addExceptionType(HClass exceptionType);
    public void setExceptionTypes(HClass[] exceptionTypes);
    public void removeExceptionType(HClass exceptionType);

    public void setSynthetic(boolean isSynthetic);
}
