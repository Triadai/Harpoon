
/******************************************************************************/
/*                                                                            */
/*  Copyright (c) 1996 University of California at Santa Barbara              */
/*                                                                            */
/*  All rights reserved.                                                      */
/*                                                                            */
/*  Permission is given to use, copy, and modify this software for any        */
/*  non-commercial purpose as long as this copyright notice is not removed    */
/*  All other uses, including redistribution in whole or in part, are         */
/*  forbidden without prior written permission.                               */
/*                                                                            */
/*  This software is provided with absolutely no warranty and no support.     */
/*                                                                            */
/*  For further information contact: pedro@cs.ucsb.edu or martin@cs.ucsb.edu  */
/*                                                                            */
/******************************************************************************/

import java.io.*;
import java.lang.Math.*;

class C { 
  static final double ONE 	= (double) 1.00;
  static final double TWO	= (double) 2.00;
  static final double THREE	= (double) 3.00;
  static final double FIVE	= (double) 5.00;
  static final double ROH = 0.9572;
  static final double ROHI = (1.00/ROH);
  static final double ROHI2 = (ROHI*ROHI);
  static final double ANGLE = 1.824218;
  static final double OMAS = 15.99945;
  static final double HMAS = 1.007825;
  static final double WTMOL = (OMAS+2.00*HMAS);
/*.....UNITS USeD TO SCALe VARIABLeS (IN C.G.S.) */
  static final double UNITT = (1.0e-15);
  static final double UNITL = (1.0e-8);
  static final double UNITM = (1.6605655e-24);
  static final double BOLTZ = (1.380662e-16);
  static final double AVGNO = (6.0220045e23);
/* .....FORCe CONSTANTS SCALeD(DIVIDeD) BY (UNITM/UNITT**2) */
  static final double FC11  = (0.512596);
  static final double FC33  = (0.048098);
  static final double FC12 = (-0.0058230);
  static final double FC13  = (0.016452);
  static final double FC111 = (-0.57191);
  static final double FC333 = (-0.007636);
  static final double FC112 = (-0.001867);
  static final double FC113 = (-0.002047);
  static final double FC123 = (-0.03083);
  static final double FC133 = (-0.0094245);
  static final double FC1111  = (0.8431);
  static final double FC3333 = (-0.00193);
  static final double FC1112 = (-0.0030);
  static final double FC1122  = (0.0036);
  static final double FC1113 = (-0.012);
  static final double FC1123  = (0.0060);
  static final double FC1133 = (-0.0048);
  static final double FC1233  = (0.0211);
  static final double FC1333  = (0.006263);
/*.....WATeR-WATeR INTeRACTION PARAMeTeRS */
  static final double QQ = 0.07152158;
  static final double A1 = 455.313100;
  static final double B1 = 5.15271070;
  static final double A2 = 0.27879839;
  static final double B2 = 2.76084370;
  static final double A3 = 0.60895706;
  static final double B3 = 2.96189550;
  static final double A4 = 0.11447336;
  static final double B4 = 2.23326410;
  static final double CM = 0.45682590;
  static final double AB1 = (A1*B1);
  static final double AB2 = (A2*B2);
  static final double AB3 = (A3*B3);
  static final double AB4 = (A4*B4);
  static final double C1 = (1.00-CM);
  static final double C2 = (0.50*CM);
  static final double QQ2 = (2.00*QQ);
  static final double QQ4 = (2.00*QQ2);
  static final int NDIR = 3;             /* XDIR, YDIR, ZDIR             */
  static final int XDIR = 0;
  static final int YDIR = 1;
  static final int ZDIR = 2;
  static final int NATOM = 3;		/* H1, Oxy, H2 			*/
  static final int MAXODR = 7;		/* Predictor/corrector order	*/
  static final int MAXODR2 = 9;		// (MAXODR+2);
  static final int DISP   = 0;		/* Displacement (position)      */
  static final int VEL    = 1;           /* Velocity                     */
  static final int ACC    = 2;           /* Acceleration                 */
  static final int DER_3  = 3;		/* Higher order derivatives ... */
  static final int DER_4  = 4;
  static final int DER_5  = 5;
  static final int DER_6  = 6;
  static final int FORCES = 7;
}

class util { 
  public static double sign(double a, double b){
    if(b < 0) {
      if(a < 0)
        return a;
      return -a;
    } else {
      if(a < 0)
        return -a;
      return a;
    }
  }
}

class vec { 
  public static int NDIM   = 3;
  public static int NSUB = 8; 			// (1 << NDIM)
  double value[] = new double[NDIM];
  vec() { 
    this.clear();
  }
  synchronized void clear() { 
    for (int i = 0; i < NDIM; i++) { 
      value[i] = 0.0;
    }
  }
  synchronized void unit() { 
    for (int i = 0; i < NDIM; i++) { 
      value[i] = 1.0;
    }
  }
  synchronized void add(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      value[i] += v.value[i];
    }
  }
  synchronized void sub(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      value[i] -= v.value[i];
    }
  }
  synchronized void load(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      value[i] = v.value[i];
    }
  }
  void store(vec v) { 
   synchronized (v) { 
     for (int i = 0; i < NDIM; i++) {
        v.value[i] = value[i];
      }
   }
  }
  synchronized void div(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      value[i] /= v.value[i];
    }
  }
  synchronized void prod(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      value[i] *= v.value[i];
    }
  }
  synchronized void scale(double s) { 
   for (int i = 0; i < NDIM; i++) {
      value[i] *= s;
    }
  }
  double dotProd(vec v) { 
    double d = 0.0;
    for (int i = 0; i < NDIM; i++) {
       d += value[i] * v.value[i];
    }
    return d; 
  }
  synchronized void crossProd(vec u, vec w) { 
    value[0] = u.value[1]*w.value[2] - u.value[2]*w.value[1];
    value[1] = u.value[2]*w.value[0] - u.value[0]*w.value[2];
    value[2] = u.value[0]*w.value[1] - u.value[1]*w.value[0];
  }
  synchronized void min(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      if (value[i] > v.value[i]) value[i] = v.value[i];
    }
  }
  synchronized void max(vec v) { 
   for (int i = 0; i < NDIM; i++) {
      if (value[i] < v.value[i]) value[i] = v.value[i];
    }
  }
  double norm(){ 
    double d=0.0;
    for(int i=0; i < NDIM; i++) {
      d += value[i]; 
    }
    return d;
  }
  double squareNorm(){ 
    double d=0.0;
    for(int i=0; i < NDIM; i++) {
      d += (value[i]*value[i]); 
    }
    return d;
  }
  double getElement(int i) { 
    return(value[i]);
  }
  synchronized void setElement(int i, double v) { 
    value[i] = v;
  }
  synchronized void clearElement(int i) { 
    value[i] = 0.0;
  }
  synchronized void addElement(int i, double v) { 
    value[i] += v;
  }
  void read(DataInputStream s) throws java.io.IOException { 
    for (int i = 0; i < NDIM; i++) { 
      value[i] = s.readDouble();
    }
  }
  void print() { 
    System.out.print("(");
    for (int i = 0; i < NDIM; i++) { 
      System.out.print(value[i]);
      System.out.print(" ");
    }
    System.out.print(")");
  }
}

// --- To be used as a scalar accumulator
class acc_double {
  double val;
  acc_double()			{ val = 0.0; }
  double readval()		{ return val; }
  void writeval(double d)	{ val = d; }
  synchronized void addval(double d)         { val += d; }
};

// -----------------------------------------------------------------------------
// ATOM
// -----------------------------------------------------------------------------
class atom {
  vec M[] = new vec[C.MAXODR2];
  atom() { 
    for (int i = 0; i < C.MAXODR2; i++) { 
      M[i] = new vec();
    }
  }
  
  vec getM(int idx) { return M[idx]; }

  void clearAtom() 		{ 
    for(int i=0; i < C.MAXODR2;i++) {
      M[i].clear();
    }
  }
  void predic(int norder, double coeffs[]){
    int JIZ, JI, L, f;
    vec S = new vec(), T = new vec();
  
    JIZ=2;
    for (f = 0; f < norder; f++) {
      JI = JIZ;
      S.clear();
      for (L = f; L < norder; L++) {
        M[L+1].store(T);
        T.scale(coeffs[JI]);
        S.add(T);
        JI++;
      } 
      M[f].add(S);
      JIZ += (norder+1);
    } 
  } 

  void correc(int norder, double coeffs[]){
    int f;
    vec S = new vec(), T = new vec();
  
    M[C.FORCES].store(S);
    M[C.ACC].store(T);
    S.sub(T);
    for(f = 0; f < (norder+1); f++){
      T.load(S);
      T.scale(coeffs[f]);
      M[f].add(T);
    }
  }
}

// -----------------------------------------------------------------------------
// WATER 
// -----------------------------------------------------------------------------
class h2o {
  atom H1 = new atom();
  atom O = new atom();
  atom H2 = new atom();
  vec V = new vec();
  h2o() { }

  void loadH1Pos(vec v) { H1.getM(C.DISP).load(v); }
  void loadOPos(vec v)  {  O.getM(C.DISP).load(v); }
  void loadH2Pos(vec v) { H2.getM(C.DISP).load(v); }
  
