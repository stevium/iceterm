#include <jni.h>

#include "jni_Errors.h"
#include "jni_EventDispatcher.h"
#include "jni_Globals.h"
#include "jni_Logger.h"

// JNI Related global references.
JavaVM *jvm;
JavaVMAttachArgs jvm_attach_args = {
	JNI_VERSION_1_4,
	"JNativeHook Library",
	NULL
};

// JNI entry point, This is executed when the Java virtual machine attaches to the native library.
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
	// Grab the currently running virtual machine so we can attach to it in
	// functions that are not called from java.
	jvm = vm;
	JNIEnv *env = NULL;
	if ((*jvm).GetEnv((void **)(&env), jvm_attach_args.version) == JNI_OK) {
		// Create all the global class references onload to prevent class loader
		// issues with JNLP and some IDE's.
		if (jni_CreateGlobals(env) == JNI_OK) {
			// Set Java logger for native code messages.
//			hook_set_logger_proc(&uiohook_LoggerCallback);

			// Set the hook callback function to dispatch events.
//			hook_set_dispatch_proc(&jni_EventDispatcher);
		}
	}
	else {
		jni_ThrowFatalError(env, "Failed to acquire JNI interface pointer");
	}

	return jvm_attach_args.version;
}

// JNI exit point, This is executed when the Java virtual machine detaches from the native library.
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
	// Unset the hook callback function to dispatch events.
//	hook_set_dispatch_proc(NULL);

	// Unset Java logger for native code messages.
//	hook_set_logger_proc(NULL);

	// Grab the currently JNI interface pointer so we can cleanup the
	// system properties set on load.
	JNIEnv *env = NULL;
	if ((*jvm).GetEnv((void **)(&env), jvm_attach_args.version) == JNI_OK) {
		// It is not critical that these values are cleared so no exception
		// will be thrown if this does not succeed.

		// Cleanup JNI global memory.
		jni_DestroyGlobals(env);
	}
}
