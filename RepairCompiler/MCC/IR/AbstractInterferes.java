package MCC.IR;
import java.util.*;

class AbstractInterferes {
    static public boolean interferes(AbstractRepair ar, Rule r, boolean satisfy) {
	boolean mayadd=false;
	boolean mayremove=false;
	switch (ar.getType()) {
	case AbstractRepair.ADDTOSET:
	case AbstractRepair.ADDTORELATION:
	    if (interferesquantifier(ar.getDescriptor(), true, r, satisfy))
		return true;
	    mayadd=true;
	    break;
	case AbstractRepair.REMOVEFROMSET:
	case AbstractRepair.REMOVEFROMRELATION:
	    if (interferesquantifier(ar.getDescriptor(), false, r, satisfy))
		return true;
	    mayremove=true;
	    break;
	case AbstractRepair.MODIFYRELATION:
	    if (interferesquantifier(ar.getDescriptor(), true, r, satisfy))
		return true;
	    if (interferesquantifier(ar.getDescriptor(), false, r, satisfy))
		return true;
	    mayadd=true;
	    mayremove=true;
	break;
	default:
	    throw new Error("Unrecognized Abstract Repair");
	}
	DNFRule drule=null;
	if (satisfy)
	    drule=r.getDNFGuardExpr();
	else
	    drule=r.getDNFNegGuardExpr();
	
	for(int i=0;i<drule.size();i++) {
	    RuleConjunction rconj=drule.get(i);
	    for(int j=0;j<rconj.size();j++) {
		DNFExpr dexpr=rconj.get(j);
		Expr expr=dexpr.getExpr();
		if (expr.usesDescriptor(ar.getDescriptor())) {
		    /* Need to check */
		    if ((mayadd&&!dexpr.getNegation())||(mayremove&&dexpr.getNegation()))
			return true;
		}
	    }
	}
	return false;
    }


