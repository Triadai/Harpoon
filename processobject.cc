#include "processobject.h"
#include "processabstract.h"
#include "omodel.h"
#include "Hashtable.h"
#include "element.h"
#include "model.h"
#include "dmodel.h"
#include "set.h"
#include "Relation.h"
#include "repair.h"


processobject::processobject(model *m) {
  globalmodel=m;
  repair=m->getrepair();
}

bool processobject::processconstraint(Constraint *c) {
  State *st=new State(c,globalmodel->gethashtable());
  bool clean=true;
  if (st->initializestate(globalmodel)) {
    while(true) {
      if (c->getstatement()!=NULL) {
	if (processstatement(c->getstatement(),st->env)!=PTRUE) {
	  printf("ERROR: Predicate violation\n");
	  repair->repairconstraint(c,this,st->env);
	  clean=false;
	}
      }
      if (!st->increment(globalmodel))
	break; /* done */
    }
  }
  delete(st);
  return clean;
}

processobject::~processobject() {
}

int processobject::processstatement(Statement *s, Hashtable *env) {
  switch (s->gettype()) {
  case STATEMENT_OR: {
    int l=processstatement(s->getleft(),env);
    int r=processstatement(s->getright(),env);
    if (l==PFAIL&&(r==PFAIL||r==PFALSE)) return PFAIL;
    if ((l==PFAIL||l==PFALSE)&&r==PFAIL) return PFAIL;
    return l||r;
  }
  case STATEMENT_AND: {
    int l=processstatement(s->getleft(),env);
    int r=processstatement(s->getright(),env);
    if (l==PFAIL&&(r==PFAIL||r==PTRUE)) return PFAIL;
    if (r==PFAIL&&(l==PFAIL||l==PTRUE)) return PFAIL;
    return l&&r;
  }
  case STATEMENT_NOT: {
    int l=processstatement(s->getleft(),env);
    if (l==PFAIL) return PFAIL;
    return !l;
  }
  case STATEMENT_PRED:
    return processpredicate(s->getpredicate(),env);
  }
}

int processobject::processpredicate(Predicate *p, Hashtable *env) {
  switch(p->gettype()) {
  case PREDICATE_LT: {
    Element *left=evaluatevalueexpr(p->getvalueexpr(),env,globalmodel);
    Element *right=evaluateexpr(p->geteleexpr(),env,globalmodel);
    if (right==NULL) {
      return PFAIL;
    }
    if (left==NULL) {
      delete(right);
      return false;
    }
    int t=left->intvalue()<right->intvalue();
    delete(right);
    return t;
  }
  case PREDICATE_LTE: {
    Element *left=evaluatevalueexpr(p->getvalueexpr(),env,globalmodel);
    Element *right=evaluateexpr(p->geteleexpr(),env,globalmodel);
    if (right==NULL) {return PFAIL;}
    if (left==NULL) {
      delete(right);
      return false;
    }
    bool t=left->intvalue()<=right->intvalue();
    delete(right);
    return t;
  }
  case PREDICATE_EQUALS: {
    Element *left=evaluatevalueexpr(p->getvalueexpr(),env,globalmodel);
    Element *right=evaluateexpr(p->geteleexpr(),env,globalmodel);
    if (right==NULL) {return PFAIL;}
    if (left==NULL) {
      delete(right);
      return false;
    }
    /* Can have more than just int's here */
    bool t=left->equals(right); /*Just ask the equals method*/
    delete(right);
    return t;
  }
  case PREDICATE_GTE: {
    Element *left=evaluatevalueexpr(p->getvalueexpr(),env,globalmodel);
    Element *right=evaluateexpr(p->geteleexpr(),env,globalmodel);
    if (right==NULL) {return PFAIL;}
    if (left==NULL) {
      delete(right);
      return false;
    }
    bool t=left->intvalue()>=right->intvalue();
    delete(right);
    return t;
  }
  case PREDICATE_GT: {
    Element *left=evaluatevalueexpr(p->getvalueexpr(),env,globalmodel);
    Element *right=evaluateexpr(p->geteleexpr(),env,globalmodel);
    if (right==NULL) {return PFAIL;}
    if (left==NULL) {
      delete(right);
      return false;
    }
    bool t=left->intvalue()>right->intvalue();
    delete(right);
    return t;
  }
  case PREDICATE_SET: {
    Label *label=p->getlabel();
    Setexpr * setexpr=p->getsetexpr();
    Element *labelele=(Element *) env->get(label->label());
    switch(setexpr->gettype()) {
      case SETEXPR_LABEL:
	return globalmodel->getdomainrelation()->getset(setexpr->getsetlabel()->getname())->getset()->contains(labelele);
      case SETEXPR_REL:
	return globalmodel->getdomainrelation()->getrelation(setexpr->getrelation()->getname())->getrelation()->getset(setexpr->getlabel()->label())->contains(labelele);
      case SETEXPR_INVREL:
	return globalmodel->getdomainrelation()->getrelation(setexpr->getrelation()->getname())->getrelation()->invgetset(setexpr->getlabel()->label())->contains(labelele);
    }
  }
  case PREDICATE_EQ1:
  case PREDICATE_GTE1: {
    int setsize;
    Setexpr * setexpr=p->getsetexpr();
    switch(setexpr->gettype()) {
    case SETEXPR_LABEL:
      setsize=globalmodel->getdomainrelation()->getset(setexpr->getsetlabel()->getname())->getset()->size();
      break;
    case SETEXPR_REL: {
      Element *key=(Element *)env->get(setexpr->getlabel()->label());
      WorkSet *ws=globalmodel->getdomainrelation()->getrelation(setexpr->getrelation()->getname())->getrelation()->getset(key);
      if (ws!=NULL)
	setsize=ws->size();
      else
	setsize=0;
      break;
    }
    case SETEXPR_INVREL: {
      Element *key=(Element *)env->get(setexpr->getlabel()->label());
      WorkSet *ws=globalmodel->getdomainrelation()->getrelation(setexpr->getrelation()->getname())->getrelation()->invgetset(key);
      if (ws!=NULL)
	setsize=ws->size();
      else
	setsize=0;
      break;
    }
    }
    return ((p->gettype()==PREDICATE_EQ1)&&(setsize==1))||
      ((p->gettype()==PREDICATE_GTE1)&&(setsize>=1));
  }
  }
}


