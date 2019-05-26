#include <jni.h>
#include <uiohook.h>

#include "jni_Converter.h"
#include "org_jnativehook_NativeInputEvent.h"
#include "org_jnativehook_keyboard_NativeKeyEvent.h"

jint jni_ConvertToJavaType(event_type nativeType, jint *javaType) {
	jint status = JNI_ERR;

	if (javaType != NULL) {
		switch (nativeType) {
			case EVENT_KEY_TYPED:
			case EVENT_KEY_PRESSED:
			case EVENT_KEY_RELEASED:
				// 3 = EVENT_HOOK_ENABLED + EVENT_HOOK_DISABLED + UNDEFINED.
				*javaType = org_jnativehook_keyboard_NativeKeyEvent_NATIVE_KEY_FIRST + (nativeType - 3);
				status = JNI_OK;
				break;

			default:
				*javaType = 0;
				break;
		}
	}

	return status;
}


jint jni_ConvertToNativeType(jint javaType, event_type *nativeType) {
	jint status = JNI_ERR;

	if (nativeType != NULL) {
		switch (javaType) {
			case org_jnativehook_keyboard_NativeKeyEvent_NATIVE_KEY_TYPED:
			case org_jnativehook_keyboard_NativeKeyEvent_NATIVE_KEY_PRESSED:
			case org_jnativehook_keyboard_NativeKeyEvent_NATIVE_KEY_RELEASED:
				*nativeType = static_cast<event_type>((javaType + 3) -
                                                      org_jnativehook_keyboard_NativeKeyEvent_NATIVE_KEY_FIRST);
				status = JNI_OK;
				break;

			default:
				*nativeType = static_cast<event_type>(0);
				break;
		}
	}

	return status;
}

jint jni_ConvertToJavaLocation(unsigned short int *nativeKeyCode, jint *javaKeyLocation) {
	jint status = JNI_ERR;

	if (nativeKeyCode != NULL && javaKeyLocation != NULL) {
		switch (*nativeKeyCode) {
			case VC_SHIFT_L:
			case VC_CONTROL_L:
			case VC_ALT_L:
			case VC_META_L:
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_LEFT;
				break;

			case VC_SHIFT_R:
			case VC_CONTROL_R:
			case VC_ALT_R:
				*nativeKeyCode ^= 0x0E00;
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_RIGHT;
				break;

			case VC_META_R:
				*nativeKeyCode -= 1;
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_RIGHT;
				break;

			case VC_KP_COMMA:
				*nativeKeyCode = VC_COMMA;
				/* fall-thru */

			case VC_NUM_LOCK:
			case VC_KP_SEPARATOR:
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_NUMPAD;
				break;

			case VC_KP_ENTER:
			case VC_KP_MULTIPLY:
			case VC_KP_ADD:
			case VC_KP_SUBTRACT:
			case VC_KP_DIVIDE:

			case VC_KP_DOWN:
			case VC_KP_LEFT:
			case VC_KP_CLEAR:
			case VC_KP_RIGHT:
			case VC_KP_UP:
				*nativeKeyCode ^= 0x0E00;
                *javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_NUMPAD;
                break;

			case VC_KP_0:
                *nativeKeyCode = VC_0;
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_NUMPAD;
				break;

			case VC_KP_1:
			case VC_KP_2:
			case VC_KP_3:
				*nativeKeyCode -= ((VC_KP_1 - VC_1) - (VC_KP_4 - VC_4) ); // 0x4D - 0x46
				// FIXME Should this fall though?
				/* fall-thru */

			case VC_KP_4:
			case VC_KP_5:
			case VC_KP_6:
				*nativeKeyCode -= ((VC_KP_4 - VC_4) - (VC_KP_7 - VC_7) ); // 0x46 - 0x3F
				// FIXME Should this fall though?
				/* fall-thru */

			case VC_KP_7:
			case VC_KP_8:
			case VC_KP_9:
				*nativeKeyCode -= (VC_KP_7 - VC_7); // 0x3F
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_NUMPAD;
				break;

			case VC_KP_END:
			case VC_KP_PAGE_DOWN:
			case VC_KP_HOME:
			case VC_KP_PAGE_UP:
			case VC_KP_INSERT:
			case VC_KP_DELETE:
				*nativeKeyCode ^= 0xE000;
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_NUMPAD;
				break;

			default:
				*javaKeyLocation = org_jnativehook_keyboard_NativeKeyEvent_LOCATION_STANDARD;
				break;
		}

		status = JNI_OK;
	}

	return status;
}
