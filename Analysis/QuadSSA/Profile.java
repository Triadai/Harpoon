// -*-Mode: Java-*- 
// Profile.java -- Inserts profiling statements into QuadSSA CFG.
// Author: Mark Foltz <mfoltz@ai.mit.edu> 
// Maintainer: Mark Foltz <mfoltz@ai.mit.edu> 
// Version: 
// Created: <Tue Oct  6 12:41:25 1998> 
// Time-stamp: <1998-11-22 17:31:54 mfoltz> 
// Keywords: 

package harpoon.Analysis.QuadSSA;

import harpoon.ClassFile.*;
import harpoon.IR.QuadSSA.*;
import harpoon.Temp.Temp;
import harpoon.Util.Set;
import harpoon.Util.Util;
import harpoon.RunTime.Monitor;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Enumeration;

/**
 * 
 * @author  Mark A. Foltz <mfoltz@ai.mit.edu>
 * @version 
 */

public class Profile {

  private Profile() { }

  public static void optimize(HCode hc) {

    HMethod _method = hc.getMethod();
    HClass _class = _method.getDeclaringClass();
    Set W = new Set();

    Visitor v = new Visitor(W, _method, _class);

    Enumeration e;

    // put CALLs and NEWs on worklist
    for (e = hc.getElementsE(); e.hasMoreElements();) {
      Quad q = (Quad) e.nextElement();
      if (q instanceof CALL || q instanceof NEW) 
	W.push(q);
    }

    // Make sure METHODHEADER visited first 
    for (e = hc.getElementsE(); e.hasMoreElements();) {
      Quad q = (Quad) e.nextElement();
      if (q instanceof METHODHEADER) 
	q.visit(v);
    }
    
    // Grovel over worklist until empty
    while (!W.isEmpty()) {
      Quad q = (Quad) W.pull();
      q.visit(v);
    }
  }

  static class Visitor extends QuadVisitor {

    // worklist
    Set _W;

    // calling method, class, and this for this quad graph
    HMethod _method;
    HClass _class;
    Temp _this, _null_temp, _calling_method_name_temp, _calling_class_name_temp;

    // classes for constants
    HClass _java_lang_string;
    //    HClass _java_lang_integer;

    // profiling callbacks
    HMethod _call_profiling_method;
    HMethod _new_profiling_method;

    // static monitor implementation
    StaticMonitor _static_monitor;

    Visitor(Set W, HMethod M, HClass C) {

      this._W = W;
      this._method = M;
      this._class = C;

      _static_monitor = new StaticMonitor();

      _java_lang_string = HClass.forName("java.lang.String");
      //      _java_lang_integer = HClass.forName("java.lang.Integer");

      _null_temp = new Temp();
      _calling_method_name_temp = new Temp();
      _calling_class_name_temp = new Temp();

      // Get HMethods for profiling callbacks.
      HClass monitor = HClass.forName("harpoon.RunTime.Monitor");

//        HMethod[] monitor_methods = monitor.getMethods();
//        // dump method descriptiors
//        for (int i = 0; i < monitor_methods.length; i++) 
//  	System.out.println(monitor_methods[i].getDescriptor());

      HClass _java_lang_object = HClass.forName("java.lang.Object");
      try {

 	_call_profiling_method = 
	  monitor.getMethod("logCALL",
			    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)V");

 	_new_profiling_method = 
	  monitor.getMethod("logNEW",
			    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;I)V");

       } catch (Exception e) {
 	System.err.println("Couldn't find method in harpoon.RunTime.Monitor!!!!");
 	e.printStackTrace();
       }
      
    }

    // Do nothing to regular statements.
    public void visit(Quad q) { }