  void storeH1Pos(vec v) { H1.getM(C.DISP).store(v); }
  void storeOPos(vec v)  {  O.getM(C.DISP).store(v); }
  void storeH2Pos(vec v) { H2.getM(C.DISP).store(v); }
  
  void loadH1Vel(vec v) { H1.getM(C.VEL).load(v); }
  void loadOVel(vec v)  {  O.getM(C.VEL).load(v); }
  void loadH2Vel(vec v) { H2.getM(C.VEL).load(v); }
  
  void storeH1Vel(vec v) { H1.getM(C.VEL).store(v); }
  void storeOVel(vec v)  {  O.getM(C.VEL).store(v); }
  void storeH2Vel(vec v) { H2.getM(C.VEL).store(v); }
  
  void loadV(vec v)     { V.load(v); }
  void storeV(vec v)    { V.store(v); }
  
  void clear(){
    H1.clearAtom();
     O.clearAtom();
    H2.clearAtom();
  }
  
  void predic(int n, double c[]){
    H1.predic(n,c);
     O.predic(n,c);
    H2.predic(n,c);
  }
  
  void correc(int n, double c[]){
    H1.correc(n,c);
     O.correc(n,c);
    H2.correc(n,c);
  }
  
  void scaleMomenta(int Dest, double HM, double OM){
    H1.getM(Dest).scale(HM); 
     O.getM(Dest).scale(OM); 
    H2.getM(Dest).scale(HM);
  }
  
  void loadDirPos(int dir, double v[]){
    H1.getM(C.DISP).setElement(dir,v[0]);
     O.getM(C.DISP).setElement(dir,v[1]);
    H2.getM(C.DISP).setElement(dir,v[2]);
  }
  
  // ---------------------------------------------------------------------------
  void storeDirVel(int dir, vec v){
    v.value[0] = H1.getM(C.VEL).getElement(dir);
    v.value[1] =  O.getM(C.VEL).getElement(dir);
    v.value[2] = H2.getM(C.VEL).getElement(dir);
  }
  // ---------------------------------------------------------------------------
  void shiftAxis(int dir, double v){
    H1.getM(C.DISP).addElement(dir,v);
     O.getM(C.DISP).addElement(dir,v);
    H2.getM(C.DISP).addElement(dir,v);
  }

  void kineti(vec s){
    vec T1 = new vec();
    vec T2 = new vec();
    vec v1 = new vec();
    vec v2 = new vec();
    vec v3 = new vec();
    
    H1.getM(C.VEL).store(v1);
     O.getM(C.VEL).store(v2);
    H2.getM(C.VEL).store(v3);
    T1.load(v1);
    T1.prod(T1);
    T2.load(v3);
    T2.prod(T2);
    T1.add(T2);
    T1.scale(C.HMAS);
    v2.prod(v2);
    v2.scale(C.OMAS);
    T1.add(v2);
    s.add(T1);
  }
  
  void bndry(double b){
    int i;
    vec t = new vec();
  
    O.getM(C.DISP).store(t);
    for(i = C.XDIR; i <= C.ZDIR; i++ ) {
      if(t.value[i] > b) {
        shiftAxis(i,-b);
      } else if (t.value[i] < 0.00) {
        shiftAxis(i,b);
      }
    }
  } 
  
  void intra_poteng(vec v){
    double LocPot, DTS, R1, R2, RX, COS, DT, DR1, DR2, DR1S, DR2S, DRP;
    vec t1 = new vec();
    vec t2 = new vec();
    vec t3 = new vec();
    vec t4 = new vec();
    vec h1 = new vec();
    vec Ox = new vec();
    vec h2 = new vec();
    vec VM = new vec();
    vec tmp = new vec();
  
    tmp.clear();
    LocPot = 0.0;
  
    H1.getM(C.DISP).store(h1);
     O.getM(C.DISP).store(Ox);
    H2.getM(C.DISP).store(h2);
  
    // Compute VM = Ox.Pos * C1 + (H1.Pos + H2.Pos) *C2
    t1.load(h1);
    t2.load(Ox);
    t3.load(h2);
    t2.scale(C.C1);
    t3.add(t1);
    t3.scale(C.C2);
    t2.add(t3);
    VM.load(t2);
  
    t1.load(Ox);
    t1.sub(h1);
    R1 = t1.dotProd(t1);
  
    t1.load(Ox);
    t1.sub(h2);
    R2 = t1.dotProd(t1);
  
    t1.load(h1);
    t2.load(Ox);
    t3.load(h2);
    t4.load(t2);
    t2.sub(t1);
    t4.sub(t3);
    RX = t2.dotProd(t4);
  
    R1 = Math.sqrt(R1);
    R2 = Math.sqrt(R2);
    COS = RX/(R1*R2);
    DT = (Math.acos(COS)-C.ANGLE)*C.ROH;
    DR1 = R1-C.ROH;
    DR2 = R2-C.ROH;
    DR1S = DR1*DR1;
    DR2S = DR2*DR2;
    DRP = DR1+DR2;
    DTS = DT*DT;
  
    LocPot += (C.FC11*(DR1S+DR2S)+C.FC33*DTS)*0.5+C.FC12*DR1*DR2+C.FC13*DRP*DT
       +(C.FC111*(DR1S*DR1+DR2S*DR2)+C.FC333*DTS*DT+C.FC112*DRP*DR1*DR2
       +C.FC113*(DR1S+DR2S)*DT+C.FC123*DR1*DR2*DT+C.FC133*DRP*DTS)*C.ROHI;
    LocPot += (C.FC1111*(DR1S*DR1S+DR2S*DR2S)+C.FC3333*DTS*DTS+
       C.FC1112*(DR1S+DR2S)*DR1*DR2+C.FC1122*DR1S*DR2S+
       C.FC1113*(DR1S*DR1+DR2S*DR2)*DT+C.FC1123*DRP*DR1*DR2*DT+
       C.FC1133*(DR1S+DR2S)*DTS+C.FC1233*DR1*DR2*DTS+
       C.FC1333*DRP*DTS*DT)*C.ROHI2;
  
    tmp.value[0] = LocPot;
    V.load(VM);
    v.add(tmp);
  }
  
