#ifndef _Included_jni_EventDispathcer_h
#define _Included_jni_EventDispathcer_h

#include <uiohook.h>

// This is a simple forwarding function to the Java event dispatcher.
extern void jni_EventDispatcher(uiohook_event * const event);

#endif