    // Insert quads calling the profiling procedure
    // before each CALL quad, and log some static info. 
    public void visit(CALL q) {

      // log static info
      _static_monitor.logMAYCALL(_class,_method,q.method.getDeclaringClass(),q.method);

      // Set up constants.

      Temp t1 = new Temp();
      CONST c1 = new CONST(q.getSourceElement(), t1,
			   q.method.getName(), _java_lang_string);

      Temp[] parameters = new Temp[4];

      // Set up other parameters
      parameters[1] = _calling_method_name_temp;
      parameters[3] = t1;

      // if this is a static method call then we need a null constant
      if (q.objectref == null || _this == null) {

	if (_this == null) parameters[0] = _null_temp;
	else parameters[0] = _this;
	if (q.objectref == null) parameters[2] = _null_temp;
	else parameters[2] = q.objectref; 

      } else {
	parameters[0] = _this;
	parameters[2] = q.objectref;	
      }

      CALL profiling_call = new CALL(q.getSourceElement(), _call_profiling_method,
				     null, parameters, null, new Temp(), false);

      Quad.addEdge(c1, 0, profiling_call, 0);

      // splice new quads into CFG
      splice(q, c1, profiling_call);

    }

    public void visit(NEW q) {

      Temp[] parameters = new Temp[5];

      Temp t1 = new Temp();
      CONST c1 = new CONST(q.getSourceElement(), t1,
			   new Integer(q.getLineNumber()), 
			   HClass.Int);

      parameters[1] = _calling_class_name_temp;
      parameters[2] = _calling_method_name_temp;
      parameters[3] = q.dst;
      parameters[4] = t1;
      
      if (_this == null) parameters[0] = _null_temp;
      else parameters[0] = _this;

      CALL profiling_call = new CALL(q.getSourceElement(), _new_profiling_method,
				     null, parameters, null, new Temp(), false);

      Quad.addEdge(c1,0,profiling_call,0);

      // splice new quad into CFG
      Util.assert(q.next().length==1);
      splice(q.next(0), c1, profiling_call);

    }

    public void visit(METHODHEADER q) {

      CONST _null_CONST = new CONST(q.getSourceElement(), _null_temp,
				    null, HClass.Void);

      CONST _calling_method_name_CONST = 
	new CONST(q.getSourceElement(), _calling_method_name_temp,
		  _method.getName(), _java_lang_string);

      CONST _calling_class_name_CONST =
	new CONST(q.getSourceElement(), _calling_class_name_temp,
		  _class.getName(), _java_lang_string);

      Quad.addEdge(_null_CONST,0,_calling_method_name_CONST,0);
      Quad.addEdge(_calling_method_name_CONST,0,_calling_class_name_CONST,0);

      Util.assert(q.next().length==1);
      splice(q.next(0), _null_CONST, _calling_class_name_CONST);

      // make sure _this is null for static methods
      if (_method.isStatic()) _this = null;
      else _this = q.def()[0];
      // System.err.println(q);
    }

    // Splice quads r to s before q in the CFG.  r is assumed
    // to have no predecessors and s no successors.
    private void splice(Quad q, Quad r, Quad s) {
      for (int i = 0; i < q.prev().length; i++) {
	Quad.addEdge(q.prev(i), q.prevEdge(i).which_succ(),
		     r, i);
      }
      Quad.addEdge(s, 0, q, 0);
    }

  }

  static class StaticMonitor {
    
    Properties _properties = new Properties();
    DataOutputStream _logstream;

    StaticMonitor() {
      try {
	System.runFinalizersOnExit(true);
	_properties.load(new FileInputStream("/home/mfoltz/Harpoon/Code/RunTime/Monitor.properties"));
	_logstream = new DataOutputStream(new FileOutputStream(_properties.getProperty("staticfile"),true));
      } catch (Exception e) { }
    }

    // static MAYCALL graph
    public void logMAYCALL(HClass sending_class, HMethod sending_method, 
			   HClass receiving_class, HMethod receiving_method) {
      try {
	_logstream.writeBytes("MAYCALL "+sending_class.getName()+" "+sending_method.getName()+" "+
			      receiving_class.getName()+" "+receiving_method.getName()+"\n");
      } catch (Exception e) { }
    }

    void classFinalizer() throws Throwable {
      _logstream.flush();
      _logstream.close();
    }

  }

}




			   


      
      

      
