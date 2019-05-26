#include <jni.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdlib.h>
#include <uiohook.h>

#include "jni_Errors.h"
#include "jni_Globals.h"

static char log_buffer[1024];
static bool logger(JNIEnv *env, unsigned int level, const char *format, va_list args) {
	bool status = false;

	int log_size = vsnprintf(log_buffer, sizeof(log_buffer), format, args);

	if (log_size >= 0) {
		jstring name = (*env).NewStringUTF("org.jnativehook");
		jstring message = (*env).NewStringUTF(log_buffer);

		jobject Logger_object = (*env).CallStaticObjectMethod(
				java_util_logging_Logger->cls,
				java_util_logging_Logger->getLogger,
				name);

		switch (level) {
			case LOG_LEVEL_DEBUG:
				(*env).CallVoidMethod(
					Logger_object,
					java_util_logging_Logger->fine,
					message);
				break;

			case LOG_LEVEL_INFO:
				(*env).CallVoidMethod(
					Logger_object,
					java_util_logging_Logger->info,
					message);
				break;

			case LOG_LEVEL_WARN:
				(*env).CallVoidMethod(
					Logger_object,
					java_util_logging_Logger->warning,
					message);
				break;

			case LOG_LEVEL_ERROR:
				(*env).CallVoidMethod(
					Logger_object,
					java_util_logging_Logger->severe,
					message);
				break;
		}

		(*env).DeleteLocalRef(name);
		(*env).DeleteLocalRef(message);
		(*env).DeleteLocalRef(Logger_object);

		status = true;
	}

	va_end(args);

	return status;
}

bool jni_Logger(JNIEnv *env, unsigned int level, const char *format, ...) {
	va_list args;
	va_start(args, format);

	return logger(env, level, format, args);
}

bool uiohook_LoggerCallback(unsigned int level, const char *format, ...) {
	bool status = false;

	JNIEnv *env = NULL;
	if ((*jvm).GetEnv((void **)(&env), jvm_attach_args.version) == JNI_OK) {
		va_list args;
		va_start(args, format);
		status = logger(env, level, format, args);
	}
	else {
		jni_Logger(env, LOG_LEVEL_ERROR, "%s [%u]: GetEnv failed!\n",
				__FUNCTION__, __LINE__);
	}

	return status;
}
