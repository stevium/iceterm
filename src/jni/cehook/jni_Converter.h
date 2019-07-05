#ifndef _Included_jni_Converter_h
#define _Included_jni_Converter_h

#include <jni.h>
#include <uiohook.h>

extern jint jni_ConvertToJavaType(event_type nativeType, jint *javaType);

extern jint jni_ConvertToNativeType(jint javaType, event_type *nativeType);

extern jint jni_ConvertToJavaLocation(unsigned short int *nativeKeyCode, jint *javaKeyLocation);

#endif
