#ifndef _Included_jni_Errors_h
#define _Included_jni_Errors_h

/* Produces a fatal error in the virtual machine.  This error is unrecoverable
 * and program execution will terminate immediately.
 */
extern void jni_ThrowFatalError(JNIEnv *env, const char *message);

/* Produces and throw a general exception to the virtual machine.  This error may or may
 * not be recoverable outside of the native library.
 */
extern void jni_ThrowException(JNIEnv *env, const char *classname, const char *message);

/* Produces a specific NativeHookException containing an error code indicating what might
 * have gone wrong.
 */
extern void jni_ThrowNativeHookException(JNIEnv *env, short code, const char *message);

#endif