  void vir(acc_double v){
    double loc_vir;
    vec tmp1 = new vec();
    vec tmp2 = new vec();
  
    loc_vir = 0.0;
    H1.getM(C.DISP).store(tmp1);
    H1.getM(C.FORCES).store(tmp2);
    loc_vir += tmp1.dotProd(tmp2);
    O.getM(C.DISP).store(tmp1);
    O.getM(C.FORCES).store(tmp2);
    loc_vir += tmp1.dotProd(tmp2);
    H2.getM(C.DISP).store(tmp1);
    H2.getM(C.FORCES).store(tmp2);
    loc_vir += tmp1.dotProd(tmp2);
    v.addval(loc_vir);
  }
  
  
  void intraf(){
    double SUM, R1, R2, COS, SIN;
    double DT, DTS, DR1, DR1S, DR2, DR2S, R1S, R2S, F1, F2, F3;
    vec vr1 = new vec();
    vec vr2 = new vec();
    vec dt1 = new vec();
    vec dt3 = new vec();
    vec dr11 = new vec();
    vec dr23 = new vec();
    vec s = new vec();
    vec v1 = new vec();
    vec v2 = new vec();
    vec v3 = new vec();
    vec h1 = new vec();
    vec Ox = new vec();
    vec h2 = new vec();

    SUM=0.0;
    R1=0.0;
    R2=0.0;
  
    s.clear();
  
    H1.getM(C.DISP).store(h1);
     O.getM(C.DISP).store(Ox);
    H2.getM(C.DISP).store(h2);
  
    v1.load(Ox);
    v1.scale(C.C1);
    v2.load(h1);
    v3.load(h2);
    v2.add(v3);
    v2.scale(C.C2);
    v1.add(v2);
    V.load(v1);
  
    vr1.load(Ox);
    v1.load(h1);
    vr1.sub(v1);
    R1 = vr1.dotProd(vr1);
    vr2.load(Ox);
    v2.load(h2);
    vr2.sub(v2);
    R2 = vr2.dotProd(vr2);
    SUM = vr1.dotProd(vr2);
  
    R1=Math.sqrt(R1);
    R2=Math.sqrt(R2);
  
  /* CALCULATE COS(THETA), SIN(THETA), DELTA(R1), DELTA(R2), AND DELTA(THETA) */
    COS=SUM/(R1*R2);
    SIN=Math.sqrt(C.ONE-COS*COS);
    DT=(Math.acos(COS)-C.ANGLE)*C.ROH;
    DTS=DT*DT;
    DR1=R1-C.ROH;
    DR1S=DR1*DR1;
    DR2=R2-C.ROH;
    DR2S=DR2*DR2;
  
  /* CALCULATE DERIVATIVES OF R1/X1, R2/X3, THETA/X1, AND THETA/X3 */
    R1S=C.ROH/(R1*SIN);
    R2S=C.ROH/(R2*SIN);
    dr11.load(vr1); dr11.scale((1.0/R1));
    dr23.load(vr2); dr23.scale((1.0/R2));
    dt1.load(dr11); dt1.scale(COS);
    dt1.sub(dr23); dt1.scale(R1S);
    dt3.load(dr23); dt3.scale(COS);
    dt3.sub(dr11); dt3.scale(R2S);
  
  /* CALCULATE FORCES */
    F1=C.FC11*DR1+C.FC12*DR2+C.FC13*DT;
    F2=C.FC33*DT +C.FC13*(DR1+DR2);
    F3=C.FC11*DR2+C.FC12*DR1+C.FC13*DT;
    F1=F1+(3.0*C.FC111*DR1S+C.FC112*(2.0*DR1+DR2)*DR2
       +2.0*C.FC113*DR1*DT+C.FC123*DR2*DT+C.FC133*DTS)*C.ROHI;
    F2=F2+(3.0*C.FC333*DTS+C.FC113*(DR1S+DR2S)
       +C.FC123*DR1*DR2+2.0*C.FC133*(DR1+DR2)*DT)*C.ROHI;
      F3=F3+(3.0*C.FC111*DR2S+C.FC112*(2.0*DR2+DR1)*DR1
       +2.0*C.FC113*DR2*DT+C.FC123*DR1*DT+C.FC133*DTS)*C.ROHI;
    F1=F1+(4.0*C.FC1111*DR1S*DR1+C.FC1112*(3.0*DR1S+DR2S)
       *DR2+2.0*C.FC1122*DR1*DR2S+3.0*C.FC1113*DR1S*DT
       +C.FC1123*(2.0*DR1+DR2)*DR2*DT+(2.0*C.FC1133*DR1
       +C.FC1233*DR2+C.FC1333*DT)*DTS)*C.ROHI2;
    F2=F2+(4.0*C.FC3333*DTS*DT+C.FC1113*(DR1S*DR1+DR2S*DR2)
       +C.FC1123*(DR1+DR2)*DR1*DR2+2.0*C.FC1133*(DR1S+DR2S)
       *DT+2.0*C.FC1233*DR1*DR2*DT+3.0*C.FC1333*(DR1+DR2)*DTS)*C.ROHI2;
    F3=F3+(4.0*C.FC1111*DR2S*DR2+C.FC1112*(3.0*DR2S+DR1S)
       *DR1+2.0*C.FC1122*DR1S*DR2+3.0*C.FC1113*DR2S*DT
       +C.FC1123*(2.0*DR2+DR1)*DR1*DT+(2.0*C.FC1133*DR2
       +C.FC1233*DR1+C.FC1333*DT)*DTS)*C.ROHI2;
  
    v1.load(dr11); 
    v1.scale(F1);
    v2.load(dt1); 
    v2.scale(F2);
    v1.add(v2);
    v3.load(v1);
    H1.getM(C.FORCES).load(v1);
  
    v1.load(dr23); 
    v1.scale(F3);
    v2.load(dt3); 
    v2.scale(F2);
    v1.add(v2);
    H2.getM(C.FORCES).load(v1);
  
    v3.add(v1);
    v3.scale(-1.00);
    O.getM(C.FORCES).load(v3);
  }
  
  // --------------------------------------------------------------------
  void updateFields(int d, skratch_pad p){
    vec tmp = new vec();
     p.storeH1force(tmp);
     H1.getM(d).add(tmp);
     p.storeOforce(tmp);
     O.getM(d).add(tmp);
     p.storeH2force(tmp);
     H2.getM(d).add(tmp);
  }
  
  // --------------------------------------------------------------------
  void dump(){
    int i;
    for(i=0; i <= C.FORCES; i++){
      H1.getM(i).print();
      O.getM(i).print();
      H2.getM(i).print();
      System.out.print("\n");
    }
    System.out.print("\n V:");
    V.print();
    System.out.print("\n");
  }
}
  
  // --------------------------------------------------------------------
  // PAD: to perform the accumulation of forces during INTERF
  // --------------------------------------------------------------------
class skratch_pad {
    vec H1pos = new vec();	// input data
    vec Opos = new vec();   
    vec H2pos = new vec();
    vec VM = new vec();
    vec H1force = new vec(); // output data
    vec Oforce = new vec();
    vec H2force = new vec();
    skratch_pad() { 
      H1pos.clear(); 
      Opos.clear(); 
      H2pos.clear(); 
      VM.clear(); 
      H1force.clear(); 
      Oforce.clear(); 
      H2force.clear(); 
    }
    void storeVM(vec v)	{ VM.store(v); }
    void storeH1pos(vec v) { H1pos.store(v); }
    void storeOpos(vec v)  { Opos.store(v); }
    void storeH2pos(vec v) { H2pos.store(v); }
    void storeH1force(vec v) { H1force.store(v); }
    void storeOforce(vec v)  { Oforce.store(v); }
    void storeH2force(vec v) { H2force.store(v); }

    void update_forces(vec Res[]) {
      H1force.add(Res[0]); 
      Oforce.add(Res[1]); 
      H2force.add(Res[2]); 
    }

