#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#include <uiohook.h>
#include <windows.h>

#include "input_helper.h"
#include "logger.h"
#include "cepipe/pipe_server.h"

// Some buggy versions of MinGW and MSys do not include these constants in winuser.h.
#ifndef MAPVK_VK_TO_VSC
#define MAPVK_VK_TO_VSC			0
#define MAPVK_VSC_TO_VK			1
#define MAPVK_VK_TO_CHAR		2
#define MAPVK_VSC_TO_VK_EX		3
#endif
// Some buggy versions of MinGW and MSys only define this value for Windows
// versions >= 0x0600 (Windows Vista) when it should be 0x0500 (Windows 2000).
#ifndef MAPVK_VK_TO_VSC_EX
#define MAPVK_VK_TO_VSC_EX		4
#endif

#ifndef KEYEVENTF_SCANCODE
#define KEYEVENTF_EXTENDEDKEY	0x0001
#define KEYEVENTF_KEYUP			0x0002
#define	KEYEVENTF_UNICODE		0x0004
#define KEYEVENTF_SCANCODE		0x0008
#endif

#ifndef KEYEVENTF_KEYDOWN
#define KEYEVENTF_KEYDOWN		0x0000
#endif

#define MAX_WINDOWS_COORD_VALUE 65535

static UINT keymask_lookup[8] = {
	VK_LSHIFT,
	VK_LCONTROL,
	VK_LWIN,
	VK_LMENU,

	VK_RSHIFT,
	VK_RCONTROL,
	VK_RWIN,
	VK_RMENU
};

UIOHOOK_API void hook_post_event(uiohook_event * const postedEvent) {
//	//FIXME implement multiple monitor support
	unsigned char events_size = 0, events_max = 28;
	INPUT *events = static_cast<INPUT *>(malloc(sizeof(INPUT) * events_max));

	if (postedEvent->mask & (MASK_SHIFT | MASK_CTRL | MASK_META | MASK_ALT)) {
		for (unsigned int i = 0; i < sizeof(keymask_lookup) / sizeof(UINT); i++) {
			if (postedEvent->mask & 1 << i) {
				events[events_size].type = INPUT_KEYBOARD;
				events[events_size].ki.wVk = keymask_lookup[i];
				events[events_size].ki.dwFlags = KEYEVENTF_KEYDOWN;
				events[events_size].ki.time = 0; // Use current system time.
				events_size++;
			}
		}
	}

	switch (postedEvent->type) {
		case EVENT_KEY_PRESSED:
			events[events_size].ki.wVk = scancode_to_keycode(postedEvent->data.keyboard.keycode);
			if (events[events_size].ki.wVk != 0x0000) {
				events[events_size].type = INPUT_KEYBOARD;
				events[events_size].ki.dwFlags = KEYEVENTF_KEYDOWN; // |= KEYEVENTF_SCANCODE;
				events[events_size].ki.wScan = 0; // postedEvent->data.keyboard.keycode;
				events[events_size].ki.time = 0; // GetSystemTime()
				events_size++;
			}
			else {
				logger(LOG_LEVEL_INFO, "%s [%u]: Unable to lookup scancode: %li\n",
						__FUNCTION__, __LINE__,
						postedEvent->data.keyboard.keycode);
			}
			break;

		case EVENT_KEY_RELEASED:
			events[events_size].ki.wVk = scancode_to_keycode(postedEvent->data.keyboard.keycode);
			if (events[events_size].ki.wVk != 0x0000) {
				events[events_size].type = INPUT_KEYBOARD;
				events[events_size].ki.dwFlags = KEYEVENTF_KEYUP; // |= KEYEVENTF_SCANCODE;
				events[events_size].ki.wVk = scancode_to_keycode(postedEvent->data.keyboard.keycode);
				events[events_size].ki.wScan = 0; // postedEvent->data.keyboard.keycode;
				events[events_size].ki.time = 0; // GetSystemTime()
				events_size++;
			}
			else {
				logger(LOG_LEVEL_INFO, "%s [%u]: Unable to lookup scancode: %li\n",
						__FUNCTION__, __LINE__,
						postedEvent->data.keyboard.keycode);
			}
			break;


		case EVENT_KEY_TYPED:
			// Ignore clicked and typed events.

		case EVENT_HOOK_ENABLED:
		case EVENT_HOOK_DISABLED:
			// Ignore hook enabled / disabled events.

		default:
			// Ignore any other garbage.
			logger(LOG_LEVEL_WARN, "%s [%u]: Ignoring post postedEvent type %#X\n",
					__FUNCTION__, __LINE__, postedEvent->type);
			break;
	}

	// Release the previously held modifier keys used to fake the postedEvent mask.
	if (postedEvent->mask & (MASK_SHIFT | MASK_CTRL | MASK_META | MASK_ALT)) {
		for (unsigned int i = 0; i < sizeof(keymask_lookup) / sizeof(UINT); i++) {
			if (postedEvent->mask & 1 << i) {
				events[events_size].type = INPUT_KEYBOARD;
				events[events_size].ki.wVk = keymask_lookup[i];
				events[events_size].ki.dwFlags = KEYEVENTF_KEYUP;
				events[events_size].ki.time = 0; // Use current system time.
				events_size++;
			}
		}
	}

    if(postedEvent->reserved == 0x01) {
//        MessageBox(0, "Setting escape data", "post_event", MB_OK);
        pServer->SetEscapeData(*postedEvent);
    } else if (! SendInput(events_size, events, sizeof(INPUT)) ) {
//        MessageBox(0, "Event Posted", "post_event", MB_OK);
		logger(LOG_LEVEL_ERROR, "%s [%u]: SendInput() failed! (%#lX)\n",
				__FUNCTION__, __LINE__, (unsigned long) GetLastError());
	}

	free(events);
}
