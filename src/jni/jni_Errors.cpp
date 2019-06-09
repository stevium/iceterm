#include <jni.h>
#include <stdlib.h>

#include "jni_Errors.h"
#include "jni_Globals.h"
#include "jni_Logger.h"

void jni_ThrowFatalError(JNIEnv *env, const char *message) {
	// Throw a fatal error to the JVM.
	(*env).FatalError(message);

	exit(EXIT_FAILURE);
}

void jni_ThrowException(JNIEnv *env, const char *classname, const char *message) {
	// Locate our exception class.
	jclass Exception_class = (*env).FindClass(classname);
	if (Exception_class != NULL) {
		(*env).ThrowNew(Exception_class, message);
		(*env).DeleteLocalRef(Exception_class);
	}
	else {
		// Throw a ClassNotFoundException if we could not locate the exception class above.
		Exception_class = (*env).FindClass("java/lang/ClassNotFoundException");
		if (Exception_class != NULL) {
			(*env).ThrowNew(Exception_class, classname);
			(*env).DeleteLocalRef(Exception_class);
		}
		else {
			jni_ThrowFatalError(env, "Failed to locate core class: java.lang.ClassNotFoundException");
		}
	}
}

void jni_ThrowNativeHookException(JNIEnv *env, short code, const char *message) {
	jobject Exception_object = (*env).NewObject(org_iceterm_cehook_NativeHookException->cls,
			org_iceterm_cehook_NativeHookException->init, (jint) code, (*env).NewStringUTF(message));
	(*env).Throw((jthrowable) Exception_object);
	(*env).DeleteLocalRef(Exception_object);
}
