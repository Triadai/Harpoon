// HFieldProxy.java, created Tue Jan 11 08:14:00 2000 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

/**
 * An <code>HFieldProxy</code> is a relinkable proxy for an
 * <code>HField</code>.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HFieldProxy.java,v 1.1.2.1 2000-01-11 15:31:41 cananian Exp $
 * @see HField
 */
class HFieldProxy implements HField, HFieldMutator {
    Relinker relinker;
    HField proxy;
    HFieldMutator proxyMutator;
    
    /** Creates a <code>HFieldProxy</code>. */
    HFieldProxy(Relinker relinker, HField proxy) {
	this.relinker = relinker;
	relink(proxy);
    }
    void relink(HField proxy) {
	this.proxy = proxy;
	this.proxyMutator = proxy.getMutator();
    }

    public HFieldMutator getMutator() {
	return (proxyMutator==null) ? null : this;
    }
    // HField interface
    public HClass getDeclaringClass() {return wrap(proxy.getDeclaringClass());}
    public String getName() { return proxy.getName(); }
    public int getModifiers() { return proxy.getModifiers(); }
    public HClass getType() { return wrap(proxy.getType()); }
    public String getDescriptor() { return proxy.getDescriptor(); }
    public Object getConstant() { return proxy.getConstant(); }
    public boolean isConstant() { return proxy.isConstant(); }
    public boolean isSynthetic() { return proxy.isSynthetic(); }
    public boolean isStatic() { return proxy.isStatic(); }
    public boolean equals(Object object) { return proxy.equals(object); }
    public int hashCode() { return proxy.hashCode(); }
    public String toString() { return proxy.toString(); }
    // HFieldMutator interface
    public void addModifiers(int m) { proxyMutator.addModifiers(m); }
    public void setModifiers(int m) { proxyMutator.setModifiers(m); }
    public void removeModifiers(int m) { proxyMutator.removeModifiers(m); }
    public void setConstant(Object co) { proxyMutator.setConstant(co); }
    public void setSynthetic(boolean is) { proxyMutator.setSynthetic(is); }
    public void setType(HClass type) { proxyMutator.setType(unwrap(type)); }

    // keep member map up-to-date when hashcode changes.
    void flushMemberMap() { relinker.memberMap.remove(proxy); }
    void updateMemberMap() { relinker.memberMap.put(proxy, this); }

    // wrap/unwrap
    private HClass wrap(HClass hc) { return relinker.wrap(hc); }
    private HClass unwrap(HClass hc) { return relinker.unwrap(hc); }
}
