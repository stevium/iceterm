cmake_minimum_required(VERSION 3.14)
project(iceterm)

set(CMAKE_CXX_STANDARD 14)

include_directories(./)
include_directories(./include)
include_directories(./cehook/include)

set(ICETERM_SOURCES
    ./cehook/cepipe/pipe_server.cpp
    ./cehook/cepipe/pipe_server.h
    ./cehook/post_event.cpp
    ./cehook/input_helper.h
    ./cehook/input_helper.cpp
    ./cehook/logger.h
    ./cehook/logger.cpp
    ./org_iceterm_ceintegration_GuiMacroExecutor.cpp
    ./org_iceterm_ceintegration_GuiMacroExecutor.h
    ./org_iceterm_cehook_GlobalScreen.cpp
    ./org_iceterm_cehook_ConEmuHook.cpp
    ./org_iceterm_cehook_ConEmuHook.h
    ./cehook/jni_Globals.cpp
    ./cehook/jni_Globals.h
    ./cehook/jni_Converter.cpp
    ./cehook/jni_Converter.h
    ./cehook/jni_Logger.cpp
    ./cehook/jni_Logger.h
    ./cehook/jni_Errors.cpp
    ./cehook/jni_Errors.h
    ./cehook/jni_EventDispatcher.cpp
    ./cehook/jni_EventDispatcher.h
    ./cehook/jni_Load.cpp
)

set(CEHOOK_SOURCES
    ./cehook/cepipe/pipe_client.cpp
    ./cehook/cepipe/pipe_client.h
    ./cehook/logger.h
    ./cehook/logger.cpp
    ./cehook/input_helper.h
    ./cehook/input_helper.cpp
    ./cehook/input_hook.cpp
    ./cehook/system_properties.cpp
)

include(./detect_cpu_achitectures.cmake)

if("amd64" IN_LIST CMAKE_CPU_ARCHITECTURES)
    add_library(iceterm64 SHARED ${ICETERM_SOURCES})
    add_library(cehook64 SHARED ${CEHOOK_SOURCES})
elseif("x86" IN_LIST CMAKE_CPU_ARCHITECTURES)
    add_library(iceterm SHARED ${ICETERM_SOURCES})
    add_library(cehook SHARED ${CEHOOK_SOURCES})
endif()

set(JAVA_JVM_LIBRARY NotNeeded)
find_package(Java REQUIRED)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif ()
