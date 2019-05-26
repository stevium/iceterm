//
// Created by Milos on 4/26/19.
//

#include "org_iceterm_cehook_ConEmuHook.h"
#include "../conemuhook/conemupipe/pipe_server.h"
#include "../conemuhook/conemupipe/pipe_client.h"
#include <tlhelp32.h>
#include <Windows.h>

HINSTANCE hinst;
#pragma data_seg(".shared")
HHOOK hhk;
#pragma data_seg()

HMODULE _ceHookDll;
HHOOK keyboard_event_hhook;

DWORD runConEmuHook(DWORD ceProcessId, HWND hwnd);
void loadHookDll(LPCSTR libFileName, int i);
DWORD getThreadID(DWORD pid);

// Global Hook handle
HHOOK hKeyHook;

unsigned long GetTargetProcessIdFromWindow(char *className, char *windowName);

typedef void(__cdecl *PASSHOOKHANDLE)(HHOOK);

//static const int PATH_BUFFER_SIZE = 256;
/*
 * Class:     org_iceterm_cehook_ConEmuHook
 * Method:    runPipeServer
 * Signature: ()V
 */
void Java_org_iceterm_cehook_ConEmuHook_runPipeServer (JNIEnv *env, jclass cls) {
    std::string sPipeName(PIPENAME);
    pipe_server* pServer = new pipe_server(sPipeName, env, cls);
//    pServer->Run();
}

/*
 * Class:     org_iceterm_cehook_ConEmuHook
 * Method:    inject
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_iceterm_cehook_ConEmuHook_inject (JNIEnv *, jclass, jint nConEmuPid, jlong hwnd) {
    try {
        loadHookDll("D:\\stevium\\iceterm\\src\\cmake-build-debug\\ConEmuHook.dll", (int) nConEmuPid);
        runConEmuHook(DWORD((int) nConEmuPid), (HWND)(LONG_PTR)hwnd);
    }
    catch (const std::exception& e) {
        std::cout << e.what();
    }
    std::cout << "LOADED SUCCESSFULLY " << (int)nConEmuPid << std::endl;
}

void loadHookDll(LPCSTR libPath, int processId) {
/*    HANDLE hProcess = OpenProcess(PROCESS_ALL_ACCESS, false, processId);
    if (hProcess == NULL) {
        printf("[x] Cannot open process with id %d\n", processId);
        exit(1);
    }

    LPVOID dllAllocatedMemory = VirtualAllocEx(hProcess, NULL, strlen(libPath), MEM_RESERVE | MEM_COMMIT, PAGE_EXECUTE_READWRITE);

    WriteProcessMemory(hProcess, dllAllocatedMemory, libPath, strlen(libPath) + 1, NULL);

    LPVOID loadLibrary = (LPVOID) GetProcAddress(GetModuleHandle("kernel32.dll"), "LoadLibraryA");*/

//    Sleep(1000);
//    MessageBox(NULL, (LPCSTR) TEXT("Loading message box"), TEXT("DLL Injection"), NULL);
//    CreateRemoteThread(hProcess, NULL, 0, (LPTHREAD_START_ROUTINE) loadLibrary, dllAllocatedMemory, 0, NULL);

    _ceHookDll = LoadLibraryEx(libPath, NULL, DONT_RESOLVE_DLL_REFERENCES);
    if (_ceHookDll == NULL) {
        printf(TEXT("[-] Error: The DLL could not be found.\n"));
    }
}
/*__declspec(dllexport) LRESULT CALLBACK keyboard_hook_event_proc2(int nCode, WPARAM wParam, LPARAM lParam) {
    if  ((nCode == HC_ACTION) &&       // HC_ACTION means we may process this event
         ((wParam == WM_SYSKEYDOWN) ||  // Only react if either a system key ...
          (wParam == WM_KEYDOWN)))       // ... or a normal key have been pressed.
    {
        MessageBox(0, "Entered keyboard_hook_event_proc", "MessageBox caption", MB_OK);
    }

    return CallNextHookEx(hhk, nCode,wParam,lParam);
}*/

/*unsigned long GetTargetProcessIdFromWindow(char *className, char *windowName)
{
    HWND targetWnd;
    HANDLE hProcess;
    unsigned long processID = 0;

    targetWnd = FindWindow(className, windowName);
    return GetWindowThreadProcessId(targetWnd, &processID);
}*/

DWORD runConEmuHook(DWORD ceProcessId, HWND hwnd) {
    DWORD ceThreadId = getThreadID(ceProcessId);
//    DWORD ceThreadId = GetWindowThreadProcessId(hwnd, NULL);
//    long ceThreadId = GetTargetProcessIdFromWindow("Notepad", "Untitled - Notepad");
//    if (ceThreadId == (DWORD) 0) {
//    printf("TID: %i", ceThreadId);
//        return NULL;
//    }

    HOOKPROC addr = (HOOKPROC)GetProcAddress(_ceHookDll, "keyboard_hook_event_proc");
//    MessageBox(0, "HOOKING", "MessageBox caption", MB_OK);
//    HMODULE hinst = GetModuleHandle(NULL);
//    printf("Hinst: %i", hinst);
    keyboard_event_hhook = SetWindowsHookEx(WH_KEYBOARD, addr, _ceHookDll, ceThreadId);
    if (keyboard_event_hhook == NULL) {
        printf(TEXT("[-] Error: The KEYBOARD could not be hooked.\n"));
        return(1);
    } else {
        printf(TEXT("[+] Program successfully hooked."));
    }

//    PASSHOOKHANDLE pass_hook_handle = (PASSHOOKHANDLE)GetProcAddress(_ceHookDll, "pass_hook_handle");
//    pass_hook_handle(keyboard_event_hhook);

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

