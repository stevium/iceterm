#include <jni.h>
#include <stdlib.h>

#include "jni_Errors.h"
#include "jni_Globals.h"
#include "jni_Logger.h"

GlobalScreen *org_iceterm_cehook_GlobalScreen = NULL;
NativeHookThread *org_iceterm_cehook_GlobalScreen$NativeHookThread = NULL;
NativeHookException *org_iceterm_cehook_NativeHookException = NULL;
NativeMonitorInfo *org_iceterm_cehook_NativeMonitorInfo = NULL;
NativeInputEvent *org_iceterm_cehook_NativeInputEvent = NULL;
NativeKeyEvent *org_iceterm_cehook_keyboard_NativeKeyEvent = NULL;
Object *java_lang_Object = NULL;
Integer *java_lang_Integer = NULL;
System *java_lang_System = NULL;
Logger *java_util_logging_Logger = NULL;

static int create_GlobalScreen(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the GlobalScreen Object.
	jclass GlobalScreen_class = (*env).FindClass("org/iceterm/cehook/GlobalScreen");
	if (GlobalScreen_class != NULL) {
		// Get the field ID for hookThread.
		jfieldID hookThread = (*env).GetStaticFieldID(GlobalScreen_class, "hookThread", "Lorg/iceterm/cehook/GlobalScreen$NativeHookThread;");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			org_iceterm_cehook_GlobalScreen = static_cast<GlobalScreen *>(malloc(sizeof(GlobalScreen)));
			if (org_iceterm_cehook_GlobalScreen != NULL) {
				// Populate our structure for later use.
				org_iceterm_cehook_GlobalScreen->cls = (jclass) (*env).NewGlobalRef(GlobalScreen_class);
				org_iceterm_cehook_GlobalScreen->hookThread = hookThread;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static void destroy_GlobalScreen(JNIEnv *env) {
	if (org_iceterm_cehook_GlobalScreen != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (org_iceterm_cehook_GlobalScreen->cls != NULL) {
			(*env).DeleteGlobalRef(org_iceterm_cehook_GlobalScreen->cls);
		}

		// Free struct memory.
		free(org_iceterm_cehook_GlobalScreen);
		org_iceterm_cehook_GlobalScreen = NULL;
	}
}


static int create_NativeHookThread(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the GlobalScreen Object.
	jclass NativeHookThread_class = (*env).FindClass("org/iceterm/cehook/GlobalScreen$NativeHookThread");
	if (NativeHookThread_class != NULL) {
		// Get the method ID for GlobalScreen.dispatchEvent().
		jmethodID dispatchEvent = (*env).GetStaticMethodID(NativeHookThread_class, "dispatchEvent", "(Lorg/iceterm/cehook/NativeInputEvent;)V");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			org_iceterm_cehook_GlobalScreen$NativeHookThread = static_cast<NativeHookThread *>(malloc(
                    sizeof(NativeHookThread)));
			if (org_iceterm_cehook_GlobalScreen$NativeHookThread != NULL) {
				// Populate our structure for later use.
				org_iceterm_cehook_GlobalScreen$NativeHookThread->cls = (jclass) (*env).NewGlobalRef(NativeHookThread_class);
				org_iceterm_cehook_GlobalScreen$NativeHookThread->dispatchEvent = dispatchEvent;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static void destroy_NativeHookThread(JNIEnv *env) {
	if (org_iceterm_cehook_GlobalScreen != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (org_iceterm_cehook_GlobalScreen$NativeHookThread->cls != NULL) {
			(*env).DeleteGlobalRef(org_iceterm_cehook_GlobalScreen$NativeHookThread->cls);
		}

		// Free struct memory.
		free(org_iceterm_cehook_GlobalScreen$NativeHookThread);
		org_iceterm_cehook_GlobalScreen$NativeHookThread = NULL;
	}
}


static int create_NativeHookException(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the NativeHookException Object.
	jclass NativeHookException_class = (*env).FindClass("org/iceterm/cehook/NativeHookException");
	if (NativeHookException_class != NULL) {
		// Get the method ID for NativeInputEvent constructor.
		jmethodID init = (*env).GetMethodID(NativeHookException_class, "<init>", "(ILjava/lang/String;)V");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			org_iceterm_cehook_NativeHookException = static_cast<NativeHookException *>(malloc(sizeof(NativeInputEvent)));
			if (org_iceterm_cehook_NativeHookException != NULL) {
				// Populate our structure for later use.
				org_iceterm_cehook_NativeHookException->cls = (jclass) (*env).NewGlobalRef(NativeHookException_class);
				org_iceterm_cehook_NativeHookException->init = init;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static void destroy_NativeHookException(JNIEnv *env) {
	if (org_iceterm_cehook_NativeHookException != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (org_iceterm_cehook_NativeHookException->cls != NULL) {
			(*env).DeleteGlobalRef(org_iceterm_cehook_NativeHookException->cls);
		}

		// Free struct memory.
		free(org_iceterm_cehook_NativeHookException);
		org_iceterm_cehook_NativeHookException = NULL;
	}
}

static int create_NativeInputEvent(JNIEnv *env) {
    int status = JNI_ERR;

    // Class and Constructor for the NativeInputEvent Object.
    jclass NativeInputEvent_class = (*env).FindClass("org/iceterm/cehook/NativeInputEvent");
    if (NativeInputEvent_class != NULL) {
        // Get the field ID for NativeInputEvent.when.
        jfieldID when = (*env).GetFieldID(NativeInputEvent_class, "when", "J");

        // Get the field ID for NativeInputEvent.reserved.
        jfieldID reserved = (*env).GetFieldID(NativeInputEvent_class, "reserved", "S");

        // Get the method ID for NativeInputEvent constructor.
        jmethodID init = (*env).GetMethodID(NativeInputEvent_class, "<init>", "(Ljava/lang/Class;II)V");

        // Get the method ID for NativeInputEvent.getID().
        jmethodID getID = (*env).GetMethodID(NativeInputEvent_class, "getID", "()I");

        // Get the method ID for NativeInputEvent.getModifiers().
        jmethodID getModifiers = (*env).GetMethodID(NativeInputEvent_class, "getModifiers", "()I");

        if ((*env).ExceptionCheck() == JNI_FALSE) {
            org_iceterm_cehook_NativeInputEvent = static_cast<NativeInputEvent *>(malloc(sizeof(NativeInputEvent)));
            if (org_iceterm_cehook_NativeInputEvent != NULL) {
                // Populate our structure for later use.
                org_iceterm_cehook_NativeInputEvent->cls = (jclass) (*env).NewGlobalRef(NativeInputEvent_class);
                org_iceterm_cehook_NativeInputEvent->when = when;
                org_iceterm_cehook_NativeInputEvent->reserved = reserved;
                org_iceterm_cehook_NativeInputEvent->init = init;
                org_iceterm_cehook_NativeInputEvent->getID = getID;
                org_iceterm_cehook_NativeInputEvent->getModifiers = getModifiers;

                status = JNI_OK;
            }
            else {
                jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
                status = JNI_ENOMEM;
            }
        }
    }

    return status;
}

static void destroy_NativeInputEvent(JNIEnv *env) {
	if (org_iceterm_cehook_NativeInputEvent != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (org_iceterm_cehook_NativeInputEvent->cls != NULL) {
			(*env).DeleteGlobalRef(org_iceterm_cehook_NativeInputEvent->cls);
		}

		// Free struct memory.
		free(org_iceterm_cehook_NativeInputEvent);
		org_iceterm_cehook_NativeInputEvent = NULL;
	}
}


static int create_NativeKeyEvent(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the NativeKeyEvent Object.
	jclass NativeKeyEvent_class = (*env).FindClass("org/iceterm/cehook/keyboard/NativeKeyEvent");
	if (NativeKeyEvent_class != NULL) {
		// Get the method ID for NativeKeyEvent constructor.
		jmethodID init = (*env).GetMethodID(NativeKeyEvent_class, "<init>", "(IIIICI)V");

		// Get the method ID for NativeKeyEvent.getKeyCode().
		jmethodID getKeyCode = (*env).GetMethodID(NativeKeyEvent_class, "getKeyCode", "()I");

		// Get the method ID for NativeKeyEvent.getKeyLocation().
		jmethodID getKeyLocation = (*env).GetMethodID(NativeKeyEvent_class, "getKeyLocation", "()I");

		// Get the method ID for NativeKeyEvent.getKeyChar().
		jmethodID getKeyChar = (*env).GetMethodID(NativeKeyEvent_class, "getKeyChar", "()C");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			org_iceterm_cehook_keyboard_NativeKeyEvent = static_cast<NativeKeyEvent *>(malloc(sizeof(NativeKeyEvent)));
			if (org_iceterm_cehook_keyboard_NativeKeyEvent != NULL) {
				// Populate our structure for later use.
				org_iceterm_cehook_keyboard_NativeKeyEvent->cls = (jclass) (*env).NewGlobalRef(NativeKeyEvent_class);
				org_iceterm_cehook_keyboard_NativeKeyEvent->parent = org_iceterm_cehook_NativeInputEvent;
				org_iceterm_cehook_keyboard_NativeKeyEvent->init = init;
				org_iceterm_cehook_keyboard_NativeKeyEvent->getKeyCode = getKeyCode;
				org_iceterm_cehook_keyboard_NativeKeyEvent->getKeyLocation = getKeyLocation;
				org_iceterm_cehook_keyboard_NativeKeyEvent->getKeyChar = getKeyChar;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static void destroy_NativeKeyEvent(JNIEnv *env) {
	if (org_iceterm_cehook_keyboard_NativeKeyEvent != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (org_iceterm_cehook_keyboard_NativeKeyEvent->cls != NULL) {
			(*env).DeleteGlobalRef(org_iceterm_cehook_keyboard_NativeKeyEvent->cls);
		}

		// Free struct memory.
		free(org_iceterm_cehook_keyboard_NativeKeyEvent);
		org_iceterm_cehook_keyboard_NativeKeyEvent = NULL;
	}
}



static int create_Object(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the Object object.
	jclass Object_class = (*env).FindClass("java/lang/Object");
	if (Object_class != NULL) {
		// Get the method ID for Object.notify().
		jmethodID notify = (*env).GetMethodID(Object_class, "notify", "()V");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			java_lang_Object = static_cast<Object *>(malloc(sizeof(Object)));
			if (java_lang_Object != NULL) {
				// Populate our structure for later use.
				java_lang_Object->cls = (jclass) (*env).NewGlobalRef(Object_class);
				java_lang_Object->notify = notify;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static void destroy_Object(JNIEnv *env) {
	if (java_lang_Object != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (java_lang_Object->cls != NULL) {
			(*env).DeleteGlobalRef(java_lang_Object->cls);
		}

		// Free struct memory.
		free(java_lang_Object);
		java_lang_Object = NULL;
	}
}

static int create_Integer(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the Object object.
	jclass Integer_class = (*env).FindClass("java/lang/Integer");
	if (Integer_class != NULL) {
		// Get the method ID for NativeMouseWheelEvent constructor.
		jmethodID init = (*env).GetMethodID(Integer_class, "<init>", "(I)V");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			java_lang_Integer = static_cast<Integer *>(malloc(sizeof(Integer)));
			if (java_lang_Integer != NULL) {
				// Populate our structure for later use.
				java_lang_Integer->cls = (jclass) (*env).NewGlobalRef(Integer_class);
				java_lang_Integer->init = init;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static void destroy_Integer(JNIEnv *env) {
	if (java_lang_Integer != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (java_lang_Integer->cls != NULL) {
			(*env).DeleteGlobalRef(java_lang_Integer->cls);
		}

		// Free struct memory.
		free(java_lang_Integer);
		java_lang_Integer = NULL;
	}
}

static inline int create_System(JNIEnv *env) {
	int status = JNI_ERR;

	// Class and Constructor for the System Object.
	jclass System_class = (*env).FindClass("java/lang/System");
	if (System_class != NULL) {
		// Get the method ID for System.setProperty().
		jmethodID setProperty = (*env).GetStaticMethodID(System_class, "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");

		// Get the method ID for System.clearProperty().
		jmethodID clearProperty = (*env).GetStaticMethodID(System_class, "clearProperty", "(Ljava/lang/String;)Ljava/lang/String;");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			java_lang_System = static_cast<System *>(malloc(sizeof(System)));
			if (java_lang_System != NULL) {
				// Populate our structure for later use.
				java_lang_System->cls = (jclass) (*env).NewGlobalRef(System_class);
				java_lang_System->setProperty = setProperty;
				java_lang_System->clearProperty = clearProperty;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static inline void destroy_System(JNIEnv *env) {
	if (java_lang_System != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (java_lang_System->cls != NULL) {
			(*env).DeleteGlobalRef(java_lang_System->cls);
		}

		// Free struct memory.
		free(java_lang_System);
		java_lang_System = NULL;
	}
}



static inline int create_Logger(JNIEnv *env) {
	int status = JNI_ERR;

	// Class for the Logger object.
	jclass Logger_class = (*env).FindClass("java/util/logging/Logger");
	if (Logger_class != NULL) {
		// Get the method ID for Logger.getLogger().
		jmethodID getLogger = (*env).GetStaticMethodID(Logger_class, "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;");

		// Get the method ID for Logger.fine().
		jmethodID fine = (*env).GetMethodID(Logger_class, "fine", "(Ljava/lang/String;)V");

		// Get the method ID for Logger.info().
		jmethodID info = (*env).GetMethodID(Logger_class, "info", "(Ljava/lang/String;)V");

		// Get the method ID for Logger.warning().
		jmethodID warning = (*env).GetMethodID(Logger_class, "warning", "(Ljava/lang/String;)V");

		// Get the method ID for Logger.severe().
		jmethodID severe = (*env).GetMethodID(Logger_class, "severe", "(Ljava/lang/String;)V");

		if ((*env).ExceptionCheck() == JNI_FALSE) {
			java_util_logging_Logger = static_cast<Logger *>(malloc(sizeof(Logger)));
			if (java_util_logging_Logger != NULL) {
				// Populate our structure for later use.
				java_util_logging_Logger->cls = (jclass) (*env).NewGlobalRef(Logger_class);
				java_util_logging_Logger->getLogger = getLogger;
				java_util_logging_Logger->fine = fine;
				java_util_logging_Logger->info = info;
				java_util_logging_Logger->warning = warning;
				java_util_logging_Logger->severe = severe;

				status = JNI_OK;
			}
			else {
				jni_ThrowException(env, "java/lang/OutOfMemoryError", "Failed to allocate native memory.");
				status = JNI_ENOMEM;
			}
		}
	}

	return status;
}

static inline void destroy_Logger(JNIEnv *env) {
	if (java_util_logging_Logger != NULL) {
		// The class *should* never be null if the struct was allocated, but we will check anyway.
		if (java_util_logging_Logger->cls != NULL) {
			(*env).DeleteGlobalRef(java_util_logging_Logger->cls);
		}

		// Free struct memory.
		free(java_util_logging_Logger);
		java_util_logging_Logger = NULL;
	}
}

int jni_CreateGlobals(JNIEnv *env) {
	int status = create_GlobalScreen(env);

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_NativeHookThread(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_NativeHookException(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_NativeInputEvent(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_NativeKeyEvent(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_Object(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_Integer(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_System(env);
	}

	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_FALSE) {
		status = create_Logger(env);
	}

	// Check and make sure we don't have a pending exception and a JNI_OK status.
	if (status == JNI_OK && (*env).ExceptionCheck() == JNI_TRUE) {
		status = JNI_ERR;
	}

	return status;
}

int jni_DestroyGlobals(JNIEnv *env) {
	destroy_GlobalScreen(env);
	destroy_NativeHookThread(env);
	destroy_NativeHookException(env);
	destroy_NativeInputEvent(env);
	destroy_NativeKeyEvent(env);
	destroy_Object(env);
	destroy_Integer(env);
	destroy_System(env);
	destroy_Logger(env);

	return JNI_OK;
}