    void read_data(h2o m) { 
      vec tmp = new vec();
      m.storeH1Pos(tmp); 
      H1pos.load(tmp);
      m.storeOPos(tmp);
      Opos.load(tmp);
      m.storeH2Pos(tmp); 
      H2pos.load(tmp);
      m.storeV(tmp);
      VM.load(tmp);
      H1force.clear(); 
      Oforce.clear();
      H2force.clear();
    }
}
  
  
  // --- SIMPARM.H --------------------------------------------------------------
  class simparm {
    double TLC[] = new double[100]; /* Taylor Series Coeffs		*/
    double PCC[] = new double[11];  /* Predictor/Corrector Coeffs	*/
  
    double        R1;
    double        ELPST;
  
    int	IRST;
    int	NVAR;
    int	NXYZ;
    int 	NXV;
    int 	IXF;
    int 	IYF;
    int 	IZF;
    int 	IMY;
    int 	IMZ;
  
    double TEMP;		/* Temperature			*/
    double RHO;		/* Density			*/
    double TSTEP;		/* Time step			*/
    double BOXL;		/* Box length			*/
    double BOXH;		/* Box height			*/
    double CUTOFF;	/* Cut-Off radius		*/
    double CUT2;		/* Square of Cut-Off radius	*/
  
    double FKIN;
    double FPOT;
  
    int NMOL;			/* Number of Molecules		*/
    int NORDER;			/* Integration order		*/
    int NATMO;			/* Number of Atoms		*/
    int NATMO3;
    int NMOL1;
    int NSTEP;			/* Number of time steps		*/
    int NSAVE; 
    int NRST; 
    int NPRINT;
    int NFMC;
    int PAR;
    int I2;
  
    double FHM;
    double FOM;
    double REF1;
    double REF2;
    double REF4;
  
    double[] getTLC()		{ return TLC; }
    double[] getPCC()		{ return PCC; }
  
    double getR1()         { return R1; }
    double getELPST()      { return ELPST; }
  
    double getFKIN()      { return FKIN; }
    double getFPOT()      { return FPOT; }
  
    void setPS1(double v[]) { R1 =v[0]; ELPST=v[1]; }
    void resetStat()       { ELPST=0.00; }
  
   int getIRST() 	{ return IRST; }
   int getNVAR() 	{ return NVAR; }
   int getNXYZ() 	{ return NXYZ; }
   int getNXV() 	{ return NXV; }
   int getIXF() 	{ return IXF; }
   int getIYF() 	{ return IYF; }
   int getIZF() 	{ return IZF; }
   int getIMY() 	{ return IMY; }
   int getIMZ() 	{ return IMZ; }
  
   void setPS2(int v[]) { 
     IRST=v[0]; 
     NVAR=v[1]; 
     NXYZ=v[2]; 
     NXV=v[3]; 
     IXF=v[4]; 
     IYF=v[5]; 
     IZF=v[6]; 
     IMY=v[7]; 
     IMZ=v[8]; 
   }
  
   double getTEMP()	{ return TEMP; }
   double getRHO() 	{ return RHO; }
   double getTSTEP()	{ return TSTEP; }
   double getBOXL()	{ return BOXL; }
   double getBOXH()	{ return BOXH; }
   double getCUTOFF()	{ return CUTOFF; }
   double getCUT2()  	{ return CUT2; }
  
   void setPS3(double v[]) { 
     TEMP =v[0]; RHO=v[1]; 
     TSTEP=v[2]; BOXL=v[3]; BOXH=v[4]; CUTOFF=v[5]; CUT2=v[6];}
  
   int getNMOL()		{ return  NMOL; }
   int getNORDER()	{ return  NORDER; }
   int getNATMO()		{ return  NATMO; }
   int getNATMO3()	{ return  NATMO3; }
   int getNMOL1()		{ return  NMOL1; }
   int getNSTEP()		{ return  NSTEP; }
   int getNSAVE()		{ return  NSAVE; }
   int getNRST()		{ return  NRST; }
   int getNPRINT()	{ return  NPRINT; }
   int getNFMC()		{ return  NFMC; }
   int getPAR()		{ return  PAR; }
   int getI2()		{ return  I2; }
  
   void setPS4(int v[]){ NMOL=v[0]; NORDER=v[1]; NATMO=v[2]; 
  NATMO3=v[3]; NMOL1=v[4]; NSTEP=v[5]; NSAVE=v[6]; NRST=v[7]; NPRINT=v[8]; 
  NFMC=v[9]; I2=v[10]; }
   void setNFMC(int v) { NFMC = v; }
  
   double getFHM() { return FHM; }
   double getFOM() { return FOM; }
   double getREF1() { return REF1; }
   double getREF2() { return REF2; }
   double getREF4() { return REF4; }
  
   void   setPS5(double v[]) {FHM=v[0];FOM=v[1];REF1=v[2]; REF2=v[3]; REF4=v[4]; }
  
   double computeFAC(){
     return C.BOLTZ*TEMP*NATMO/C.UNITM * Math.pow((C.UNITT*TSTEP/C.UNITL),2.0);
   }
  
  // void	dump();

simparm(){
  int i;

  for(i=0; i < 100; i++)
    TLC[i] = 0.0;
  for(i=0; i < 11; i++)
    PCC[i] = 0.0;
  R1 = ELPST = 0.0;
  IRST = NVAR = NXYZ = NXV = IXF = IYF = IZF = IMY = IMZ = 0;
  TEMP = RHO = TSTEP = BOXL = BOXH = CUTOFF = CUT2 = 0.0;
  NMOL = NORDER = NATMO = NATMO3 = NMOL1 = NSTEP = NSAVE = 0;
  NRST = NPRINT = NFMC = I2 = 0;
  FHM = FOM = REF1 = REF2 = REF4 = 0.0;
}

void CNSTNT(){
  int NN,N1,K1,N;
  double TN, TK;

  N = NORDER+1;
  TLC[1] = 1.00;
  for (N1 = 2; N1<=N; N1++){  
    NN = N1-1;
    TN = NN;
    TLC[N1] = 1.00;
    TK = 1.00;
    for (K1=2;K1<=N1;K1++) { 
      TLC[(K1-1)*N+NN] = TLC[(K1-2)*N+NN+1]*TN/TK;
      NN = NN-1;
      TN = TN-C.ONE;
      TK = TK+C.ONE;
    }
  }

  PCC[2] = C.ONE;
  N1 = N-1;
  if((N1 == 1) || (N1 == 2)){
    System.err.print("***** ERROR: THE ORDER HAS TO BE GREATER THAN 2 ****\n");
  }
  if(N1 == 3){
    PCC[0] = C.ONE/6.00;
    PCC[1] = C.FIVE/6.00;
    PCC[3] = C.ONE/3.00;
  }
  if(N1 == 4){
    PCC[0] = (double) 19.00/120.00;
    PCC[1] = C.THREE/4.00;
    PCC[3] = C.ONE/2.00;
    PCC[4] = C.ONE/12.00;
  }
  if(N1 == 5){
    PCC[0] = C.THREE/20.00;
    PCC[1] = (double) 251.00/360.00;
    PCC[3] = (double) 11.00/18.00;
    PCC[4] = C.ONE/6.00;
    PCC[5] = C.ONE/60.00;
  }
  if(N1 ==  6){
	PCC[0] = (double) 863.00/6048.00;
	PCC[1] = (double) 665.00/1008.00;
	PCC[3] = (double) 25.00/36.00;
	PCC[4] = (double) 35.00/144.00;
	PCC[5] = C.ONE/24.00;
	PCC[6] = C.ONE/360.00;
  }
    if(N1 ==  7){
	PCC[0] = (double) 275.00/2016.00;
	PCC[1] = (double) 19087.00/30240.00;
	PCC[3] = (double) 137.00/180.00;
	PCC[4] = C.FIVE/16.00;
	PCC[5] = (double) 17.00/240.00;
	PCC[6] = C.ONE/120.00;
	PCC[7] = C.ONE/2520.00;
    }
}

void SYSCNS() {
  TSTEP=TSTEP/C.UNITT;
  NATMO=C.NATOM*NMOL;
  NATMO3=NATMO*3;
  BOXL=Math.pow((NMOL*C.WTMOL*C.UNITM/RHO),(1.00/3.00));
  BOXL=BOXL/C.UNITL;
  BOXH=BOXL*0.50;
  CUTOFF=Math.max(BOXH,CUTOFF);
  REF1= -C.QQ/(CUTOFF*CUTOFF*CUTOFF);
  REF2=C.TWO*REF1;
  REF4=C.TWO*REF2;
  CUT2=CUTOFF*CUTOFF;
  FPOT= C.UNITM * Math.pow((C.UNITL/C.UNITT),2.0) / (C.BOLTZ*TEMP*NATMO);
  FKIN=FPOT*0.50/(TSTEP*TSTEP);
  FHM=(TSTEP*TSTEP*0.50)/C.HMAS;
  FOM=(TSTEP*TSTEP*0.50)/C.OMAS;
  NMOL1=NMOL-1;
}

void loadParms(String name) throws java.io.FileNotFoundException, java.io.IOException {
  System.out.print("OUTPUT FOR PERFECT CLUB BENCHMARK: MDG Revision: 1.*  Author: kipp\n");
  NORDER = 5;
  TEMP = 298.0;
  RHO = 0.9980;
  TSTEP = 1.5e-16;
  CUTOFF = 0.0;

  BufferedReader instr = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
  String s;
  s = instr.readLine();
  TSTEP = new Double(s).doubleValue();
  s = instr.readLine();
  NMOL = new Integer(s).intValue();
  s = instr.readLine();
  NSTEP = new Integer(s).intValue();
  s = instr.readLine();
  NORDER = new Integer(s).intValue();
  s = instr.readLine();
  NSAVE = new Integer(s).intValue();
  s = instr.readLine();
  NRST = new Integer(s).intValue();
  s = instr.readLine();
  NPRINT = new Integer(s).intValue();
  s = instr.readLine();
  NFMC = new Integer(s).intValue();
  s = instr.readLine();
  PAR = new Integer(s).intValue();
  
  CNSTNT();
  System.out.print("\nTEMPERATURE                = ");
  System.out.print(TEMP);
  System.out.print("\n");
  System.out.print("DENSITY                    = ");
  System.out.print(RHO);
  System.out.print(" G/C.C.\n");
  System.out.print("NUMBER OF MOLECULES        = ");
  System.out.print(NMOL); 
  System.out.print("\n");
  System.out.print("TIME STEP                  = ");
  System.out.print(TSTEP);
  System.out.print(" SEC\n");
  System.out.print("ORDER USED TO SOLVE F=MA   = ");
  System.out.print(NORDER);
  System.out.print("\n");
  System.out.print("NO. OF TIME STEPS          = ");
  System.out.print(NSTEP);
  System.out.print("\n");
  System.out.print("FREQUENCY OF DATA SAVING   = ");
  System.out.print(NSAVE);
  System.out.print("\n");
  System.out.print("FREQUENCY TO WRITE RST FILE= ");
  System.out.print(NRST);
  System.out.print("\n");
  SYSCNS();
  System.out.print("SPHERICAL CUTOFF RADIUS    = ");
  System.out.print(CUTOFF); 
  System.out.print(" ANGSTROM\n");
  IRST=0;
}


}

// --- ENSEMBLE_H -------------------------

class ensemble {
  int numMol;
  h2o molecule[];
  skratch_pad pad[];
  double TTMV;
  double TVIR;
  acc_double VIR = new acc_double();
  vec POT = new vec();
  double TKIN;
  vec KIN = new vec();
  h2o getMolecule(int idx)  	{ return (molecule[idx]); }
  skratch_pad getPad(int idx)	{ return (pad[idx]); }


