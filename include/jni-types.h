/* Java Native Interface header file.  C. Scott Ananian. */
/* Implemented from the JNI spec, v 1.1 */

#ifndef INCLUDED_JNI_TYPES_H
#define INCLUDED_JNI_TYPES_H

#include <sys/types.h>

/* java primitive types and their machine-dependent native equivalents */
typedef u_int8_t  jboolean;
typedef   int8_t  jbyte;
typedef u_int16_t jchar;
typedef   int16_t jshort;
typedef   int32_t jint;
typedef   int64_t jlong;

typedef    float  jfloat;
typedef   double  jdouble;

/* the jsize integer type is used to describe cardinal indices and sizes */
typedef jint jsize;

/* reference types */
typedef void * jobject;
typedef jobject jclass;
typedef jobject jstring;
typedef jobject jarray;
typedef jobject jthrowable;
typedef jarray jobjectArray;
typedef jarray jbooleanArray;
typedef jarray jbyteArray;
typedef jarray jcharArray;
typedef jarray jshortArray;
typedef jarray jintArray;
typedef jarray jlongArray;
typedef jarray jfloatArray;
typedef jarray jdoubleArray;
/* you can be a bit more clever in c++, but i rather dislike c++ */

/* field and method IDs are regular C pointer types. */
struct _jfieldID; /* opaque structure */
typedef struct _jfieldID *jfieldID; /* field IDs */

struct _jmethodID; /* opaque structure */
typedef struct _jmethodID *jmethodID; /* method IDs */

/* the jvalue union type is used as the element type in argument arrays. */
typedef union jvalue {
  jboolean z;
  jbyte    b;
  jchar    c;
  jshort   s;
  jint     i;
  jlong    j;
  jfloat   f;
  jdouble  d;
  jobject  l;
} jvalue;

/* each function is accessible at a fixed offset through the JNIEnv argument.
 * The JNIEnv type is a pointer to a structure storing all JNI function
 * pointers. */
typedef const struct JNINativeInterface *JNIEnv;

/* register natives... */
typedef struct {
  char *name;
  char *signature;
  void *fnPtr;
} JNINativeMethod;

#endif /* INCLUDED_JNI_TYPES_H */
