package MCC.IR;

import java.util.*;

public class OpExpr extends Expr {

    Expr left;
    Expr right;
    Opcode opcode;

    public void findmatch(Descriptor d, Set  s) {
	left.findmatch(d,s);
	if (right!=null)
	    right.findmatch(d,s);
    }

    public static boolean isInt(Expr e) {
	if ((e instanceof IntegerLiteralExpr)||
	    ((e instanceof OpExpr)&&(((OpExpr)e).getLeftExpr() instanceof IntegerLiteralExpr)))
	    return true;
	return false;
    }

    public static int getInt(Expr e) {
	if (e instanceof IntegerLiteralExpr)
	    return ((IntegerLiteralExpr)e).getValue();
	else if ((e instanceof OpExpr) && (((OpExpr)e).getLeftExpr() instanceof IntegerLiteralExpr))
	    return ((IntegerLiteralExpr)((OpExpr)e).getLeftExpr()).getValue();
	else throw new Error();
    }

    public OpExpr(Opcode opcode, Expr left, Expr right) {
	if ((isInt(left)&&isInt(right))||
	    (isInt(left)&&(opcode==Opcode.NOT))||
	    (isInt(left)&&(opcode==Opcode.RND))) {
	    this.opcode=Opcode.NOP;
	    this.right=null;
	    int lint=getInt(left);
	    int rint=getInt(right);
	    int value=0;
	    if (opcode==Opcode.ADD) {
		value=lint+rint;
	    } else if (opcode==Opcode.SUB) {
		value=lint-rint;
	    } else if (opcode==Opcode.SHL) {
		value=lint<<rint;
	    } else if (opcode==Opcode.SHR) {
		value=lint>>rint;
	    } else if (opcode==Opcode.MULT) {
		value=lint*rint;
	    } else if (opcode==Opcode.DIV) {
		value=lint/rint;
	    } else if (opcode==Opcode.GT) {
		if (lint>rint)
		    value=1;
	    } else if (opcode==Opcode.GE) {
		if (lint>=rint)
		    value=1;
	    } else if (opcode==Opcode.LT) {
		if (lint<rint)
		    value=1;
	    } else if (opcode==Opcode.LE) {
		if (lint<=rint)
		    value=1;
	    } else if (opcode==Opcode.EQ) {
		if (lint==rint)
		    value=1;
	    } else if (opcode==Opcode.NE) {
		if (lint!=rint)
		    value=1;
	    } else if (opcode==Opcode.AND) {
		if ((lint!=0)&&(rint!=0))
		    value=1;
	    } else if (opcode==Opcode.OR) {
		if ((lint!=0)||(rint!=0))
		    value=1;
	    } else if (opcode==Opcode.NOT) {
		if (lint==0)
		    value=1;
	    } else if (opcode==Opcode.RND) {
		value=((lint>>3)<<3);
		if ((lint % 8)!=0)
		    value+=8;
	    } else throw new Error("Unrecognized Opcode");
	    this.left=new IntegerLiteralExpr(value);
	    } else {
	    this.opcode = opcode;
	    this.left = left;
	    this.right = right;
	    assert (right == null && (opcode == Opcode.NOT||opcode==Opcode.RND)) || (right != null);
	}
    }

    public Expr getRightExpr() {
	return right;
    }

    public Expr getLeftExpr() {
	return left;
    }

    public Set freeVars() {
	Set lset=left.freeVars();
	Set rset=null;
	if (right!=null)
	    rset=right.freeVars();
	if (lset==null)
	    return rset;
	if (rset!=null)
	    lset.addAll(rset);
	return lset;
    }

    public String name() {
	if (opcode==Opcode.NOT)
	    return "!("+left.name()+")";
	if (opcode==Opcode.NOP)
	    return left.name();
	if (opcode==Opcode.RND)
	    return "Round("+left.name()+")";
	String name=left.name()+opcode.toString();
	if (right!=null)
	    name+=right.name();
	return name;
    }

    public Opcode getOpcode() {
	return opcode;
    }




    public boolean equals(Map remap, Expr e) {
	if (e==null||!(e instanceof OpExpr))
	    return false;
	OpExpr oe=(OpExpr)e;
	if (opcode!=oe.opcode)
	    return false;
	if (!left.equals(remap,oe.left))
	    return false;
	if ((opcode!=Opcode.NOT)&&(opcode!=Opcode.RND)&&(opcode!=Opcode.NOP))
	    if (!right.equals(remap,oe.right))
		return false;
	return true;
    }

