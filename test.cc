#include "classlist.h"
#include "model.h"
#include "dmodel.h"
extern "C" {
#include "test.h"
}
#include <stdio.h>
#include "element.h"
#include "Hashtable.h"
#include "tmap.h"
#include <sys/time.h>


model * exportmodel;

void initializeanalysis() {
  exportmodel=new model("testabstract","testmodel","testspace","teststruct","testconcrete");
}


void doanalysis() {
  struct timeval begin,end;
  unsigned long t;
  gettimeofday(&begin,NULL);
  exportmodel->doabstraction();
  exportmodel->getdomainrelation()->fixstuff();
  exportmodel->docheck();
  exportmodel->doconcrete();
  gettimeofday(&end,NULL);
  t=(end.tv_sec-begin.tv_sec)*1000000+end.tv_usec-begin.tv_usec;

  printf("Time used for analysis(us): %ld\n",t);
}

void resetanalysis() {
  exportmodel->reset();
}

void addmapping(char *key, void * address,char *type) {
  Hashtable *env=exportmodel->gethashtable();
  env->put(key,new Element(address,exportmodel->getstructure(type)));//should be of badstruct
}

void addintmapping(char *key, int value) {
  Hashtable *env=exportmodel->gethashtable();
  env->put(key,new Element(value));//should be of badstruct
}

void *ourcalloc(size_t nmemb, size_t size) {
  typemap *tm=exportmodel->gettypemap();
  void *oc=calloc(nmemb,size);
  tm->allocate(oc,size*nmemb);
  return oc;
}

void *ourmalloc(size_t size) {
  typemap *tm=exportmodel->gettypemap();
  void *oc=malloc(size);
  tm->allocate(oc,size);
  return oc;
}

void ourfree(void *ptr) {
  typemap *tm=exportmodel->gettypemap();
  tm->deallocate(ptr);
  free(ptr);
}

void *ourrealloc(void *ptr, size_t size) {
  typemap *tm=exportmodel->gettypemap();
  void *orr=realloc(ptr,size);
  if (size==0) {
    tm->deallocate(ptr);
    return orr;
  }
  if (orr==NULL) {
    return orr;
  }
  tm->deallocate(ptr);
  tm->allocate(ptr,size);
}

void alloc(void *ptr,int size) {
  typemap *tm=exportmodel->gettypemap();
  tm->allocate(ptr,size);
}

void dealloc(void *ptr) {
  typemap *tm=exportmodel->gettypemap();
  tm->deallocate(ptr);
}