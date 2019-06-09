//
// Created by Milos on 5/26/19.
//

#include <jni.h>
#include <stdbool.h>
#include <uiohook.h>

#include "jni_Converter.h"
#include "jni_Errors.h"
#include "jni_Globals.h"
#include "jni_Logger.h"
#include "org_iceterm_cehook_NativeInputEvent.h"
#include "org_iceterm_cehook_keyboard_NativeKeyEvent.h"
#include "jni_EventDispatcher.h"

// Simple function to notify() the hook thread.
static inline void notifyHookThread(JNIEnv *env) {
    jobject hookThread_obj = (*env).GetStaticObjectField(
            java_lang_Object->cls,
            org_iceterm_cehook_GlobalScreen->hookThread);

    if (hookThread_obj != NULL) {
        (*env).MonitorEnter(hookThread_obj);
        (*env).CallVoidMethod(
                hookThread_obj,
                java_lang_Object->notify);
        (*env).MonitorExit(hookThread_obj);
    }
}

// NOTE: This function executes on the hook thread!  If you need to block
// please do so on another thread via your own event dispatcher.
void jni_EventDispatcher(uiohook_event * const event) {
    JNIEnv *env;
    if ((*jvm).GetEnv((void **)(&env), jvm_attach_args.version) == JNI_OK) {
        jobject NativeInputEvent_obj = NULL;
        jint location = org_iceterm_cehook_keyboard_NativeKeyEvent_LOCATION_UNKNOWN;
        switch (event->type) {
            case EVENT_HOOK_DISABLED:
            case EVENT_HOOK_ENABLED:
                notifyHookThread(env);
                return;


            case EVENT_KEY_PRESSED:
                // FIXME We really shouldnt be wrighting to that memory.
                if (jni_ConvertToJavaLocation(&(event->data.keyboard.keycode), &location) == JNI_OK) {
                    NativeInputEvent_obj = (*env).NewObject(
                            org_iceterm_cehook_keyboard_NativeKeyEvent->cls,
                            org_iceterm_cehook_keyboard_NativeKeyEvent->init,
                            org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_PRESSED,
                            (jint)	event->mask,
                            (jint)	event->data.keyboard.rawcode,
                            (jint)	event->data.keyboard.keycode,
                            (jchar)	org_iceterm_cehook_keyboard_NativeKeyEvent_CHAR_UNDEFINED,
                            location);
                }
                break;

            case EVENT_KEY_RELEASED:
                // FIXME We really shouldnt be wrighting to that memory.
                if (jni_ConvertToJavaLocation(&(event->data.keyboard.keycode), &location) == JNI_OK) {
                    NativeInputEvent_obj = (*env).NewObject(
                            org_iceterm_cehook_keyboard_NativeKeyEvent->cls,
                            org_iceterm_cehook_keyboard_NativeKeyEvent->init,
                            org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_RELEASED,
                            (jint)	event->mask,
                            (jint)	event->data.keyboard.rawcode,
                            (jint)	event->data.keyboard.keycode,
                            (jchar)	org_iceterm_cehook_keyboard_NativeKeyEvent_CHAR_UNDEFINED,
                            location);
                }
                break;

            case EVENT_KEY_TYPED:
                NativeInputEvent_obj = (*env).NewObject(
                        org_iceterm_cehook_keyboard_NativeKeyEvent->cls,
                        org_iceterm_cehook_keyboard_NativeKeyEvent->init,
                        org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_TYPED,
                        (jint)	event->mask,
                        (jint)	event->data.keyboard.rawcode,
                        (jint)	org_iceterm_cehook_keyboard_NativeKeyEvent_VC_UNDEFINED,
                        (jchar)	event->data.keyboard.keychar,
                        location);
                break;

            default:
                jni_Logger(env, LOG_LEVEL_INFO,	"%s [%u]: Unknown native event type: %#X.\n",
                           __FUNCTION__, __LINE__, event->type);
                break;
        }

        if (NativeInputEvent_obj != NULL) {
            // Set the private when field to the native event time.
            (*env).SetShortField(
                    NativeInputEvent_obj,
                    org_iceterm_cehook_NativeInputEvent->when,
                    (jlong)	event->time);

            // Dispatch the event.
            (*env).CallStaticVoidMethod(
                    org_iceterm_cehook_GlobalScreen$NativeHookThread->cls,
                    org_iceterm_cehook_GlobalScreen$NativeHookThread->dispatchEvent,
                    NativeInputEvent_obj);

            // Set the propagate flag from java.
            event->reserved = (unsigned short) (*env).GetShortField(
                    NativeInputEvent_obj,
                    org_iceterm_cehook_NativeInputEvent->reserved);

            // Make sure our object is garbage collected.
            (*env).DeleteLocalRef(NativeInputEvent_obj);
        }
    }
}
