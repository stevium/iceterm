cmake_minimum_required(VERSION 3.14)
project(iceterm)

set(CMAKE_CXX_STANDARD 14)

include_directories(jni)
include_directories(jni/include)
include_directories(conemuhook/include)

add_library(iceterm SHARED
        jni/org_iceterm_ceintegration_GuiMacroExecutor.cpp
        jni/org_iceterm_ceintegration_GuiMacroExecutor.h
        conemuhook/conemupipe/pipe_server.cpp
        conemuhook/conemupipe/pipe_server.h
        conemuhook/conemupipe/pipe_client.cpp
        conemuhook/conemupipe/pipe_client.h
        conemuhook/logger.h
        conemuhook/logger.cpp
        conemuhook/input_helper.h
        conemuhook/input_helper.cpp
        conemuhook/input_hook.cpp
        conemuhook/system_properties.cpp
        conemuhook/post_event.cpp
        jni/org_iceterm_cehook_ConEmuHook.cpp
        jni/org_iceterm_cehook_ConEmuHook.h
        jni/jni_Globals.cpp
        jni/jni_Globals.h
        jni/jni_Converter.cpp
        jni/jni_Converter.h
        jni/jni_Logger.cpp
        jni/jni_Logger.h
        jni/jni_Errors.cpp
        jni/jni_Errors.h
        jni/jni_EventDispatcher.cpp
        jni/jni_EventDispatcher.h
        jni/jni_Load.cpp
        jni/org_jnativehook_GlobalScreen.cpp
        )

add_library(ConEmuHook SHARED
        conemuhook/conemupipe/pipe_client.cpp
        conemuhook/conemupipe/pipe_client.h
#        conemuhook/conemupipe/pipe_client_driver.cpp
        conemuhook/logger.h
        conemuhook/logger.cpp
        conemuhook/input_helper.h
        conemuhook/input_helper.cpp
        conemuhook/input_hook.cpp
        conemuhook/system_properties.cpp
        conemuhook/post_event.cpp
#        conemuhook/conemupipe/pipe_server_driver.cpp
        )

#[[add_executable(client
        conemuhook/conemupipe/pipe_client.cpp
        conemuhook/conemupipe/pipe_client.h
        conemuhook/conemupipe/pipe_client_driver.cpp)

add_executable(server
        conemuhook/conemupipe/pipe_server.cpp
        conemuhook/conemupipe/pipe_server_driver.cpp
        conemuhook/conemupipe/pipe_server.h)]]

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

#target_link_libraries(iceterm ${JNI_LIBRARIES})