Element * evaluatevalueexpr(Valueexpr *ve, Hashtable *env, model *m) {
  Element * e=(Element *) env->get(ve->getlabel()->label());
  return (Element *)m->getdomainrelation()->getrelation(ve->getrelation()->getname())->getrelation()->getobj(e);
}

Element * evaluateexpr(Elementexpr *ee, Hashtable *env, model *m) {
  switch(ee->gettype()) {
  case ELEMENTEXPR_LABEL: {
    return new Element((Element *)env->get(ee->getlabel()->label()));
  }
  case ELEMENTEXPR_SUB: {
    Elementexpr *left=ee->getleft();
    Elementexpr *right=ee->getright();
    Element *leftval=evaluateexpr(left,env,m);
    if(leftval==NULL) return NULL;
    Element *rightval=evaluateexpr(right,env,m);
    if(rightval==NULL) {delete(leftval);return NULL;}
    Element *diff=new Element(leftval->intvalue()-rightval->intvalue());
    delete(leftval);delete(rightval);
    return diff;
  }
  case ELEMENTEXPR_ADD: {
    Elementexpr *left=ee->getleft();
    Elementexpr *right=ee->getright();
    Element *leftval=evaluateexpr(left,env,m);
    if(leftval==NULL) return NULL;
    Element *rightval=evaluateexpr(right,env,m);
    if(rightval==NULL) {delete(leftval);return NULL;}
    Element *sum=new Element(leftval->intvalue()+rightval->intvalue());
    delete(leftval);delete(rightval);
    return sum;
  }
  case ELEMENTEXPR_RELATION: {
    Elementexpr *left=ee->getleft();
    Relation *rel=ee->getrelation();
    Element *leftval=evaluateexpr(left,env,m);
    if(leftval==NULL) return NULL;
    Element *retval=(Element *)m->getdomainrelation()->getrelation(rel->getname())->getrelation()->getobj(leftval);
    delete(leftval);
    return new Element(retval);
  }
  case ELEMENTEXPR_MULT: {
    Elementexpr *left=ee->getleft();
    Elementexpr *right=ee->getright();
    Element *leftval=evaluateexpr(left,env,m);
    if(leftval==NULL) return NULL;
    Element *rightval=evaluateexpr(right,env,m);
    if(rightval==NULL) {delete(leftval);return NULL;}
    Element *diff=new Element(leftval->intvalue()*rightval->intvalue());
    delete(leftval);delete(rightval);
    return diff;
  }
  case ELEMENTEXPR_LIT: {
    Literal *l=ee->getliteral();
    switch(l->gettype()) {
    case LITERAL_NUMBER:
      return new Element(l->number());
    case LITERAL_TOKEN:
      return new Element(copystr(l->token()));
    default:
      printf("ERROR with lit type\n");
      exit(-1);
    }
  }
  /*  case ELEMENTEXPR_PARAM: {
    Element *ele=evaluateexpr(ee->getleft(),env,m);
    Element *eec=ele->paramvalue(ee->getliteral()->number());
    Element *retval=new Element(eec);
    delete(ele);
    return eec;
    }*/ //NO OBJECT PARAMETERS
  case ELEMENTEXPR_SETSIZE:
    Setexpr * setexpr=ee->getsetexpr();
    switch(setexpr->gettype()) {
    case SETEXPR_LABEL:
      return new Element(m->getdomainrelation()->getset(setexpr->getsetlabel()->getname())->getset()->size());
    case SETEXPR_REL: {
      Element *key=(Element *)env->get(setexpr->getlabel()->label());
      WorkSet *ws=m->getdomainrelation()->getrelation(setexpr->getrelation()->getname())->getrelation()->getset(key);
      if (ws==NULL)
	return new Element(0);
      else
	return new Element(ws->size());
    }
    case SETEXPR_INVREL: {
      Element *key=(Element *)env->get(setexpr->getlabel()->label());
      WorkSet *ws=m->getdomainrelation()->getrelation(setexpr->getrelation()->getname())->getrelation()->invgetset(key);
      if (ws==NULL)
	return new Element(0);
      else
	return new Element(ws->size());
    }
    }
    break;
  }
}