#include <stdlib.h>

#include "cehook/jni_Converter.h"
#include "cehook/jni_Globals.h"
#include "cehook/jni_Logger.h"
#include "cehook/jni_Errors.h"
#include "org_iceterm_cehook_NativeInputEvent.h"
#include "org_iceterm_cehook_keyboard_NativeKeyEvent.h"
#include "org_iceterm_cehook_GlobalScreen.h"

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