    static public boolean interferes(AbstractRepair ar, DNFPredicate dp) {
	if ((ar.getDescriptor()!=dp.getPredicate().getDescriptor()) &&
	    ((ar.getDescriptor() instanceof SetDescriptor)||
	     !dp.getPredicate().usesDescriptor((RelationDescriptor)ar.getDescriptor())))
	    return false;

	/* This if handles all the c comparisons in the paper */
	if (ar.getDescriptor()==dp.getPredicate().getDescriptor()&&
	    (ar.getType()==AbstractRepair.ADDTOSET||ar.getType()==AbstractRepair.ADDTORELATION)&&
	    (ar.getPredicate().getPredicate() instanceof ExprPredicate)&&
	    (dp.getPredicate() instanceof ExprPredicate)&&
	    (dp.getPredicate().inverted()==ar.getPredicate().getPredicate().inverted())&&
	    (((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
	    boolean neg1=ar.getPredicate().isNegated();
	    Opcode op1=((ExprPredicate)ar.getPredicate().getPredicate()).getOp();
	    int size1=((ExprPredicate)ar.getPredicate().getPredicate()).leftsize();
	    boolean neg2=dp.isNegated();
	    Opcode op2=((ExprPredicate)dp.getPredicate()).getOp();
	    int size2=((ExprPredicate)dp.getPredicate()).leftsize();
	    if ((!neg1&&((op1==Opcode.EQ)||(op1==Opcode.NE)||(op1==Opcode.GT)||op1==Opcode.GE))||
		(neg1&&((op1==Opcode.EQ)||(op1==Opcode.NE)||(op1==Opcode.LT)||op1==Opcode.LE))) {
		int size1a=0;
		if (!neg1) {
		    if((op1==Opcode.EQ)||(op1==Opcode.GE))
			size1a=size1;
		    if((op1==Opcode.GT)||(op1==Opcode.NE))
			size1a=size1+1;
		}
		if (neg1) {
		    if((op1==Opcode.EQ)||(op1==Opcode.LE))
			size1a=size1+1;
		    if((op1==Opcode.LT)||(op1==Opcode.NE))
			size1a=size1;
		}
		if ((!neg2&&(op2==Opcode.EQ)&&(size1a==size2))||
		    (neg2&&(op2==Opcode.EQ)&&(size1a!=size2))||
		    (!neg2&&(op2==Opcode.NE)&&(size1a!=size2))||
		    (neg2&&(op2==Opcode.NE)&&(size1a==size2))||
		    (!neg2&&(op2==Opcode.GE))||
		    (!neg2&&(op2==Opcode.GT))||
		    (neg2&&(op2==Opcode.LE))||
		    (neg2&&(op2==Opcode.LT))||
   		    (neg2&&(op2==Opcode.GE)&&(size1a<size2))||
		    (neg2&&(op2==Opcode.GT)&&(size1a<=size2))||
		    (!neg2&&(op2==Opcode.LE)&&(size1a<=size2))||
		    (!neg2&&(op2==Opcode.LT)&&(size1a<size2)))
		    return false;
	    } 
	}

	if (ar.getDescriptor()==dp.getPredicate().getDescriptor()&&
	    (ar.getType()==AbstractRepair.ADDTOSET||ar.getType()==AbstractRepair.ADDTORELATION)&&
	    (dp.getPredicate() instanceof ExprPredicate)&&
	    (((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
	    boolean neg2=dp.isNegated();
	    Opcode op2=((ExprPredicate)dp.getPredicate()).getOp();
	    int size2=((ExprPredicate)dp.getPredicate()).leftsize();
	    if ((!neg2&&(op2==Opcode.EQ)&&(size2==0))||
		(neg2&&(op2==Opcode.NE)&&(size2==0))||
		(!neg2&&(op2==Opcode.GE))||
		(!neg2&&(op2==Opcode.GT))||
		(neg2&&(op2==Opcode.LE))||
		(neg2&&(op2==Opcode.LT)))
		return false;
	}

	/* This handles all the c comparisons in the paper */
	if (ar.getDescriptor()==dp.getPredicate().getDescriptor()&&
	    (ar.getType()==AbstractRepair.REMOVEFROMSET||ar.getType()==AbstractRepair.REMOVEFROMRELATION)&&
	    (ar.getPredicate().getPredicate() instanceof ExprPredicate)&&
	    (dp.getPredicate() instanceof ExprPredicate)&&
	    (dp.getPredicate().inverted()==ar.getPredicate().getPredicate().inverted())&&
	    (((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
	    boolean neg1=ar.getPredicate().isNegated();
	    Opcode op1=((ExprPredicate)ar.getPredicate().getPredicate()).getOp();
	    int size1=((ExprPredicate)ar.getPredicate().getPredicate()).leftsize();
	    boolean neg2=dp.isNegated();
	    Opcode op2=((ExprPredicate)dp.getPredicate()).getOp();
	    int size2=((ExprPredicate)dp.getPredicate()).leftsize();
	    if ((!neg1&&((op1==Opcode.EQ)||(op1==Opcode.LT)||op1==Opcode.LE)||(op1==Opcode.NE))||
		(neg1&&((op1==Opcode.EQ)||(op1==Opcode.GT)||op1==Opcode.GE)||(op1==Opcode.NE))) {
		int size1a=0;
		if (neg1) {
		    if((op1==Opcode.EQ)||(op1==Opcode.GE))
			size1a=size1-1;
		    if((op1==Opcode.GT)||(op1==Opcode.NE))
			size1a=size1;
		}
		if (!neg1) {
		    if((op1==Opcode.EQ)||(op1==Opcode.LE))
			size1a=size1;
		    if((op1==Opcode.LT)||(op1==Opcode.NE))
			size1a=size1-1;
		}
		if ((!neg2&&(op2==Opcode.EQ)&&(size1a==size2))||
		    (neg2&&(op2==Opcode.EQ)&&(size1a!=size2))||
		    (!neg2&&(op2==Opcode.NE)&&(size1a!=size2))||
		    (neg2&&(op2==Opcode.NE)&&(size1a==size2))||
		    (neg2&&(op2==Opcode.GE))||
		    (neg2&&(op2==Opcode.GT))||
		    (!neg2&&(op2==Opcode.LE))||
		    (!neg2&&(op2==Opcode.LT))||
   		    (!neg2&&(op2==Opcode.GE)&&(size1a>=size2))||
		    (!neg2&&(op2==Opcode.GT)&&(size1a>size2))||
		    (neg2&&(op2==Opcode.LE)&&(size1a>size2))||
		    (neg2&&(op2==Opcode.LT)&&(size1a>=size2)))
		    return false;
	    }
	}

	if (ar.getDescriptor()==dp.getPredicate().getDescriptor()&&
	    (ar.getType()==AbstractRepair.REMOVEFROMSET||ar.getType()==AbstractRepair.REMOVEFROMRELATION)&&
	    (dp.getPredicate() instanceof ExprPredicate)&&
	    (((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
	    boolean neg2=dp.isNegated();
	    Opcode op2=((ExprPredicate)dp.getPredicate()).getOp();
	    int size2=((ExprPredicate)dp.getPredicate()).leftsize();
	    if ((!neg2&&(op2==Opcode.EQ)&&(size2==0))||
		(neg2&&(op2==Opcode.NE)&&(size2==0))||
		(neg2&&(op2==Opcode.GE))||
		(neg2&&(op2==Opcode.GT))||
		(!neg2&&(op2==Opcode.LE))||
		(!neg2&&(op2==Opcode.LT)))
		return false;
	    
	}
	if ((ar.getDescriptor()==dp.getPredicate().getDescriptor())&&
	    (ar.getType()==AbstractRepair.ADDTOSET||ar.getType()==AbstractRepair.ADDTORELATION)&&
	    (dp.getPredicate() instanceof InclusionPredicate)&&
	    (dp.isNegated()==false))
	    return false; /* Could only satisfy this predicate */

	if ((ar.getDescriptor()==dp.getPredicate().getDescriptor())&&
	    (ar.getType()==AbstractRepair.REMOVEFROMSET||ar.getType()==AbstractRepair.REMOVEFROMRELATION)&&
	    (dp.getPredicate() instanceof InclusionPredicate)&&
	    (dp.isNegated()==true))
	    return false; /* Could only satisfy this predicate */
	  
	return true;
    }

    static public boolean interferes(ScopeNode sn, DNFPredicate dp) {
	if (!sn.getSatisfy()&&(sn.getDescriptor() instanceof SetDescriptor)) {
	    Rule r=sn.getRule();
	    Set target=r.getInclusion().getTargetDescriptors();
	    boolean match=false;
	    for(int i=0;i<r.numQuantifiers();i++) {
		Quantifier q=r.getQuantifier(i);
		if (q instanceof SetQuantifier) {
		    SetQuantifier sq=(SetQuantifier) q;
		    if (target.contains(sq.getSet())) {
			match=true;
			break;
		    }
		}
	    }
	    if (match&&
		sn.getDescriptor()==dp.getPredicate().getDescriptor()&&
		(dp.getPredicate() instanceof ExprPredicate)&&
		(((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
		boolean neg=dp.isNegated();
		Opcode op=((ExprPredicate)dp.getPredicate()).getOp();
		int size=((ExprPredicate)dp.getPredicate()).leftsize();
		if (neg) {
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
		if ((op==Opcode.GE)&&
		    ((size==0)||(size==1)))
		    return false;
		if ((op==Opcode.GT)&&
		    (size==0))
		    return false;
	    }
	}
	return interferes(sn.getDescriptor(), sn.getSatisfy(),dp);
    }

    static public boolean interferes(Descriptor des, boolean satisfy, DNFPredicate dp) {
	if ((des!=dp.getPredicate().getDescriptor()) &&
	    ((des instanceof SetDescriptor)||
	     !dp.getPredicate().usesDescriptor((RelationDescriptor)des)))
	    return false;

	/* This if handles all the c comparisons in the paper */
	if (des==dp.getPredicate().getDescriptor()&&
	    (satisfy)&&
	    (dp.getPredicate() instanceof ExprPredicate)&&
	    (((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
	    boolean neg2=dp.isNegated();
	    Opcode op2=((ExprPredicate)dp.getPredicate()).getOp();
	    int size2=((ExprPredicate)dp.getPredicate()).leftsize();
	    {
		if ((!neg2&&(op2==Opcode.GE))||
		    (!neg2&&(op2==Opcode.GT))||
		    (neg2&&(op2==Opcode.EQ)&&(size2==0))||
		    (!neg2&&(op2==Opcode.NE)&&(size2==0))||
		    (neg2&&(op2==Opcode.LE))||
		    (neg2&&(op2==Opcode.LT)))
		    return false;
	    }
	}
	/* This if handles all the c comparisons in the paper */
	if (des==dp.getPredicate().getDescriptor()&&
	    (!satisfy)&&
	    (dp.getPredicate() instanceof ExprPredicate)&&
	    (((ExprPredicate)dp.getPredicate()).getType()==ExprPredicate.SIZE)) {
	    boolean neg2=dp.isNegated();
	    Opcode op2=((ExprPredicate)dp.getPredicate()).getOp();
	    int size2=((ExprPredicate)dp.getPredicate()).leftsize();
	    {
		if ((neg2&&(op2==Opcode.GE))||
		    (neg2&&(op2==Opcode.GT))||
		    (!neg2&&(op2==Opcode.EQ)&&(size2==0))||
		    (neg2&&(op2==Opcode.NE)&&(size2==0))||
		    (!neg2&&(op2==Opcode.LE))||
		    (!neg2&&(op2==Opcode.LT)))
		    return false;
	    } 
	}
	if ((des==dp.getPredicate().getDescriptor())&&
	    satisfy&&
	    (dp.getPredicate() instanceof InclusionPredicate)&&
	    (dp.isNegated()==false))
	    return false; /* Could only satisfy this predicate */

	if ((des==dp.getPredicate().getDescriptor())&&
	    (!satisfy)&&
	    (dp.getPredicate() instanceof InclusionPredicate)&&
	    (dp.isNegated()==true))
	    return false; /* Could only satisfy this predicate */

	return true;
    }

    static public boolean interferesquantifier(Descriptor des, boolean satisfy, Quantifiers r, boolean satisfyrule) {
	for(int i=0;i<r.numQuantifiers();i++) {
	    Quantifier q=r.getQuantifier(i);
	    if (q instanceof RelationQuantifier||q instanceof SetQuantifier) {
		if (q.getRequiredDescriptors().contains(des)&&(satisfy==satisfyrule))
		    return true;
	    } else if (q instanceof ForQuantifier) {
		if (q.getRequiredDescriptors().contains(des))
		    return true;
	    } else throw new Error("Unrecognized Quantifier");
	}
	return false;
    }

    static public boolean interferes(AbstractRepair ar, Quantifiers q) {
	if (ar.getType()==AbstractRepair.ADDTOSET||ar.getType()==AbstractRepair.ADDTORELATION)
	    return interferesquantifier(ar.getDescriptor(),true,q,true);
	return false;
    }

    static public boolean interferes(Descriptor des, boolean satisfy, Quantifiers q) {
	return interferesquantifier(des, satisfy, q,true);
    }

    static public boolean interferes(Descriptor des, boolean satisfy, Rule r, boolean satisfyrule) {
	if (interferesquantifier(des,satisfy,r,satisfyrule))
	    return true;
	/* Scan DNF form */
	DNFRule drule=r.getDNFGuardExpr();
	for(int i=0;i<drule.size();i++) {
	    RuleConjunction rconj=drule.get(i);
	    for(int j=0;j<rconj.size();j++) {
		DNFExpr dexpr=rconj.get(j);
		Expr expr=dexpr.getExpr();
		boolean negated=dexpr.getNegation();
		/*
		  satisfy  negated
		  Yes      No             Yes
		  Yes      Yes            No
		  No       No             No
		  No       Yes            Yes
		*/
		boolean satisfiesrule=(satisfy^negated);/*XOR of these */
		if (satisfiesrule==satisfyrule) {
		    /* Effect is the one being tested for */
		    /* Only expr's to be concerned with are TupleOfExpr and
		       ElementOfExpr */
		    if (expr.getRequiredDescriptors().contains(des)) {
			if (((expr instanceof ElementOfExpr)||
			    (expr instanceof TupleOfExpr))&&
			    (expr.getRequiredDescriptors().size()==1))
			    return true;
			else
			    throw new Error("Unrecognized EXPR");
		    }
		}
	    }
	}
	return false;
    }
}
