// HFieldProxy.java, created Tue Jan 11 08:14:00 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

/**
 * An <code>HFieldProxy</code> is a relinkable proxy for an
 * <code>HField</code>.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HFieldProxy.java,v 1.1.4.3 2000-03-30 09:58:27 cananian Exp $
 * @see HField
 */
class HFieldProxy extends HMemberProxy
    implements HField, HFieldMutator, java.io.Serializable {
    HField proxy;
    HFieldMutator proxyMutator;
    
    /** Creates a <code>HFieldProxy</code>. */
    HFieldProxy(Relinker relinker, HField proxy) {
	super(relinker, proxy);
	relink(proxy);
    }
    void relink(HField proxy) {
	super.relink(proxy);
	this.proxy = proxy;
	this.proxyMutator = (proxy==null) ? null : proxy.getMutator();
    }

    public HFieldMutator getMutator() {
	if (proxyMutator==null)
	    ((HClassProxy)getDeclaringClass()).getMutator();
	return (proxyMutator==null) ? null : this;
    }
    // HField interface
    public HClass getType() { return wrap(proxy.getType()); }
    public Object getConstant() { return proxy.getConstant(); }
    public boolean isConstant() { return proxy.isConstant(); }
    public boolean isStatic() { return proxy.isStatic(); }
    // HFieldMutator interface
    public void addModifiers(int m) { proxyMutator.addModifiers(m); }
    public void setModifiers(int m) { proxyMutator.setModifiers(m); }
    public void removeModifiers(int m) { proxyMutator.removeModifiers(m); }
    public void setConstant(Object co) { proxyMutator.setConstant(co); }
    public void setSynthetic(boolean is) { proxyMutator.setSynthetic(is); }
    public void setType(HClass type) { proxyMutator.setType(unwrap(type)); }
    /** Serializable interface. */
    public Object writeReplace() { return new HFieldImpl.HFieldStub(this); }
}
