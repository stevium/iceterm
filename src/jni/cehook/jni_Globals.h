#ifndef _Included_jni_Globals_h
#define _Included_jni_Globals_h

#include <jni.h>
#include <cehook/include/uiohook.h>

// Globals for the jvm and jni version, this is set in JNILoad.c
extern JavaVM *jvm;
extern JavaVMAttachArgs jvm_attach_args;

/* JNI requires that all calls to FindClass be made during JNI_OnLoad to
 * prevent NoClassDefError from popping up when various Java security models
 * are used.  The following structures are used to better organize the data
 * associated with each class.  Note that these structs do not cover all of the
 * available methods for each class; only methods used in native code are
 * included.
 */
typedef struct _org_iceterm_cehook_GlobalScreen {
	jclass cls;
	jfieldID hookThread;
} GlobalScreen;

typedef struct org_iceterm_cehook_GlobalScreen$NativeHookThread {
	jclass cls;
	jmethodID dispatchEvent;
} NativeHookThread;

typedef struct _org_iceterm_cehook_NativeHookException {
	jclass cls;
	jmethodID init;
} NativeHookException;

typedef struct _org_iceterm_cehook_NativeMonitorInfo {
	jclass cls;
	jmethodID init;
} NativeMonitorInfo;

typedef struct _org_iceterm_cehook_NativeInputEvent {
	jclass cls;
	jfieldID when;
	jfieldID reserved;
	jmethodID init;
	jmethodID getID;
	jmethodID getModifiers;
} NativeInputEvent;

typedef struct _org_iceterm_cehook_keyboard_NativeKeyEvent {
	jclass cls;
	jmethodID init;
	NativeInputEvent *parent;
	jmethodID getKeyCode;
	jmethodID getKeyLocation;
	jmethodID getKeyChar;
} NativeKeyEvent;

typedef struct _org_iceterm_cehook_mouse_NativeMouseEvent {
	jclass cls;
	jmethodID init;
	NativeInputEvent *parent;
	jmethodID getButton;
	jmethodID getClickCount;
	jmethodID getX;
	jmethodID getY;
} NativeMouseEvent;

typedef struct _org_iceterm_cehook_mouse_NativeMouseWheelEvent {
	jclass cls;
	jmethodID init;
	NativeMouseEvent *parent;
	jmethodID getScrollAmount;
	jmethodID getScrollType;
	jmethodID getWheelRotation;
} NativeMouseWheelEvent;

typedef struct _java_lang_Object {
	jclass cls;
	jmethodID notify;
} Object;

typedef struct _java_lang_Integer {
	jclass cls;
	jmethodID init;
} Integer;

typedef struct _java_lang_System {
	jclass cls;
	jmethodID setProperty;
	jmethodID clearProperty;
} System;

typedef struct _java_util_logging_Logger {
	jclass cls;
	jmethodID getLogger;
	jmethodID fine;
	jmethodID info;
	jmethodID warning;
	jmethodID severe;
} Logger;

// Global variables for Java object struct representation.
extern GlobalScreen *org_iceterm_cehook_GlobalScreen;
extern NativeHookThread *org_iceterm_cehook_GlobalScreen$NativeHookThread;
extern NativeHookException *org_iceterm_cehook_NativeHookException;
extern NativeMonitorInfo *org_iceterm_cehook_NativeMonitorInfo;
extern NativeInputEvent *org_iceterm_cehook_NativeInputEvent;
extern NativeKeyEvent *org_iceterm_cehook_keyboard_NativeKeyEvent;
extern NativeMouseEvent *org_iceterm_cehook_mouse_NativeMouseEvent;
extern NativeMouseWheelEvent *org_iceterm_cehook_mouse_NativeMouseWheelEvent;
extern Object *java_lang_Object;
extern Integer *java_lang_Integer;
extern System *java_lang_System;
extern Logger *java_util_logging_Logger;

// Create all of the JNI global references used throughout the native library.
extern int jni_CreateGlobals(JNIEnv *env);

// Free all of the JNI globals created by the CreateJNIGlobals() function.
extern int jni_DestroyGlobals(JNIEnv *env);

#endif
