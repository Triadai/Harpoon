// ToSSA.java, created Sat Jul  3 01:26:14 1999 by root
// Copyright (C) 1999 root <root@bdemsky.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import harpoon.Analysis.SSITOSSAMap;
import harpoon.Temp.TempMap;
import harpoon.Temp.Temp;
import harpoon.ClassFile.*;
import harpoon.IR.Quads.*;
import harpoon.IR.LowQuad.*;

/**
 * <code>ToSSA</code>
 * Converts SSI to SSA.  Should work on LowQuads and Quads. 
 *
 * @author  Brian Demsky <bdemsky@mit.edu>
 * @version $Id: ToSSA.java,v 1.1.2.6 1999-10-23 05:59:19 cananian Exp $
 */

public final class ToSSA {
    TempMap ssitossamap;

    /** <code>ToSSA</code> takes in a TempMap and returns a <code>ToSSA</code>
     *  object.*/
    public ToSSA(TempMap ssitossamap) {
	this.ssitossamap=ssitossamap;
    }

    /** Creates a <code>toSSA</code> codeFactory. */
    public static HCodeFactory codeFactory(final HCodeFactory parent) {
	return new HCodeFactory() {
	    public HCode convert(HMethod m) {
		HCode hc = parent.convert(m);
		try {
		hc = hc.clone(m);
		} catch (CloneNotSupportedException e) {
		    System.out.println("Error:  clone not supported on class handed to ToSSA");
		}
		if (hc!=null) {
		    (new ToSSA(new SSITOSSAMap(hc))).optimize(hc);
		}
		return hc;
	    }
	    public String getCodeName() { return parent.getCodeName(); }
	    public void clear(HMethod m) { parent.clear(m); }
	};
    }

    /** This method takes in a HCode and transforms it from SSI to SSA.*/
    public void optimize(final HCode hc) {
	SSAVisitor visitor=new SSAVisitor(ssitossamap);
	Quad[] ql = (Quad[]) hc.getElements();
	for (int i=0; i<ql.length; i++)
	    ql[i].accept(visitor);
    }

    class SSAVisitor extends LowQuadVisitor {
	SSAVisitor(TempMap ssitossamap) {
	    this.ssitossamap=ssitossamap;
	}
	
	public void visit(Quad q) {
	    //Build a new quad and link it in
	    Quad newquad=q.rename(ssitossamap,ssitossamap);
	    Quad []prev=q.prev();
	    Quad []next=q.next();
	    for(int i=0;i<prev.length;i++) {
		Quad.addEdge(prev[i],q.prevEdge(i).which_succ(),newquad,i);
	    }
      	    for(int j=0;j<next.length;j++) {
		Quad.addEdge(newquad,j,next[j],q.nextEdge(j).which_pred());
	    }
	}
	
	public void visit(HEADER q) {
	    //Do nothing
	}

	public void visit(FOOTER q) {
	    //Do nothing
	}

	// All of these redefined to avoid error messages!
	public void visit(harpoon.IR.Quads.AGET q)    {visit((Quad)q);}
	
	public void visit(harpoon.IR.Quads.ASET q)    {visit((Quad)q);}
	
	public void visit(harpoon.IR.Quads.GET q)     {visit((Quad)q);}

	public void visit(harpoon.IR.Quads.HANDLER q) {visit((Quad)q);}

	public void visit(harpoon.IR.Quads.OPER q)    {visit((Quad)q);}

	public void visit(harpoon.IR.Quads.SET q)     {visit((Quad)q);}




	public void visit(CJMP q) {
	    int arity=q.arity();
	    Temp[] nothing=new Temp[0];
	    CJMP newsigma=new CJMP(q.getFactory(), q, ssitossamap.tempMap(q.test()), nothing);
	    Quad []prev=q.prev();
	    Quad []next=q.next();
	    for(int i=0;i<prev.length;i++) {
		Quad.addEdge(prev[i],q.prevEdge(i).which_succ(),newsigma,i);
	    }
      	    for(int j=0;j<next.length;j++) {
		Quad.addEdge(newsigma,j,next[j],q.nextEdge(j).which_pred());
	    }
	}