  ensemble(simparm p) { 
    numMol = p.getNMOL(); 
    molecule = new h2o[numMol]; 
    for (int i = 0; i < numMol; i++) { 
      molecule[i] = new h2o();
    }
    pad = new skratch_pad[numMol]; 
    for (int i = 0; i < numMol; i++) { 
      pad[i] = new skratch_pad();
    }
    TTMV = 0.0; 
    TVIR = 0.0; 
    TKIN = 0.0; 
    VIR.writeval(0.0); 
    POT.clear(); 
    KIN.clear(); 
  }

void clearTVIR(){ 
  TVIR = 0.0;
}

void updateTVIR(){ 
  TVIR -= VIR.readval();
}

void BNDRY(double size) {
  int i;

  for(i = 0; i < numMol; i++){
    molecule[i].bndry(size);
  }
} 

void INITIA()  throws java.io.FileNotFoundException, java.io.IOException {
  vec minimum;
  double XS,ZERO,WCOS,WSIN,FAC,NS;
  double XMAS[] = new double[C.NATOM];
  double XT[] = new double[C.NATOM];
  double YT[] = new double[C.NATOM];
  double ZT[] = new double[C.NATOM];
  vec t = new vec();

  vec SUM = new vec();
  vec SU = new vec();
  vec t1 = new vec();

  int i, j, k, mol;

  BufferedReader random_numbers = new BufferedReader(new InputStreamReader(new FileInputStream("random.in")));

     NS = Math.pow((double) numMol, 1.0/3.0) - 0.00001;
     XS = water.parms.getBOXL()/NS;
     ZERO = XS * 0.50;
     WCOS = C.ROH * Math.cos(C.ANGLE * 0.5);
     WSIN = C.ROH * Math.sin(C.ANGLE * 0.5);

     System.out.print("***** NEW RUN STARTING FROM REGULAR LATTICE *****\n");
     XT[1] = ZERO;
     mol = 0;
     for(i=0; i < NS; i++) {
       XT[0]=XT[1]+WCOS;
       XT[2]=XT[0];
       YT[1]=ZERO;
       for(j=0; j < NS; j++) {
         YT[0]=YT[1]+WSIN;
         YT[2]=YT[1]-WSIN;
         ZT[0]=ZT[1]=ZT[2]=ZERO;
         for(k = 0; k < NS; k++) {
	   molecule[mol].loadDirPos(C.XDIR,XT);
	   molecule[mol].loadDirPos(C.YDIR,YT);
	   molecule[mol].loadDirPos(C.ZDIR,ZT);
           mol++;
           ZT[0] += XS; ZT[1] += XS; ZT[2] += XS;
         }
         YT[1] += XS;
       }
       XT[1] += XS;
     }

     if(numMol != mol) {
       System.out.print(" *** Error: Lattice number of mol mismatch \n");
     }

  /* ASSIGN RANDOM MOMENTA */
  String s;
  s = random_numbers.readLine();
  SU.value[0] = new Double(s).doubleValue();

  SUM.clear();
  for(i = 0; i < numMol; i++) {
    for (k = 0; k < 3; k++) { 
      s = random_numbers.readLine();
      t.value[k] = new Double(s).doubleValue();
    }

    molecule[i].loadH1Vel(t);
    SUM.add(t);
    for (k = 0; k < 3; k++) { 
      s = random_numbers.readLine();
      t.value[k] = new Double(s).doubleValue();
    }

    molecule[i].loadOVel(t);
    SUM.add(t);
    for (k = 0; k < 3; k++) { 
      s = random_numbers.readLine();
      t.value[k] = new Double(s).doubleValue();
    }

    molecule[i].loadH2Vel(t);
    SUM.add(t);
  } 
  /* .....FIND AVERAGE MOMENTA PER ATOM */
  SUM.value[0] /= (C.NATOM*water.parms.getNMOL());
  SUM.value[1] /= (C.NATOM*water.parms.getNMOL());
  SUM.value[2] /= (C.NATOM*water.parms.getNMOL());

  /* FIND NORMALIZATION FACTOR SO THAT <K.E.>=KT/2 */

  SU.clear();
  for (i = 0; i < numMol; i++) {
    molecule[i].storeDirVel(C.XDIR,t);
    SU.value[0] += (Math.pow((t.value[0]-SUM.value[0]),2.0) + Math.pow((t.value[2]-SUM.value[0]),2.0))/C.HMAS 
	+ Math.pow((t.value[1]-SUM.value[0]),2.0)/C.OMAS;
    molecule[i].storeDirVel(C.YDIR,t);
    SU.value[1] += (Math.pow((t.value[0]-SUM.value[1]),2.0) + Math.pow((t.value[2]-SUM.value[1]),2.0))/C.HMAS 
	+ Math.pow((t.value[1]-SUM.value[1]),2.0)/C.OMAS;
    molecule[i].storeDirVel(C.ZDIR,t);
    SU.value[2] += (Math.pow((t.value[0]-SUM.value[2]),2.0) + Math.pow((t.value[2]-SUM.value[2]),2.0))/C.HMAS 
	+ Math.pow((t.value[1]-SUM.value[2]),2.0)/C.OMAS;
  }
  FAC=water.parms.computeFAC();
  SU.value[0]=Math.sqrt(FAC/SU.value[0]);
  SU.value[1]=Math.sqrt(FAC/SU.value[1]);
  SU.value[2]=Math.sqrt(FAC/SU.value[2]);
  /* NORMALIZE INDIVIDUAL VELOCITIES SO THAT THERE IS NO BULK MOMENTA */

  XMAS[0] = 1.0/C.HMAS;
  XMAS[1] = 1.0/C.OMAS;
  XMAS[2] = 1.0/C.HMAS;

  for(i = 0; i < numMol; i++) {
    molecule[i].storeH1Vel(t1);
    t1.sub(SUM); 
    t1.prod(SU); 
    t1.scale(XMAS[0]);
    molecule[i].loadH1Vel(t1);

    molecule[i].storeOVel(t1);
    t1.sub(SUM); 
    t1.prod(SU); 
    t1.scale(XMAS[1]);
    molecule[i].loadOVel(t1);

    molecule[i].storeH2Vel(t1);
    t1.sub(SUM); 
    t1.prod(SU); 
    t1.scale(XMAS[2]);
    molecule[i].loadH2Vel(t1);
  } 
} 

// ----------------------------------------------------------------------
// POTENG
// ----------------------------------------------------------------------
  void potengInnerSplit(int idx, int first, int num) { 
    if (num == 1) {
      vec tmp = new vec();
      interPoteng2Aux(pad[idx],pad[first],tmp);
      POT.add(tmp);
    } else {
      potengInnerSplit(idx, first, num/2);
      potengInnerSplit(idx, first+num/2, (num - (num/2)));
    }
  }

  void potengInnerLoop(int idx){
    int i;
    vec tmp = new vec();

    for(i = idx+1; i < numMol; i++){
      interPoteng2Aux(pad[idx],pad[i],tmp);
      POT.add(tmp);
    }
  }

  void potengInnerDispatch(int idx) { 
    if (water.parms.getPAR() == 0) {
      potengInnerLoop(idx);
    } else if (water.parms.getPAR() == 1) {
      potengInnerSplit(idx, idx+1, (numMol-idx)-1);
    } else if (water.parms.getPAR() == 2) {
      potengInnerThread t = new potengInnerThread(this, idx, idx+1, (numMol-idx)-1);
      t.inlineStart();
    } else {
      potengInnerThread t = new potengInnerThread(this, idx, idx+1, (numMol-idx)-1);
      t.start();
      try {
        t.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in INTERF\n");
      }
    }
  }

  void potengOuterLoop(){
    int i;
    for(i = 0; i < numMol-1; i++) {
      potengInnerDispatch(i);
    }
  }

  void potengOuterSplit(int first, int num) {
    if (num == 1) {
      potengInnerDispatch(first);
    } else {
      potengOuterSplit(first, num/2);
      potengOuterSplit(first+num/2, (num - (num/2)));
    }
  }

