cmake_minimum_required(VERSION 3.14)
project(iceterm)

set(CMAKE_CXX_STANDARD 14)

include_directories(jni)
include_directories(pipeclient)
include_directories(pipeserver)

add_library(iceterm SHARED
        jni/ConEmuPipeServer_N.cpp
        jni/GuiMacroExecutor_N.cpp
        jni/org_iceterm_jni_ConEmuPipeServer_N.h
        jni/org_iceterm_jni_GuiMacroExecutor_N.h
        pipeclient/CPipeClient.cpp
        pipeclient/CPipeClient.h
        pipeclient/PipeClientDriver.cpp
        pipeclient/PipeClientDriver.h
        pipeserver/CPipeServer.cpp
        pipeserver/CPipeServer.h
        pipeserver/PipeConst.h
        pipeserver/PipeServerDriver.cpp)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()


target_link_libraries(iceterm ${JNI_LIBRARIES})