    public DNFRule constructDNF() {
        if (opcode==Opcode.AND) {
            DNFRule leftd=left.constructDNF();
            DNFRule rightd=right.constructDNF();
            return leftd.and(rightd);
        } else if (opcode==Opcode.OR) {
            DNFRule leftd=left.constructDNF();
            DNFRule rightd=right.constructDNF();
            return leftd.or(rightd);
        } else if (opcode==Opcode.NOT) {
            DNFRule leftd=left.constructDNF();
            return leftd.not();
        } else return new DNFRule(this);
    }

    public boolean usesDescriptor(Descriptor d) {
	if (opcode==Opcode.GT||opcode==Opcode.GE||opcode==Opcode.LT||
	    opcode==Opcode.LE||opcode==Opcode.EQ||opcode==Opcode.NE)
	    return left.usesDescriptor(d)||(right!=null&&right.usesDescriptor(d));
	    //	    return right.usesDescriptor(d);
	else
	    return left.usesDescriptor(d)||(right!=null&&right.usesDescriptor(d));
    }
    

    public int[] getRepairs(boolean negated) {
	if (left instanceof RelationExpr)
	    return new int[] {AbstractRepair.MODIFYRELATION};
	if (left instanceof SizeofExpr) {
	    Opcode op=opcode;
	    if (negated) {
		/* remove negation through opcode translation */
		if (op==Opcode.GT)
		    op=Opcode.LE;
		else if (op==Opcode.GE)
		    op=Opcode.LT;
		else if (op==Opcode.EQ)
		    op=Opcode.NE;
		else if (op==Opcode.NE)
		    op=Opcode.EQ;
		else if (op==Opcode.LT)
		    op=Opcode.GE;
		else if (op==Opcode.LE)
		    op=Opcode.GT;
	    }



	    boolean isRelation=((SizeofExpr)left).setexpr instanceof ImageSetExpr;
	    if (isRelation) {
		if (op==Opcode.EQ) {
		    if (((IntegerLiteralExpr)right).getValue()==0)
			return new int[] {AbstractRepair.REMOVEFROMRELATION};
		    else
			return new int[] {AbstractRepair.ADDTORELATION,
					  AbstractRepair.REMOVEFROMRELATION};
		} else if (op==Opcode.GE||op==Opcode.GT) {
		    return new int[]{AbstractRepair.ADDTORELATION}; 
		} else if (op==Opcode.LE||op==Opcode.LT) {
		    return new int[]{AbstractRepair.REMOVEFROMRELATION};
		} else if (op==Opcode.NE) {
		    return new int[]{AbstractRepair.ADDTORELATION};
		} else throw new Error();
	    } else {
		if (op==Opcode.EQ) {
		    if (((IntegerLiteralExpr)right).getValue()==0)
			return new int[] {AbstractRepair.REMOVEFROMSET};			
		    else
			return new int[] {AbstractRepair.ADDTOSET,
					      AbstractRepair.REMOVEFROMSET};
		} else if (op==Opcode.GE||op==Opcode.GT) {
		    return new int[] {AbstractRepair.ADDTOSET}; 
		} else if (op==Opcode.LE||op==Opcode.LT) {
		    return new int[] {AbstractRepair.REMOVEFROMSET};
		} else if (op==Opcode.NE) {
		    return new int[] {AbstractRepair.ADDTOSET};
		} else throw new Error();
	    }
	}
	throw new Error("BAD");
    }
    
    public Descriptor getDescriptor() {
	return left.getDescriptor();
    }

    public boolean inverted() {
	return left.inverted();
    }

    public Set getInversedRelations() {
        Set set = left.getInversedRelations();
        if (right != null) {
            set.addAll(right.getInversedRelations());
        }
        return set;
    }

    public Set getRequiredDescriptors() {
        Set v = left.getRequiredDescriptors();
     
        if (right != null) {
            v.addAll(right.getRequiredDescriptors());
        }

        return v;
    }   

