// DO NOT EDIT THIS FILE - it is machine generated -*- c++ -*-

#ifndef __edu_uci_ece_ac_time_HighResTime__
#define __edu_uci_ece_ac_time_HighResTime__

#pragma interface

#include <java/lang/Object.h>

extern "Java"
{
  namespace edu
  {
    namespace uci
    {
      namespace ece
      {
        namespace ac
        {
          namespace time
          {
            class HighResTime;
          }
        }
      }
    }
  }
};

class ::edu::uci::ece::ac::time::HighResTime : public ::java::lang::Object
{
public:
  virtual jlong getMilliSec () { return msec; }
  virtual void setMilliSec (jlong);
  virtual jlong getMicroSec () { return usec; }
  virtual void setMicroSec (jlong);
  virtual jlong getNanoSec () { return nsec; }
  virtual void setNanoSec (jlong);
  virtual void setTime (jlong, jlong, jlong);
  virtual void incrementBy (jlong, jlong, jlong);
  virtual ::edu::uci::ece::ac::time::HighResTime *add (::edu::uci::ece::ac::time::HighResTime *);
  virtual void reset ();
  virtual ::java::lang::String *toString ();
  virtual void printTo (::java::io::PrintStream *);
  HighResTime ();
  HighResTime (jlong, jlong, jlong);
  HighResTime (jlong, jlong);
private:
  jlong msec;
  jlong usec;
  jlong nsec;
public:

  static ::java::lang::Class class$;
};

#endif /* __edu_uci_ece_ac_time_HighResTime__ */