  void potengOuterDispatch() {
    if (water.parms.getPAR() == 0) {
      potengOuterLoop();
    } else if (water.parms.getPAR() == 1) {
      potengOuterSplit(0, numMol-1);
    } else if (water.parms.getPAR() == 2) {
      potengOuterThread t = new potengOuterThread(this, 0, numMol-1);
      t.inlineStart();
    } else {
      potengOuterThread t = new potengOuterThread(this, 0, numMol-1);
      t.start();
      try {
        t.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in INTERF\n");
      }
    }
  }



void INTER_POTENG() {
  loadData();
  potengOuterDispatch();
}


// ---------------------------------------------------------------------------
void interPoteng2Aux(skratch_pad p1, skratch_pad p2, vec res){
  int KC,K;
  double CL[][];
  CL = new double[3][];
  CL[0] = new double[15]; 
  CL[1] = new double[15]; 
  CL[2] = new double[15]; 
  double RS[] = new double[14];
  double RL[] = new double[14];
  double S[] = new double[2];
  double PotR, PotF;
  double Cut2, Cutoff, Ref1, Ref2;

  PotF = 0.0;
  PotR = 0.0;
  S[0] = water.parms.getBOXH();	// BoxH
  S[1] = water.parms.getBOXL();	// BoxL
  Cut2 = water.parms.getCUT2();
  Cutoff = water.parms.getCUTOFF();
  Ref1 = water.parms.getREF1();
  Ref2 = water.parms.getREF2();

  CSHIFT2(p1,p2,CL,S);
  KC=0;
  for(K = 0; K < 9; K++) {
    RS[K]=CL[0][K]*CL[0][K]+CL[1][K]*CL[1][K]+CL[2][K]*CL[2][K];
    if(RS[K] > Cut2) 
      KC=KC+1;
  } 

  if (KC != 9) {
     for(K = 0; K < 9; K++) {
      if(RS[K] <= Cut2){
        RL[K] = Math.sqrt(RS[K]);
      } else {
    	RL[K] = Cutoff;
	RS[K] = Cut2;
      } 
    } 

    PotR += (-C.QQ2/RL[1]-C.QQ2/RL[2]-C.QQ2/RL[3]-C.QQ2/RL[4]
      +C.QQ /RL[5]+C.QQ /RL[6]+C.QQ /RL[7]+C.QQ /RL[8]+C.QQ4/RL[0]);
    PotF += (-Ref2*RS[0]-Ref1*((RS[5]+RS[6]+RS[7]+RS[8])*0.5
      -RS[1]-RS[2]-RS[3]-RS[4]));

    if (KC <= 0) {
      for (K = 9; K <  14; K++) 
	RL[K]=Math.sqrt(CL[0][K]*CL[0][K]+CL[1][K]*CL[1][K]+CL[2][K]*CL[2][K]);
      PotR += C.A1 * Math.exp(-C.B1*RL[9]) +C.A2*(Math.exp(-C.B2*RL[5])+ 
          Math.exp(-C.B2*RL[6])+Math.exp(-C.B2*RL[7])+Math.exp(-C.B2*RL[ 8]))
     	+C.A3*(Math.exp(-C.B3*RL[10])+Math.exp(-C.B3*RL[11])+Math.exp(-C.B3*RL[12])
	+Math.exp(-C.B3*RL[13]))-C.A4*(Math.exp(-C.B4*RL[10])+Math.exp(-C.B4*RL[11])
	+Math.exp(-C.B4*RL[12])+Math.exp(-C.B4*RL[13]));
    } 
  } 
  res.value[0] = 0.0;
  res.value[1] = PotR;
  res.value[2] = PotF;
}

void INTRA_POTENG(){
  int i;
  for(i=0; i < numMol; i++){
    molecule[i].intra_poteng(POT);
  }
}

void POTENG(){
  POT.clear();
  INTRA_POTENG();
  INTER_POTENG();
  POT.scale(water.parms.getFPOT());
}

void clearTKIN(){ 
  TKIN = 0.0; 
}

void updateTKIN(){ 
  TKIN += KIN.norm();
}

void KINETI(){
  int i;
  KIN.clear();
  for(i=0; i < numMol; i++){
    molecule[i].kineti(KIN);
  }
  updateTKIN();
}

void computeVIR(){
  int i;

  for(i=0; i < numMol; i++){
    molecule[i].vir(VIR);
  }
}

// ----------------------------------------------------------------------
void printENERGY(int iter){
  double XVIR, TENN, XTT, AVGT;
  vec loc_pot = new vec();

/*
  System.out.print("KIN.norm() = ");
  System.out.print(KIN.norm());
  System.out.print(" ");
  KIN.print();
  System.out.print("\n");
*/

  POT.store(loc_pot);
  XVIR = TVIR*water.parms.getFPOT()*0.50/((double)TTMV);
  AVGT = TKIN*water.parms.getFKIN()*(water.parms.getTEMP())*2.00/(3.00*((double)TTMV));
  TENN = KIN.norm() * water.parms.getFKIN();
  XTT = loc_pot.norm()+TENN;

  if ((iter % water.parms.getNPRINT()) == 0){
    System.out.print("     ");
    System.out.print(iter);
    System.out.print(" ");
    System.out.print(TENN);
    System.out.print(" ");
    System.out.print(loc_pot.value[0]);
    System.out.print(" ");
    System.out.print(loc_pot.value[1]);
    System.out.print(" ");
    System.out.print(loc_pot.value[2]);
    System.out.print(" ");
    System.out.print(XTT);
    System.out.print(" ");
    System.out.print(AVGT);
    System.out.print(" ");
    System.out.print(XVIR);
    System.out.print("\n");
  }
}

// ----------------------------------------------------------------------------
void PREDIC(){
  int i, ord;
  double coeffs[];

  for(i=0; i < numMol; i++){
    ord = water.parms.getNORDER();
    coeffs = water.parms.getTLC();
    molecule[i].predic(ord,coeffs);
  }
}

// ----------------------------------------------------------------------------
void CORREC(){
  int i, ord;
  double coeffs[];

  for(i=0; i < numMol; i++){
    ord = water.parms.getNORDER();
    coeffs = water.parms.getPCC();
    molecule[i].correc(ord,coeffs);
  }
}

// ----------------------------------------------------------------------------
// MDMAIN --- MOLECULAR DYNAMICS LOOP 
// ----------------------------------------------------------------------------
void MDMAIN(){

  System.out.print("RESTART ");
  System.out.print(water.parms.getIRST());
  System.out.print(" AFTER ");
  System.out.print(water.parms.getELPST());
  System.out.print(" SECONDS\n");
  clearTVIR();
  clearTKIN();

  if(water.parms.getNSAVE() > 0){
    System.out.print("COLLECTING X AND V DATA AT EVERY %4d TIME STEPS\n");
    System.out.print(water.parms.getNSAVE());
  }
  System.out.print("INTERMEDIATE RESULTS (ENERGY EXPRESSED IN UNITS OF KT ATOM) \n");
  System.out.print("  TIME       KINETIC   INTRA POT   INTER POT   REACT POT       ");
  System.out.print("TOTAL  \n<TEMPERATURE>   <VIR>\n");

  stepsystem();
}

// --- MAIN LOOP -------------------------------------------------------
void stepsystem(){
  int i, n;
  long start, stop;
  long start_serial, stop_serial, total_serial;
  int ticks;
  double dticks;

  total_serial = 0;
  start = System.currentTimeMillis();

  n = water.parms.getNSTEP();
  for(i=1;i <= n; i++) {

    TTMV += 1.00;
/*
    System.out.print(" >>> Step: ");
    System.out.print(i);
    System.out.print("\n");
*/

    start_serial = System.currentTimeMillis();
    PREDIC();
    INTRAF();
    VIR.writeval(0.0);
    computeVIR();
    stop_serial = System.currentTimeMillis();
    total_serial += (stop_serial - start_serial);
    INTERF(C.FORCES);
    SCALEFORCES(C.FORCES);

    start_serial = System.currentTimeMillis();
    CORREC();
    BNDRY(water.parms.getBOXL());
    KINETI();
    updateTVIR();

    stop_serial = System.currentTimeMillis();

    if(( (i % water.parms.getNPRINT()) == 0) || 
	((water.parms.getNSAVE() > 0) && ((i % water.parms.getNSAVE()) == 0))) {
      POTENG();
      printENERGY(i);
    }

  } 
  stop = System.currentTimeMillis();
  dticks = 1000.0;

  System.out.print("execution time = ");
  System.out.print((stop-start) / dticks);
  System.out.print("\n");
} 

void CSHIFT2(skratch_pad p1, skratch_pad p2, double L[][], double S[]){
  int i;

  // ---  XDIR ---
  double vm1,vm2,h1pos1,h1pos2,h2pos1,h2pos2,opos1,opos2;
  vm1 = p1.VM.value[0];
  vm2 = p2.VM.value[0];
  L[0][0] = vm1-vm2;
  h1pos2  = p2.H1pos.value[0];
  L[0][1] = vm1-h1pos2;
  h2pos2  = p2.H2pos.value[0];
  L[0][2] = vm1-h2pos2;
  h1pos1 = p1.H1pos.value[0];
  L[0][3] = h1pos1-vm2;
  h2pos1 = p1.H2pos.value[0];
  L[0][4] = h2pos1-vm2;
  L[0][5] = h1pos1-h1pos2;
  L[0][6] = h1pos1-h2pos2;
  L[0][7] = h2pos1-h1pos2;
  L[0][8] = h2pos1-h2pos2;
  opos1 = p1.Opos.value[0];
  opos2 = p2.Opos.value[0];
  L[0][9] = opos1-opos2;
  L[0][10] = opos1-h1pos2;
  L[0][11] = opos1-h2pos2;
  L[0][12] = h1pos1-opos2;
  L[0][13] = h2pos1-opos2;
  for (i = 0; i < 14; i++) { 
    if (Math.abs(L[0][i]) > S[0]) 
      L[0][i] -= (util.sign(S[1],L[0][i]));
  }

  // ---  YDIR ---
  vm1 = p1.VM.value[1];
  vm2 = p2.VM.value[1];
  h1pos1 = p1.H1pos.value[1];
  L[1][0] = vm1-vm2;
  h1pos2 = p2.H1pos.value[1];
  L[1][1] = vm1-h1pos2;
  h2pos2 = p2.H2pos.value[1];
  L[1][2] = vm1-h2pos2;
  L[1][3] = h1pos1-vm2;
  h2pos1 = p1.H2pos.value[1];
  L[1][4] = h2pos1 - vm2;
  L[1][5] = h1pos1-h1pos2;
  L[1][6] = h1pos1-h2pos2;
  L[1][7] = h2pos1-h1pos2;
  L[1][8] = h2pos1-h2pos2;
  opos1 = p1.Opos.value[1];
  opos2 = p2.Opos.value[1];
  L[1][9] = opos1-opos2;
  L[1][10] = opos1-h1pos2;
  L[1][11] = opos1-h2pos2;
  L[1][12] = h1pos1-opos2;
  L[1][13] = h2pos1-opos2;
  for (i = 0; i < 14; i++) {
    if (Math.abs(L[1][i]) > S[0])
      L[1][i] -= (util.sign(S[1],L[1][i]));
  }

  // ---  ZDIR ---
  vm1 = p1.VM.value[2];
  vm2 = p2.VM.value[2];
  L[2][0] = vm1-vm2;
  h1pos2 = p2.H1pos.value[2];
  L[2][1] = vm1-h1pos2;
  h2pos2 = p2.H2pos.value[2];
  L[2][2] = vm1-h2pos2;
  h1pos1 = p1.H1pos.value[2];
  L[2][3] = h1pos1-vm2;
  h2pos1 = p1.H2pos.value[2];
  L[2][4] = h2pos1-vm2;
  L[2][5] = h1pos1-h1pos2;
  L[2][6] = h1pos1-h2pos2;
  L[2][7] = h2pos1-h1pos2;
  L[2][8] = h2pos1-h2pos2;
  opos1 = p1.Opos.value[2];
  opos2 = p2.Opos.value[2];
  L[2][9] = opos1-opos2;
  L[2][10] = opos1-h1pos2;
  L[2][11] = opos1-h2pos2;
  L[2][12] = h1pos1-opos2;
  L[2][13] = h2pos1-opos2;
  for (i = 0; i < 14; i++) {
    if (Math.abs(L[2][i]) > S[0])
      L[2][i] -= (util.sign(S[1],L[2][i]));
  }
}

// -----------------------------------------------------------------------
// INTRAF:
// -----------------------------------------------------------------------
void INTRAF(){
  int i;
  for (i = 0; i < numMol; i++){
    molecule[i].intraf();
  }
}

double interf2_aux(skratch_pad p1, skratch_pad p2, 
  vec Res1[], vec Res2[]){

  int KC, K;
  double CL[][] = new double[3][];
  CL[0] = new double[15];
  CL[1] = new double[15];
  CL[2] = new double[15];
  double RS[] = new double[15];
  double FF[] = new double[15];
  double RL[] = new double[15];
  double GG[] = new double[15];
  double G110, G23, G45, TT1, TT, FTEMP;
  double gCUT2, gREF1, gREF2, gREF4;
  double loc_vir;
  double S[] = new double[2];

  S[0] = water.parms.getBOXH();
  S[1] = water.parms.getBOXL();
  gCUT2 = water.parms.getCUT2();
  gREF1 = water.parms.getREF1();
  gREF2 = water.parms.getREF2();
  gREF4 = water.parms.getREF4();
  loc_vir = 0.0;

  CSHIFT2(p1,p2,CL,S);
  KC=0;
  for (K = 0; K < 9; K++) {
    RS[K]=CL[0][K]*CL[0][K]+CL[1][K]*CL[1][K]+CL[2][K]*CL[2][K];
    if (RS[K] > gCUT2) 
      KC++;
  } 

  if(KC != 9) {
    for (K = 0; K < 14; K++) 
        FF[K]=0.0;
    if(RS[0] < gCUT2) {
      FF[0]=C.QQ4/(RS[0]*Math.sqrt(RS[0]))+gREF4;
      loc_vir = loc_vir + FF[0]*RS[0];
    } 
    for (K = 1; K < 5; K++) {
      if(RS[K] < gCUT2) { 
	FF[K]= -C.QQ2/(RS[K]*Math.sqrt(RS[K]))-gREF2;
	loc_vir = loc_vir + FF[K]*RS[K];
      } 
      if(RS[K+4] <= gCUT2) { 
	RL[K+4]=Math.sqrt(RS[K+4]);
 	FF[K+4]=C.QQ/(RS[K+4]*RL[K+4])+gREF1;
	loc_vir = loc_vir + FF[K+4]*RS[K+4];
      } 
    } 

    if(KC == 0) {
      RS[9]=CL[0][9]*CL[0][9]+CL[1][9]*CL[1][9]+CL[2][9]*CL[2][9];
      RL[9]=Math.sqrt(RS[9]);
      FF[9]=C.AB1*Math.exp(-C.B1*RL[9])/RL[9];
      loc_vir = loc_vir + FF[9]*RS[9];
      for (K = 10; K < 14; K++) { 
	FTEMP=C.AB2*Math.exp(-C.B2*RL[K-5])/RL[K-5];
	FF[K-5]=FF[K-5]+FTEMP;
	loc_vir= loc_vir+FTEMP*RS[K-5];
	RS[K]=CL[0][K]*CL[0][K]+CL[1][K]*CL[1][K]+CL[2][K]*CL[2][K];
	RL[K]=Math.sqrt(RS[K]);
	FF[K]=(C.AB3*Math.exp(-C.B3*RL[K])-C.AB4*Math.exp(-C.B4*RL[K]))/RL[K];
	loc_vir = loc_vir + FF[K]*RS[K];
      }
    } 

  /* CALCULATE X-COMPONENT FORCES */
    for (K = 0; K < 14; K++) 
      GG[K+1]=FF[K]*CL[0][K];

    G110=GG[10]+GG[1]*C.C1;
    G23=GG[2]+GG[3];
    G45=GG[4]+GG[5];
    Res1[1].value[0] = G110+GG[11]+GG[12]+C.C1*G23;
    Res2[1].value[0] = -G110-GG[13]-GG[14]-C.C1*G45;
    TT1=GG[1]*C.C2;
    TT=G23*C.C2+TT1;
    Res1[0].value[0] = GG[6]+GG[7]+GG[13]+TT+GG[4];
//    Res2[0][0] = GG[8]+GG[9]+GG[14]+TT+GG[5];
    Res1[2].value[0] = GG[8]+GG[9]+GG[14]+TT+GG[5];
    TT=G45*C.C2+TT1;
//    Res1[2][0] = -GG[6]-GG[8]-GG[11]-TT-GG[2];
    Res2[0].value[0] = -GG[6]-GG[8]-GG[11]-TT-GG[2];
    Res2[2].value[0] = -GG[7]-GG[9]-GG[12]-TT-GG[3];

  /* CALCULATE Y-COMPONENT FORCES */
    for(K = 0; K < 14; K++)
      GG[K+1]=FF[K]*CL[1][K];

    G110=GG[10]+GG[1]*C.C1;
    G23=GG[2]+GG[3];
    G45=GG[4]+GG[5];
    Res1[1].value[1] = G110+GG[11]+GG[12]+C.C1*G23;
    Res2[1].value[1] = -G110-GG[13]-GG[14]-C.C1*G45;
    TT1=GG[1]*C.C2;
    TT=G23*C.C2+TT1;
    Res1[0].value[1] = GG[6]+GG[7]+GG[13]+TT+GG[4];
//    Res2[0][1] = GG[8]+GG[9]+GG[14]+TT+GG[5];
    Res1[2].value[1] = GG[8]+GG[9]+GG[14]+TT+GG[5];
    TT=G45*C.C2+TT1;
//    Res1[2][1] = -GG[6]-GG[8]-GG[11]-TT-GG[2];
    Res2[0].value[1] = -GG[6]-GG[8]-GG[11]-TT-GG[2];
    Res2[2].value[1] = -GG[7]-GG[9]-GG[12]-TT-GG[3];

  /* CALCULATE Z-COMPONENT FORCES */
    for(K = 0; K < 14; K++)
      GG[K+1]=FF[K]*CL[2][K];

    G110=GG[10]+GG[1]*C.C1;
    G23=GG[2]+GG[3];
    G45=GG[4]+GG[5];
    Res1[1].value[2] = G110+GG[11]+GG[12]+C.C1*G23;
    Res2[1].value[2] = -G110-GG[13]-GG[14]-C.C1*G45;
    TT1=GG[1]*C.C2;
    TT=G23*C.C2+TT1;
    Res1[0].value[2] = GG[6]+GG[7]+GG[13]+TT+GG[4];
//    Res2[0][2] = GG[8]+GG[9]+GG[14]+TT+GG[5];
    Res1[2].value[2] = GG[8]+GG[9]+GG[14]+TT+GG[5];
    TT=G45*C.C2+TT1;
//    Res1[2][2] = -GG[6]-GG[8]-GG[11]-TT-GG[2];
    Res2[0].value[2] = -GG[6]-GG[8]-GG[11]-TT-GG[2];
    Res2[2].value[2] = -GG[7]-GG[9]-GG[12]-TT-GG[3];

    /* Update DESTination of calculations */
//    p1->update_forces(Res1);
//    p2->update_forces(Res2);
  } else {
    for (int i = 0; i < Res1.length; i++) { 
      Res1[i].clear();
      Res2[i].clear();
    }
  }
  return loc_vir;
}


void interf2(skratch_pad p1, skratch_pad p2){
  double incr; 
  vec Res1[] = new vec[3];
  Res1[0] = new vec();
  Res1[1] = new vec();
  Res1[2] = new vec();
  vec Res2[] = new vec[3];
  Res2[0] = new vec();
  Res2[1] = new vec();
  Res2[2] = new vec();

  incr = interf2_aux(p1,p2,Res1,Res2);
  p1.update_forces(Res1);
  p2.update_forces(Res2);
  VIR.addval(incr);
}

void loadData(){
  int i;
  h2o mol;
  skratch_pad p;

  for(i=0; i < numMol; i++){
    mol = getMolecule(i);
    p = getPad(i);
    p.read_data(mol);
  }
}

void storeData(int dest){
  int i;
  h2o p1;
  skratch_pad p2;

  for(i=0; i < numMol; i++){
    p1 = getMolecule(i);
    p2 = getPad(i);
    p1.updateFields(dest,p2);
  }
}

