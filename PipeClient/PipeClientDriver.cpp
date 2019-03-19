#include "Memory.h"
#include <string>
#include <io.h>
#include <fcntl.h>
#include <thread>
#include "Windows.h"
#include "CPipeClient.h"

CPipeClient* pClient;

extern "C" __declspec(dllexport) int keyHandler(int nCode, WPARAM wParam, LPARAM lParam) {
	if (!pClient) {
		std::string sPipeName(PIPENAME);
		pClient = new CPipeClient(sPipeName);
		Sleep(100);
	}
    char* strValue = new char[2];
	strValue[0] = (char) wParam;
	strValue[1] = '\0';

	//DWORD keyCode = (((KBDLLHOOKSTRUCT *)Param)->vkCode);
	//std::string stringKeyCode = std::to_string(keyCode);
	pClient->SetData(strValue);
	pClient->SetEvent(AU_IOWRITE);
	//switch (wParam) {
	//	case WM_KEYDOWN:
	//	{
	//		//if ((lParam & (1 << 30)) == 0) {
	//			DWORD keyCode = (((KBDLLHOOKSTRUCT *)lParam)->vkCode);
	//			std::string stringKeyCode = std::to_string(keyCode);
	//			pClient->SetData(stringKeyCode);
	//			pClient->SetEvent(AU_IOWRITE);
	//		//}
	//		break;
	//	}
	//	case WM_SYSKEYDOWN:
	//		break;
	//	case WM_KEYUP:
	//		break;
	//	case WM_SYSKEYUP:
	//		break;
	//}

	return CallNextHookEx(NULL, nCode, wParam, lParam);
}
