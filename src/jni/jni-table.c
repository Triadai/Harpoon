/* JNI function dispatch table. */
#include <jni.h>

const struct JNINativeInterface FLEX_JNI_table = {
  0,
  
  0,
  0,
  0,
  FNI_GetVersion,
            
  FNI_DefineClass,
  FNI_FindClass,
  0,
  0,
  0,
  FNI_GetSuperclass,
  FNI_IsAssignableFrom,
  0,
            
  FNI_Throw,
  FNI_ThrowNew,
  FNI_ExceptionOccurred,
  FNI_ExceptionDescribe,
  FNI_ExceptionClear,
  FNI_FatalError,
  0,
  0,
            
  FNI_NewGlobalRef,
  FNI_DeleteGlobalRef,
  FNI_DeleteLocalRef,
  FNI_IsSameObject,
  0,
  0,
            
  FNI_AllocObject,
  FNI_NewObject,
  FNI_NewObjectV,
  FNI_NewObjectA,
            
  FNI_GetObjectClass,
  FNI_IsInstanceOf,
            
  FNI_GetMethodID,
            
  FNI_CallObjectMethod,
  FNI_CallObjectMethodV,
  FNI_CallObjectMethodA,
  FNI_CallBooleanMethod,
  FNI_CallBooleanMethodV,
  FNI_CallBooleanMethodA,
  FNI_CallByteMethod,
  FNI_CallByteMethodV,
  FNI_CallByteMethodA,
  FNI_CallCharMethod,
  FNI_CallCharMethodV,
  FNI_CallCharMethodA,
  FNI_CallShortMethod,
  FNI_CallShortMethodV,
  FNI_CallShortMethodA,
  FNI_CallIntMethod,
  FNI_CallIntMethodV,
  FNI_CallIntMethodA,
  FNI_CallLongMethod,
  FNI_CallLongMethodV,
  FNI_CallLongMethodA,
  FNI_CallFloatMethod,
  FNI_CallFloatMethodV,
  FNI_CallFloatMethodA,
  FNI_CallDoubleMethod,
  FNI_CallDoubleMethodV,
  FNI_CallDoubleMethodA,
  FNI_CallVoidMethod,
  FNI_CallVoidMethodV,
  FNI_CallVoidMethodA,
            
  FNI_CallNonvirtualObjectMethod,
  FNI_CallNonvirtualObjectMethodV,
  FNI_CallNonvirtualObjectMethodA,
  FNI_CallNonvirtualBooleanMethod,
  FNI_CallNonvirtualBooleanMethodV,
  FNI_CallNonvirtualBooleanMethodA,
  FNI_CallNonvirtualByteMethod,
  FNI_CallNonvirtualByteMethodV,
  FNI_CallNonvirtualByteMethodA,
  FNI_CallNonvirtualCharMethod,
  FNI_CallNonvirtualCharMethodV,
  FNI_CallNonvirtualCharMethodA,
  FNI_CallNonvirtualShortMethod,
  FNI_CallNonvirtualShortMethodV,
  FNI_CallNonvirtualShortMethodA,
  FNI_CallNonvirtualIntMethod,
  FNI_CallNonvirtualIntMethodV,
  FNI_CallNonvirtualIntMethodA,
  FNI_CallNonvirtualLongMethod,
  FNI_CallNonvirtualLongMethodV,
  FNI_CallNonvirtualLongMethodA,
  FNI_CallNonvirtualFloatMethod,
  FNI_CallNonvirtualFloatMethodV,
  FNI_CallNonvirtualFloatMethodA,
  FNI_CallNonvirtualDoubleMethod,
  FNI_CallNonvirtualDoubleMethodV,
  FNI_CallNonvirtualDoubleMethodA,
  FNI_CallNonvirtualVoidMethod,
  FNI_CallNonvirtualVoidMethodV,
  FNI_CallNonvirtualVoidMethodA,
            
  FNI_GetFieldID,
            
  FNI_GetObjectField,
  FNI_GetBooleanField,
  FNI_GetByteField,
  FNI_GetCharField,
  FNI_GetShortField,
  FNI_GetIntField,
  FNI_GetLongField,
  FNI_GetFloatField,
  FNI_GetDoubleField,
  FNI_SetObjectField,
  FNI_SetBooleanField,
  FNI_SetByteField,
  FNI_SetCharField,
  FNI_SetShortField,
  FNI_SetIntField,
  FNI_SetLongField,
  FNI_SetFloatField,
  FNI_SetDoubleField,
            
  FNI_GetStaticMethodID,
            
  FNI_CallStaticObjectMethod,
  FNI_CallStaticObjectMethodV,
  FNI_CallStaticObjectMethodA,
  FNI_CallStaticBooleanMethod,
  FNI_CallStaticBooleanMethodV,
  FNI_CallStaticBooleanMethodA,
  FNI_CallStaticByteMethod,
  FNI_CallStaticByteMethodV,
  FNI_CallStaticByteMethodA,
  FNI_CallStaticCharMethod,
  FNI_CallStaticCharMethodV,
  FNI_CallStaticCharMethodA,
  FNI_CallStaticShortMethod,
  FNI_CallStaticShortMethodV,
  FNI_CallStaticShortMethodA,
  FNI_CallStaticIntMethod,
  FNI_CallStaticIntMethodV,
  FNI_CallStaticIntMethodA,
  FNI_CallStaticLongMethod,
  FNI_CallStaticLongMethodV,
  FNI_CallStaticLongMethodA,
  FNI_CallStaticFloatMethod,
  FNI_CallStaticFloatMethodV,
  FNI_CallStaticFloatMethodA,
  FNI_CallStaticDoubleMethod,
  FNI_CallStaticDoubleMethodV,
  FNI_CallStaticDoubleMethodA,
  FNI_CallStaticVoidMethod,
  FNI_CallStaticVoidMethodV,
  FNI_CallStaticVoidMethodA,
            
  FNI_GetStaticFieldID,
            
  FNI_GetStaticObjectField,
  FNI_GetStaticBooleanField,
  FNI_GetStaticByteField,
  FNI_GetStaticCharField,
  FNI_GetStaticShortField,
  FNI_GetStaticIntField,
  FNI_GetStaticLongField,
  FNI_GetStaticFloatField,
  FNI_GetStaticDoubleField,
            
  FNI_SetStaticObjectField,
  FNI_SetStaticBooleanField,
  FNI_SetStaticByteField,
  FNI_SetStaticCharField,
  FNI_SetStaticShortField,
  FNI_SetStaticIntField,
  FNI_SetStaticLongField,
  FNI_SetStaticFloatField,
  FNI_SetStaticDoubleField,
            
  FNI_NewString,
  FNI_GetStringLength,
  FNI_GetStringChars,
  FNI_ReleaseStringChars,
            
  FNI_NewStringUTF,
  FNI_GetStringUTFLength,
  FNI_GetStringUTFChars,
  FNI_ReleaseStringUTFChars,
            
  FNI_GetArrayLength,
             
  FNI_NewObjectArray,
  FNI_GetObjectArrayElement,
  FNI_SetObjectArrayElement,
            
  FNI_NewBooleanArray,
  FNI_NewByteArray,
  FNI_NewCharArray,
  FNI_NewShortArray,
  FNI_NewIntArray,
  FNI_NewLongArray,
  FNI_NewFloatArray,
  FNI_NewDoubleArray,
            
  FNI_GetBooleanArrayElements,
  FNI_GetByteArrayElements,
  FNI_GetCharArrayElements,
  FNI_GetShortArrayElements,
  FNI_GetIntArrayElements,
  FNI_GetLongArrayElements,
  FNI_GetFloatArrayElements,
  FNI_GetDoubleArrayElements,
            
  FNI_ReleaseBooleanArrayElements,
  FNI_ReleaseByteArrayElements,
  FNI_ReleaseCharArrayElements,
  FNI_ReleaseShortArrayElements,
  FNI_ReleaseIntArrayElements,
  FNI_ReleaseLongArrayElements,
  FNI_ReleaseFloatArrayElements,
  FNI_ReleaseDoubleArrayElements,
            
  FNI_GetBooleanArrayRegion,
  FNI_GetByteArrayRegion,
  FNI_GetCharArrayRegion,
  FNI_GetShortArrayRegion,
  FNI_GetIntArrayRegion,
  FNI_GetLongArrayRegion,
  FNI_GetFloatArrayRegion,
  FNI_GetDoubleArrayRegion,
  FNI_SetBooleanArrayRegion,
  FNI_SetByteArrayRegion,
  FNI_SetCharArrayRegion,
  FNI_SetShortArrayRegion,
  FNI_SetIntArrayRegion,
  FNI_SetLongArrayRegion,
  FNI_SetFloatArrayRegion,
  FNI_SetDoubleArrayRegion,
            
  FNI_RegisterNatives,
  FNI_UnregisterNatives,
            
  FNI_MonitorEnter,
  FNI_MonitorExit,
            
  FNI_GetJavaVM,
};