    public void generate(CodeWriter writer, VarDescriptor dest) {
        VarDescriptor ld = VarDescriptor.makeNew("leftop");
        left.generate(writer, ld);        
        VarDescriptor rd = null;
	VarDescriptor lm=VarDescriptor.makeNew("lm");
	VarDescriptor rm=VarDescriptor.makeNew("rm");

        if (right != null) {
	    if ((opcode==Opcode.OR)||
		(opcode==Opcode.AND)) {
		writer.outputline("int "+lm.getSafeSymbol()+"=maybe;");
		writer.outputline("int maybe=0;");
	    }

            rd = VarDescriptor.makeNew("rightop");
            right.generate(writer, rd);
        }

        String code;
	if (opcode == Opcode.RND) {
	    writer.outputline("int " +dest.getSafeSymbol() + " = (" +
			      ld.getSafeSymbol() + ">>3)<<3; ");
	    writer.outputline("if ("+ld.getSafeSymbol()+" % 8) "+dest.getSafeSymbol()+"+=8;");
	} else if (opcode == Opcode.NOP) {
	    writer.outputline("int " +dest.getSafeSymbol() + " = " +
			      ld.getSafeSymbol() +"; ");
	} else if (opcode != Opcode.NOT) { /* two operands */
            assert rd != null;
            writer.outputline("int " + dest.getSafeSymbol() + " = " + 
                              ld.getSafeSymbol() + " " + opcode.toString() + " " + rd.getSafeSymbol() + ";");
        } else if (opcode == Opcode.AND) {
	    writer.outputline("int "+rm.getSafeSymbol()+"=maybe;");
	    writer.outputline("maybe = (" + ld.getSafeSymbol() + " && " + rm.getSafeSymbol() + ") || (" + rd.getSafeSymbol() + " && " + lm.getSafeSymbol() + ") || (" + lm.getSafeSymbol() + " && " + rm.getSafeSymbol() + ");");
	    writer.outputline(dest.getSafeSymbol() + " = " + ld.getSafeSymbol() + " && " + rd.getSafeSymbol() + ";");
	} else if (opcode == Opcode.OR) {
	    writer.outputline("int "+rm.getSafeSymbol()+"=maybe;");
	    writer.outputline("maybe = (!" + ld.getSafeSymbol() + " && " + rm.getSafeSymbol() + ") || (!" + rd.getSafeSymbol() +
			      " && " + lm.getSafeSymbol() + ") || (" + lm.getSafeSymbol() + " && " + rm.getSafeSymbol() + ");");
	    writer.outputline(dest.getSafeSymbol() + " = " + ld.getSafeSymbol() + " || " + rd.getSafeSymbol() +
			      ";");
	} else {
            writer.outputline("int " + dest.getSafeSymbol() + " = !" + ld.getSafeSymbol() + ";");
        }
    }

    public void prettyPrint(PrettyPrinter pp) {
        pp.output("(");
        if (opcode == Opcode.NOT) {
	    pp.output("!");
            left.prettyPrint(pp);
	} else if (opcode == Opcode.NOP) {
            left.prettyPrint(pp);
	} else if (opcode == Opcode.RND) {
	    pp.output("RND ");
            left.prettyPrint(pp);
        } else {           
            left.prettyPrint(pp);
            pp.output(" " + opcode.toString() + " ");
            assert right != null;
            right.prettyPrint(pp);
        }
        pp.output(")");
    }

    public TypeDescriptor typecheck(SemanticAnalyzer sa) {
        TypeDescriptor lt = left.typecheck(sa);
        TypeDescriptor rt = right == null ? null : right.typecheck(sa);

        if (lt == null) {
            return null;
        } else if (right != null && rt == null) {
            return null;
        }

        boolean ok = true;

        // #ATTN#: if we want node.next != literal(0) to represent a null check than we need to allow ptr arithmetic
        // either that or we use a isvalid clause to check for null

        /*
        if (lt != ReservedTypeDescriptor.INT) {
            sa.getErrorReporter().report(null, "Left hand side of expression is of type '" + lt.getSymbol() + "' but must be type 'int'");
            ok = false;
        }

        if (right != null) {
            if (rt != ReservedTypeDescriptor.INT) {
                sa.getErrorReporter().report(null, "Right hand side of expression is of type '" + rt.getSymbol() + "' but must be type 'int'");
                ok = false;
            }
        }
        */

        if (!ok) {
            return null;
        }

        this.td = ReservedTypeDescriptor.INT;
        return this.td;
    }

}





