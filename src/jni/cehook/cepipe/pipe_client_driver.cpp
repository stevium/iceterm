//#include <Memory.h>
//#include <string>
//#include <io.h>
//#include <fcntl.h>
//#include <thread>
//#include <Windows.h>
//#include <sstream>
//#include <uiohook.h>
#include "pipe_client.h"
//#include "windows.h"

//#define DLLEXPORT extern "C" __declspec(dllexport)

//DLLEXPORT BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
//    switch (ul_reason_for_call) {
//        case DLL_PROCESS_ATTACH:
//        case DLL_THREAD_ATTACH:
//            MessageBox(NULL, (LPCSTR) TEXT("Hello I'm injected DLL lib"), TEXT("DLL Injection"), NULL);
//        case DLL_THREAD_DETACH:
//        case DLL_PROCESS_DETACH:
//            break;
//    }
//    return TRUE;
//}
//pipe_client* pClient;

/*void jni_EventDispatcher(uiohook_event * const event) {
    std::stringstream ss;
    const char * mask = "mask " + event->mask;
    const char * time = " time " + event->time;
    const char * keychar = " data->keychar " + event->data.keyboard.keychar;
    const char * keycode = " data->keycode " + event->data.keyboard.keycode;
    const char * rawcode = " data->rawcode " + event->data.keyboard.rawcode;
    switch (event->type) {
        case EVENT_HOOK_DISABLED:
        case EVENT_HOOK_ENABLED:
            break;
        case EVENT_KEY_PRESSED:
            MessageBox(0, keychar, "MessageBox caption", MB_OK);
            break;
        case EVENT_KEY_RELEASED:
            break;
        case EVENT_KEY_TYPED:
            break;
        default:
            break;
    }
}*/

/*extern "C" __declspec(dllexport) int keyHandler(int nCode, WPARAM wParam, LPARAM lParam) {
	if (!pClient) {
		std::string sPipeName(PIPENAME);
		pClient = new pipe_client(sPipeName);
		Sleep(100);
	}
//    hook_set_dispatch_proc(&jni_EventDispatcher);
    if ((lParam & (1 << 30)) == 0) {
        pClient->SetData("keyboard_hook_event_proc");
        pClient->SetEvent(AU_IOWRITE);
    }
	return 1;
}*/

/*
int main(int argc, CHAR* argv[])
{
    std::cout << "---------------------Pipe Client--------------------" << std::endl;
    std::string sPipeName(PIPENAME);
    pipe_client* pClient = new pipe_client(sPipeName);
    Sleep(1000);
    pClient->SetData("Test 1");
    pClient->SetEvent(AU_IOWRITE);
    Sleep(1000);
    pClient->SetData("Test 2");
    pClient->SetEvent(AU_IOWRITE);
    ::WaitForSingleObject(pClient->GetThreadHandle(), INFINITE);
    delete pClient;
    pClient = NULL;
    return 0;
}
*/