	public void visit(CALL q) {
	    int arity=q.arity();
	    Temp[] nparams=new Temp[q.paramsLength()];
	    for (int i=0; i<nparams.length; i++)
		nparams[i] = ssitossamap.tempMap(q.params(i));
	    CALL newcall=new CALL(q.getFactory(), q, q.method(),
				  nparams, ssitossamap.tempMap(q.retval()),
				  ssitossamap.tempMap(q.retex()),
				  q.isVirtual(), q.isTailCall(), new Temp[0]);
	    Quad []prev=q.prev();
	    Quad []next=q.next();
	    for(int i=0;i<prev.length;i++) {
		Quad.addEdge(prev[i],q.prevEdge(i).which_succ(),newcall,i);
	    }
      	    for(int j=0;j<next.length;j++) {
		Quad.addEdge(newcall,j,next[j],q.nextEdge(j).which_pred());
	    }
	}

	public void visit(PCALL q) {
	    int arity=q.arity();
	    Temp[] nparams=new Temp[q.paramsLength()];
	    for (int i=0; i<nparams.length; i++)
		nparams[i] = ssitossamap.tempMap(q.params(i));
	    PCALL newcall=new PCALL((LowQuadFactory)q.getFactory(), q, q.ptr(),
				  nparams, ssitossamap.tempMap(q.retval()),
				  ssitossamap.tempMap(q.retex()),
				  new Temp[0], q.isVirtual(), q.isTailCall());
	    Quad []prev=q.prev();
	    Quad []next=q.next();
	    for(int i=0;i<prev.length;i++) {
		Quad.addEdge(prev[i],q.prevEdge(i).which_succ(),newcall,i);
	    }
      	    for(int j=0;j<next.length;j++) {
		Quad.addEdge(newcall,j,next[j],q.nextEdge(j).which_pred());
	    }
	}

	public void visit(SWITCH q) {
	    int arity=q.arity();
	    Temp[] nothing=new Temp[0];
	    SWITCH newsigma=new SWITCH(q.getFactory(), q,ssitossamap.tempMap(q.index()), q.keys(),nothing);
	    Quad []prev=q.prev();
	    Quad []next=q.next();
	    for(int i=0;i<prev.length;i++) {
		Quad.addEdge(prev[i],q.prevEdge(i).which_succ(),newsigma,i);
	    }
      	    for(int j=0;j<next.length;j++) {
		Quad.addEdge(newsigma,j,next[j],q.nextEdge(j).which_pred());
	    }
	}

	public void visit(PHI q) {
	    //create list of phi temps
	    int numberofphis=q.numPhis();
	    int numberofssa=0;
	    for (int i=0;i<numberofphis;i++) {
		Temp check=ssitossamap.tempMap(q.src(i,0));
		for (int j=1;j<q.arity();j++) {
		    if (ssitossamap.tempMap(q.src(i,j))!=check) {
			numberofssa++;
			break;
		    }
		}
	    }
	    Temp[] dst=new Temp[numberofssa];
	    Temp[][] src=new Temp[numberofssa][q.arity()];
	    numberofssa=0;
	    for (int i=0;i<numberofphis;i++) {
		Temp check=ssitossamap.tempMap(q.src(i,0));
		for (int j=1;j<q.arity();j++) {
		    if (ssitossamap.tempMap(q.src(i,j))!=check) {
			dst[numberofssa]=q.dst(i);
			for (int k=0;k<q.arity();k++) {
			    src[numberofssa][k]=ssitossamap.tempMap(q.src(i,k));
			}
			numberofssa++;
			break;
		    }
		}
	    }
	    PHI newphi=new PHI(q.getFactory(),q,dst,src,q.arity());
	    Quad []prev=q.prev();
	    Quad []next=q.next();
	    for(int i=0;i<prev.length;i++) {
		Quad.addEdge(prev[i],q.prevEdge(i).which_succ(),newphi,i);
	    }
      	    for(int j=0;j<next.length;j++) {
		Quad.addEdge(newphi,j,next[j],q.nextEdge(j).which_pred());
	    }
	}
	TempMap ssitossamap;
    }
}






