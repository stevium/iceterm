#include <stdlib.h>

#include "cehook/jni_Converter.h"
#include "cehook/jni_Globals.h"
#include "cehook/jni_Logger.h"
#include "cehook/jni_Errors.h"
#include "org_iceterm_cehook_NativeInputEvent.h"
#include "org_iceterm_cehook_keyboard_NativeKeyEvent.h"
#include "org_iceterm_cehook_GlobalScreen.h"

//JNIEXPORT void JNICALL Java_org_iceterm_cehook_GlobalScreen_00024NativeHookThread_enable(JNIEnv *env, jobject Thread_obj) {
////int status = hook_run();
////
////	switch (status) {
////		// System level errors.
////		case UIOHOOK_ERROR_OUT_OF_MEMORY:
////			jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
////			break;
////
////		// X11 specific errors.
////		case UIOHOOK_ERROR_X_OPEN_DISPLAY:
////			jni_ThrowNativeHookException(env, status, "Failed to open X11 display.");
////			break;
////
////		case UIOHOOK_ERROR_X_RECORD_NOT_FOUND:
////			jni_ThrowNativeHookException(env, status, "Unable to locate XRecord extension.");
////			break;
////
////		case UIOHOOK_ERROR_X_RECORD_ALLOC_RANGE:
////			jni_ThrowNativeHookException(env, status, "Unable to allocate XRecord range.");
////			break;
////
////		case UIOHOOK_ERROR_X_RECORD_CREATE_CONTEXT:
////			jni_ThrowNativeHookException(env, status, "Unable to allocate XRecord context.");
////			break;
////
////		case UIOHOOK_ERROR_X_RECORD_ENABLE_CONTEXT:
////			jni_ThrowNativeHookException(env, status, "Failed to enable XRecord context.");
////			break;
////
////
////		// Windows specific errors.
////		case UIOHOOK_ERROR_SET_WINDOWS_HOOK_EX:
////			jni_ThrowNativeHookException(env, status, "Failed to register low level windows hook.");
////			break;
////
////
////		// Darwin specific errors.
////		case UIOHOOK_ERROR_AXAPI_DISABLED:
////			jni_ThrowNativeHookException(env, status, "Failed to enable access for assistive devices.");
////			break;
////
////		case UIOHOOK_ERROR_CREATE_EVENT_PORT:
////			jni_ThrowNativeHookException(env, status, "Failed to create apple event port.");
////			break;
////
////		case UIOHOOK_ERROR_CREATE_RUN_LOOP_SOURCE:
////			jni_ThrowNativeHookException(env, status, "Failed to create apple run loop source.");
////			break;
////
////		case UIOHOOK_ERROR_GET_RUNLOOP:
////			jni_ThrowNativeHookException(env, status, "Failed to acquire apple run loop.");
////			break;
////
////		case UIOHOOK_ERROR_CREATE_OBSERVER:
////			jni_ThrowNativeHookException(env, status, "Failed to create apple run loop observer.");
////			break;
////
////
////		// Default error.
////		case UIOHOOK_FAILURE:
////			jni_ThrowNativeHookException(env, status, "An unknown hook error occurred.");
////			break;
////	}
//}
//
//JNIEXPORT void JNICALL Java_org_iceterm_cehook_GlobalScreen_00024NativeHookThread_disable(JNIEnv *env, jobject Thread_obj) {
////	int status = hook_stop();
////
////	// Only a handful of the total errors are possible on stop.
////	switch (status) {
////		// System level errors.
////		case UIOHOOK_ERROR_OUT_OF_MEMORY:
////			jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
////			break;
////
////		// Unix specific errors.
////		case UIOHOOK_ERROR_X_RECORD_GET_CONTEXT:
////			jni_ThrowNativeHookException(env, status, "Failed to get XRecord context.");
////			break;
////
////
////		// Windows specific errors.
////		// There are no Windows specific errors at this time.
////
////
////		// Darwin specific errors.
////		// There are no Darwin specific errors at this time.
////
////		// Default error.
////		case UIOHOOK_FAILURE:
////			jni_ThrowNativeHookException(env, status, "An unknown hook error occurred.");
////			break;
////	}
//}

/*
 * Class:     org_iceterm_cehook_GlobalScreen
 * Method:    postNativeEvent
 * Signature: (Lorg/jnativehook/NativeInputEvent;)V
 */
JNIEXPORT void JNICALL Java_org_iceterm_cehook_GlobalScreen_postNativeEvent(JNIEnv *env, jclass GlobalScreen_cls, jobject NativeInputEvent_obj)
{
//    MessageBox(0, "Posting Event", "global screen", MB_OK);
	// Get the event type.
	jint javaType = (*env).CallIntMethod(NativeInputEvent_obj, org_iceterm_cehook_NativeInputEvent->getID);

	// Allocate memory for the virtual event and set the type.
	uiohook_event virtualEvent;
	jni_ConvertToNativeType(javaType, &virtualEvent.type);

	// Convert Java event to virtual event.
	virtualEvent.mask = (unsigned int) (*env).CallIntMethod(NativeInputEvent_obj, org_iceterm_cehook_NativeInputEvent->getModifiers);

	switch (javaType) {
		case org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_TYPED:
			virtualEvent.data.keyboard.keychar = (*env).CallIntMethod(NativeInputEvent_obj, org_iceterm_cehook_keyboard_NativeKeyEvent->getKeyChar);
			virtualEvent.data.keyboard.keycode = VC_UNDEFINED;
			virtualEvent.data.keyboard.rawcode = 0x00;
			break;

		case org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_PRESSED:
		case org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_RELEASED:
			virtualEvent.data.keyboard.keychar = CHAR_UNDEFINED;
			virtualEvent.data.keyboard.keycode = (*env).CallIntMethod(NativeInputEvent_obj, org_iceterm_cehook_keyboard_NativeKeyEvent->getKeyCode);
			virtualEvent.data.keyboard.rawcode = 0x00;
			break;

		default:
			// TODO Should this thrown an exception?
			jni_Logger(env, LOG_LEVEL_WARN,	"%s [%u]: Invalid native event type! (%#X)\n",
					__FUNCTION__, __LINE__, javaType);
			break;
	}

    // Set the propagate flag from java.
    virtualEvent.reserved = (unsigned short) (*env).GetShortField(
            NativeInputEvent_obj,
            org_iceterm_cehook_NativeInputEvent->reserved);

	hook_post_event(&virtualEvent);
}
