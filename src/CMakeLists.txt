cmake_minimum_required(VERSION 3.14)
project(iceterm)

set(CMAKE_CXX_STANDARD 14)

include_directories(jni)
include_directories(jni/include)
include_directories(jni/cehook/include)

set(ICETERM_SOURCES
    jni/cehook/cepipe/pipe_server.cpp
    jni/cehook/cepipe/pipe_server.h
    jni/cehook/post_event.cpp
    jni/cehook/input_helper.h
    jni/cehook/input_helper.cpp
    jni/cehook/logger.h
    jni/cehook/logger.cpp
    jni/org_iceterm_ceintegration_GuiMacroExecutor.cpp
    jni/org_iceterm_ceintegration_GuiMacroExecutor.h
    jni/org_iceterm_cehook_GlobalScreen.cpp
    jni/org_iceterm_cehook_ConEmuHook.cpp
    jni/org_iceterm_cehook_ConEmuHook.h
    jni/cehook/jni_Globals.cpp
    jni/cehook/jni_Globals.h
    jni/cehook/jni_Converter.cpp
    jni/cehook/jni_Converter.h
    jni/cehook/jni_Logger.cpp
    jni/cehook/jni_Logger.h
    jni/cehook/jni_Errors.cpp
    jni/cehook/jni_Errors.h
    jni/cehook/jni_EventDispatcher.cpp
    jni/cehook/jni_EventDispatcher.h
    jni/cehook/jni_Load.cpp
)

set(CEHOOK_SOURCES
    jni/cehook/cepipe/pipe_client.cpp
    jni/cehook/cepipe/pipe_client.h
    jni/cehook/logger.h
    jni/cehook/logger.cpp
    jni/cehook/input_helper.h
    jni/cehook/input_helper.cpp
    jni/cehook/input_hook.cpp
    jni/cehook/system_properties.cpp
)

if (${TARGET} MATCHES 64)
    add_library(iceterm64 SHARED ${ICETERM_SOURCES})
    add_library(cehook64 SHARED ${CEHOOK_SOURCES})
else ()
    add_library(iceterm SHARED ${ICETERM_SOURCES})
    add_library(cehook SHARED ${CEHOOK_SOURCES})
endif ()

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif ()