  void interfInnerSplit(int idx, int first, int num) { 
    skratch_pad p1, p2;
    if (num == 1) {
      p1 = getPad(idx);
      p2 = getPad(first);
      interf2(p1,p2);
    } else {
      interfInnerSplit(idx, first, num/2);
      interfInnerSplit(idx, first+num/2, (num - (num/2)));
    }
  }

  void interfInnerLoop(int idx){
    int i;
    skratch_pad p1, p2;

    for(i = idx+1; i < numMol; i++){
      p1 = getPad(idx);
      p2 = getPad(i);
      interf2(p1,p2);
    }
  }

  void interfInnerDispatch(int idx) { 
    if (water.parms.getPAR() == 0) {
      interfInnerLoop(idx);
    } else if (water.parms.getPAR() == 1) {
      interfInnerSplit(idx, idx+1, (numMol-idx)-1);
    } else if (water.parms.getPAR() == 2) {
      interfInnerThread t = new interfInnerThread(this, idx, idx+1, (numMol-idx)-1);
      t.inlineStart();
    } else {
      interfInnerThread t = new interfInnerThread(this, idx, idx+1, (numMol-idx)-1);
      t.start();
      try {
        t.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in INTERF\n");
      }
    }
  }

  void interfOuterLoop(){
    int i;
    for(i = 0; i < numMol-1; i++) {
      interfInnerDispatch(i);
    }
  }

