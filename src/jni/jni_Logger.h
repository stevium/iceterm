#ifndef _Included_jni_Logger_h
#define _Included_jni_Logger_h

#include <stdarg.h>
#include <stdbool.h>
#include <uiohook.h>

extern bool jni_Logger(JNIEnv *env, unsigned int level, const char *format, ...);

extern bool uiohook_LoggerCallback(unsigned int level, const char *format, ...);

#endif
