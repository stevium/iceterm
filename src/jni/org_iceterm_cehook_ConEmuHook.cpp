//
// Created by Milos on 4/26/19.
//

#include "org_iceterm_cehook_ConEmuHook.h"
#include "../cehook/cepipe/pipe_server.h"
#include "../cehook/cepipe/pipe_client.h"
#include <tlhelp32.h>
#include <Windows.h>

HMODULE _ceHookDll;
HHOOK keyboard_event_hhook;

DWORD runConEmuHook(DWORD ceProcessId, HWND hwnd);
void loadHookDll(LPCSTR libFileName, int i);
DWORD getThreadID(DWORD pid);
pipe_server* pServer;

/*
 * Class:     org_iceterm_cehook_ConEmuHook
 * Method:    runPipeServer
 * Signature: ()V
 */
void Java_org_iceterm_cehook_ConEmuHook_createPipeServer (JNIEnv *env, jclass cls) {
    std::string sPipeName(PIPENAME);
    pServer = new pipe_server(sPipeName, env, cls);
}

/*
 * Class:     org_iceterm_cehook_ConEmuHook
 * Method:    runPipeServer
 * Signature: ()V
 */
void Java_org_iceterm_cehook_ConEmuHook_runPipeServer (JNIEnv *env, jclass cls) {
    pServer->Run();
//    ::WaitForSingleObject(pServer->GetThreadHandle(), INFINITE);
}

/*
 * Class:     org_iceterm_cehook_ConEmuHook
 * Method:    inject
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_iceterm_cehook_ConEmuHook_inject (JNIEnv *, jclass, jint nConEmuPid, jlong hwnd) {
    try {
        loadHookDll(R"(D:\stevium\iceterm\src\cmake-build-debug\cehook.dll)", (int) nConEmuPid);
        runConEmuHook(DWORD((int) nConEmuPid), (HWND)(LONG_PTR)hwnd);
    }
    catch (const std::exception& e) {
        std::cout << e.what();
    }
    std::cout << "LOADED SUCCESSFULLY " << (int)nConEmuPid << std::endl;
}

void loadHookDll(LPCSTR libPath, int processId) {
    _ceHookDll = LoadLibraryEx(libPath, NULL, DONT_RESOLVE_DLL_REFERENCES);
    if (_ceHookDll == NULL) {
        printf(TEXT("[-] Error: The DLL could not be found.\n"));
    }
}

DWORD runConEmuHook(DWORD ceProcessId, HWND hwnd) {
    DWORD ceThreadId = getThreadID(ceProcessId);
    HOOKPROC addr = (HOOKPROC)GetProcAddress(_ceHookDll, "keyboard_hook_event_proc");
    keyboard_event_hhook = SetWindowsHookEx(WH_KEYBOARD, addr, _ceHookDll, ceThreadId);
    if (keyboard_event_hhook == NULL) {
        printf(TEXT("[-] Error: The KEYBOARD could not be hooked.\n"));
        return(1);
    } else {
        printf(TEXT("[+] Program successfully hooked."));
    }
    return(0);
}

DWORD getThreadID(DWORD pid)
{
    HANDLE h = CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0);
    if (h != INVALID_HANDLE_VALUE)
    {
        THREADENTRY32 te;
        te.dwSize = sizeof(te);
        if (Thread32First(h, &te))
        {
            do
            {
                if (te.dwSize >= FIELD_OFFSET(THREADENTRY32, th32OwnerProcessID) + sizeof(te.th32OwnerProcessID))
                {
                    if (te.th32OwnerProcessID == pid)
                    {
                        HANDLE hThread = OpenThread(READ_CONTROL, FALSE, te.th32ThreadID);
                        if (!hThread)
                            printf(TEXT("[-] Error: Couldn't get thread handle\n"));
                        else
                            return te.th32ThreadID;
                    }
                }
            } while (Thread32Next(h, &te));
        }
    }

    CloseHandle(h);
    return (DWORD)0;
}