  void interfOuterSplit(int first, int num) {
    if (num == 1) {
      interfInnerDispatch(first);
    } else {
      interfOuterSplit(first, num/2);
      interfOuterSplit(first+num/2, (num - (num/2)));
    }
  }

  void interfOuterDispatch() {
    if (water.parms.getPAR() == 0) {
      interfOuterLoop();
    } else if (water.parms.getPAR() == 1) {
      interfOuterSplit(0, numMol-1);
    } else if (water.parms.getPAR() == 2) {
      interfOuterThread t = new interfOuterThread(this, 0, numMol-1);
      t.inlineStart();
    } else {
      interfOuterThread t = new interfOuterThread(this, 0, numMol-1);
      t.start();
      try {
        t.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in INTERF\n");
      }
    }
  }

void INTERF(int DEST){
  loadData();
  interfOuterDispatch();
  storeData(DEST);
}

// --------------------------------------------------------------------
void SCALEFORCES(int Dest){
  int i;
  double HM, OM;

  for(i=0; i < numMol; i++){
    HM = water.parms.getFHM();
    OM = water.parms.getFOM();
    molecule[i].scaleMomenta(Dest,HM,OM);
  }
}

// --------------------------------------------------------------------
void DUMP(int iter){
  int i;

  System.out.print(" >> STEP: ");
  System.out.print(iter);
  System.out.print("\n");
  System.out.print(" NMOL: ");
  System.out.print(numMol);
  System.out.print("\n");
  System.out.print(" TVIR: ");
  System.out.print(TVIR);
  System.out.print("\n");
  System.out.print("  VIR: ");
  System.out.print(VIR.readval());
  System.out.print("\n");
  System.out.print(" FKIN: ");
  System.out.print(water.parms.getFKIN());
  System.out.print("\n");
  System.out.print(" TKIN: ");
  System.out.print(TKIN);
  System.out.print("\n");
  System.out.print("  KIN: ");
  KIN.print();
  System.out.print("\n");
  System.out.print(" FPOT: ");
  System.out.print(water.parms.FPOT);
  System.out.print("\n");
  System.out.print("  POT: ");
  POT.print();
  System.out.print("\n");
/*
  System.out.print("\n");
  for (i = 0; i < numMol; i++){
    molecule[i].dump();
    System.out.print("\n");
  }
*/
}
}

class potengInnerThread extends Thread {
  ensemble en;
  int first, num, idx;

  potengInnerThread(ensemble e, int i, int s, int n) {
    en = e;
    first = s;
    num = n;
    idx = i;
  }
  public void inlineStart() {
    if (num == 1) {
      vec tmp = new vec();
      en.interPoteng2Aux(en.pad[idx],en.pad[first],tmp);
      en.POT.add(tmp);
    } else {
      potengInnerThread l = new potengInnerThread(en, idx, first, num/2);
      potengInnerThread r = new potengInnerThread(en, idx, first+num/2, (num - (num/2)));
      l.inlineStart();
      r.inlineStart();
    }
  }
  public void run() {
    if (num == 1) {
      vec tmp = new vec();
      en.interPoteng2Aux(en.pad[idx],en.pad[first],tmp);
      en.POT.add(tmp);
    } else {
      potengInnerThread l = new potengInnerThread(en, idx, first, num/2);
      potengInnerThread r = new potengInnerThread(en, idx, first+num/2, (num - (num/2)));
      l.start();
      r.start();
      try {
        l.join();
        r.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in potengInnerThread::run\n");
      }
    }
  }
}

class potengOuterThread extends Thread {
  ensemble en;
  int first, num;

  potengOuterThread(ensemble e, int s, int n) {
    en = e;
    first = s;
    num = n;
  }
  public void inlineStart() {
    if (num == 1) {
      en.potengInnerDispatch(first);
    } else {
      potengOuterThread l = new potengOuterThread(en, first, num/2);
      potengOuterThread r = new potengOuterThread(en, first+num/2, (num - (num/2)));
      l.inlineStart();
      r.inlineStart();
    }
  }
  public void run() {
    if (num == 1) {
      en.potengInnerDispatch(first);
    } else {
      potengOuterThread l = new potengOuterThread(en, first, num/2);
      potengOuterThread r = new potengOuterThread(en, first+num/2, (num - (num/2)));
      l.start();
      r.start();
      try {
        l.join();
        r.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in potengOuterThread::run\n");
      }
    }
  }
}

class interfInnerThread extends Thread {
  ensemble en;
  int first, num, idx;

  interfInnerThread(ensemble e, int i, int s, int n) {
    en = e;
    first = s;
    num = n;
    idx = i;
  }
  public void inlineStart() {
    if (num == 1) {
      skratch_pad p1, p2;
      p1 = en.getPad(idx);
      p2 = en.getPad(first);
      en.interf2(p1,p2);
    } else {
      interfInnerThread l = new interfInnerThread(en, idx, first, num/2);
      interfInnerThread r = new interfInnerThread(en, idx, first+num/2, (num - (num/2)));
      l.inlineStart();
      r.inlineStart();
    }
  }
  public void run() {
    if (num == 1) {
      skratch_pad p1, p2;
      p1 = en.getPad(idx);
      p2 = en.getPad(first);
      en.interf2(p1,p2);
    } else {
      interfInnerThread l = new interfInnerThread(en, idx, first, num/2);
      interfInnerThread r = new interfInnerThread(en, idx, first+num/2, (num - (num/2)));
      l.start();
      r.start();
      try {
        l.join();
        r.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in interfInnerThread::run\n");
      }
    }
  }
}

class interfOuterThread extends Thread {
  ensemble en;
  int first, num;

  interfOuterThread(ensemble e, int s, int n) {
    en = e;
    first = s;
    num = n;
  }
  public void inlineStart() {
    if (num == 1) {
      en.interfInnerDispatch(first);
    } else {
      interfOuterThread l = new interfOuterThread(en, first, num/2);
      interfOuterThread r = new interfOuterThread(en, first+num/2, (num - (num/2)));
      l.inlineStart();
      r.inlineStart();
    }
  }
  public void run() {
    if (num == 1) {
      en.interfInnerDispatch(first);
    } else {
      interfOuterThread l = new interfOuterThread(en, first, num/2);
      interfOuterThread r = new interfOuterThread(en, first+num/2, (num - (num/2)));
      l.start();
      r.start();
      try {
        l.join();
        r.join();
      } catch (java.lang.InterruptedException e) {
        System.err.print("InterruptedException in interfOuterThread::run\n");
      }
    }
  }
}

// ----------------------------------------------------------------------------
// MAIN
// ----------------------------------------------------------------------------
class water { 
  public static simparm parms;
  public static ensemble liquid;
  
  public static void main(String args[]) throws java.io.IOException, java.io.FileNotFoundException { 

  int n; 
  long start_time, stop_time;
  double dticks;

  System.out.print(" >>> Program Started\n");
  start_time = System.currentTimeMillis();

  parms = new simparm();
  if (args.length == 0) { 
    System.out.print("usage: java main <input filename>\n");
    return;
  }
  parms.loadParms(args[0]);
  n = parms.getNMOL();
  liquid = new ensemble(parms);


  liquid.INITIA();
  liquid.INTRAF();
  liquid.computeVIR();
  liquid.INTERF(C.ACC);
  liquid.SCALEFORCES(C.ACC);

  parms.setNFMC(-1);
  parms.resetStat();
  liquid.MDMAIN();
/*
  liquid.DUMP(1);
*/
  stop_time = System.currentTimeMillis();

  dticks = 1000.0;
  System.out.print("TOTAL CPU USED = ");
  System.out.print(((stop_time-start_time)/dticks));
  System.out.print(" SECONDS\n");
  System.out.print("\nELAPSED CPU TIME IN SECONDS: ");
  System.out.print(((stop_time-start_time)/dticks));
  System.out.print("\n");
  System.out.print("\nMFLOP RATE: ");
  System.out.print(3432.550/((stop_time-start_time)/dticks));
  System.out.print("\n");
  System.out.print("\nTotal Time = ");
  System.out.print(((stop_time-start_time)/dticks));
  System.out.print("\n");
} 
}
