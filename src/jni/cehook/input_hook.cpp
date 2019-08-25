#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <inttypes.h>
#include <uiohook.h>
#include <windows.h>
#include <TlHelp32.h>

#include "input_helper.h"
#include "logger.h"
#include <string>
#include <cehook/include/uiohook.h>
#include "cehook/cepipe/pipe_client.h"

// Thread and hook handles.
static DWORD hook_thread_id = 0;
static HHOOK keyboard_event_hhook = NULL;
static HWINEVENTHOOK win_event_hhook = NULL;

// The handle to the DLL module pulled in DllMain on DLL_PROCESS_ATTACH.
extern HINSTANCE hInst;
pipe_client *pClient;

// Modifiers for tracking key masks.
static unsigned short int current_modifiers = 0x0000;

// Static event memory.
static uiohook_event event;

// Event dispatch callback.
static dispatcher_t dispatcher = NULL;

int hook_result = 0;
int escape_mode = 0;
bool is_escape;

static bool is_escape_key(uiohook_event event);

UIOHOOK_API void hook_set_dispatch_proc(dispatcher_t dispatch_proc) {
    logger(LOG_LEVEL_DEBUG, "%s [%u]: Setting new dispatch callback to %#p.\n",
           __FUNCTION__, __LINE__, dispatch_proc);

    dispatcher = dispatch_proc;
}

// Send out an event if a dispatcher was set.
static inline void dispatch_event(uiohook_event *const event) {
    switch (event->type) {
        case EVENT_HOOK_DISABLED:
//            MessageBox(0, "EVENT_HOOK_DISABLED", "MessageBox caption", MB_OK);
            break;
        case EVENT_HOOK_ENABLED:
//            MessageBox(0, "EVENT_HOOK_ENABLED", "MessageBox caption", MB_OK);
            break;
        case EVENT_KEY_PRESSED:
            pClient->SetData(*event);
            pClient->SetEvent(AU_IOWRITE);
            break;
        case EVENT_KEY_RELEASED:
//           MessageBox(0, "EVENT_KEY_RELEASED", "MessageBox caption", MB_OK);
            break;
        case EVENT_KEY_TYPED:
            pClient->SetData(*event);
            pClient->SetEvent(AU_IOWRITE);
        case EVENT_MOUSE_PRESSED:
            pClient->SetData(*event);
            pClient->SetEvent(AU_IOWRITE);
            break;
//            MessageBox(0, "EVENT_KEY_TYPED", "MessageBox caption", MB_OK);
            break;
        default:
            break;
    }
}


// Set the native modifier mask for future events.
static inline void set_modifier_mask(unsigned short int mask) {
    current_modifiers |= mask;
}

// Unset the native modifier mask for future events.
static inline void unset_modifier_mask(unsigned short int mask) {
    current_modifiers ^= mask;
}

// Get the current native modifier mask state.
static inline unsigned short int get_modifiers() {
    return current_modifiers;
}

// Initialize the modifier mask to the current modifiers.
static void initialize_modifiers() {
    current_modifiers = 0x0000;

    // NOTE We are checking the high order bit, so it will be < 0 for a singed short.
    if (GetKeyState(VK_LSHIFT) < 0) { set_modifier_mask(MASK_SHIFT); }
    if (GetKeyState(VK_RSHIFT) < 0) { set_modifier_mask(MASK_SHIFT); }
    if (GetKeyState(VK_LCONTROL) < 0) { set_modifier_mask(MASK_CTRL); }
    if (GetKeyState(VK_RCONTROL) < 0) { set_modifier_mask(MASK_CTRL); }
    if (GetKeyState(VK_LMENU) < 0) { set_modifier_mask(MASK_ALT); }
    if (GetKeyState(VK_RMENU) < 0) { set_modifier_mask(MASK_ALT); }
    if (GetKeyState(VK_LWIN) < 0) { set_modifier_mask(MASK_META); }
    if (GetKeyState(VK_RWIN) < 0) { set_modifier_mask(MASK_META); }

//    if (GetKeyState(VK_LBUTTON) < 0) { set_modifier_mask(MASK_BUTTON1); }
//    if (GetKeyState(VK_RBUTTON) < 0) { set_modifier_mask(MASK_BUTTON2); }
//    if (GetKeyState(VK_MBUTTON) < 0) { set_modifier_mask(MASK_BUTTON3); }
//    if (GetKeyState(VK_XBUTTON1) < 0) { set_modifier_mask(MASK_BUTTON4); }
//    if (GetKeyState(VK_XBUTTON2) < 0) { set_modifier_mask(MASK_BUTTON5); }

    if (GetKeyState(VK_NUMLOCK) < 0) { set_modifier_mask(MASK_NUM_LOCK); }
    if (GetKeyState(VK_CAPITAL) < 0) { set_modifier_mask(MASK_CAPS_LOCK); }
    if (GetKeyState(VK_SCROLL) < 0) { set_modifier_mask(MASK_SCROLL_LOCK); }
}

void unregister_running_hooks() {
    // Stop the event hook and any timer still running.
    if (win_event_hhook != NULL) {
        UnhookWinEvent(win_event_hhook);
        win_event_hhook = NULL;
    }

    // Destroy the native hooks.
    if (keyboard_event_hhook != NULL) {
        UnhookWindowsHookEx(keyboard_event_hhook);
        keyboard_event_hhook = NULL;
    }
}

void hook_start_proc() {
    // Get the local system time in UNIX epoch form.
    uint64_t timestamp = GetMessageTime();

    // Populate the hook start event.
    event.time = timestamp;
    event.reserved = 0x00;

    event.type = EVENT_HOOK_ENABLED;
    event.mask = 0x00;

    // Fire the hook start event.
    dispatch_event(&event);
}

void hook_stop_proc() {
    // Get the local system time in UNIX epoch form.
    uint64_t timestamp = GetMessageTime();

    // Populate the hook stop event.
    event.time = timestamp;
    event.reserved = 0x00;

    event.type = EVENT_HOOK_DISABLED;
    event.mask = 0x00;

    // Fire the hook stop event.
    dispatch_event(&event);
}

static int process_key_pressed(int nCode, WPARAM wParam, LPARAM lParam) {
    current_modifiers = 0x0000;
    initialize_modifiers();
    //	event.time = kbhook->time;
    event.reserved = 0x00;
    event.type = EVENT_KEY_PRESSED;
    event.mask = get_modifiers();
    event.data.keyboard.keycode = keycode_to_scancode(wParam, HIWORD(lParam));
    event.data.keyboard.rawcode = wParam;
    event.data.keyboard.keychar = CHAR_UNDEFINED;

    logger(LOG_LEVEL_INFO, "%s [%u]: Key %#X pressed. (%#X)\n",
           __FUNCTION__, __LINE__, event.data.keyboard.keycode, event.data.keyboard.rawcode);

    // If the pressed event was not consumed...
    if (event.reserved ^ 0x01) {
        // Buffer for unicode typed chars. No more than 2 needed.
        WCHAR buffer[2]; // = { WCH_NONE };

        // If the pressed event was not consumed and a unicode char exists...
        SIZE_T count = keycode_to_unicode(wParam, buffer, sizeof(buffer));
        for (unsigned int i = 0; i < count; i++) {
            // Populate key typed event.
//			event.time = kbhook->time;
            event.reserved = 0x00;

            event.type = EVENT_KEY_TYPED;
//            event.mask = get_modifiers();

//            event.data.keyboard.keycode = VC_UNDEFINED;
//            event.data.keyboard.rawcode = wParam;
            event.data.keyboard.keychar = buffer[i];

//            logger(LOG_LEVEL_INFO, "%s [%u]: Key %#X typed. (%lc)\n",
//                   __FUNCTION__, __LINE__, event.data.keyboard.keycode, (wint_t) event.data.keyboard.keychar);
        }
    }

    if (is_escape_key(event)) {
        escape_mode = 0;
        dispatch_event(&event);
        return -1;
    }

    if(escape_mode) {
        // Fire key typed event.
        dispatch_event(&event);
        if(event.mask) {
            return -1;
        }
        escape_mode = 0;
        return -1;
    }

    return 0;
}

static bool is_escape_key(uiohook_event event) {
    is_escape = (escape_key != NULL)
        && (escape_key->mask == event.mask)
        && (escape_key->data.keyboard.keycode == event.data.keyboard.rawcode);
    return is_escape;
}

static void process_key_released(int nCode, WPARAM wParam, LPARAM lParam) {
    current_modifiers = 0x0000;
    initialize_modifiers();
    event.mask = get_modifiers();
    if(escape_mode && !event.mask && !is_escape) {
        escape_mode = 0;
    }
}

UIOHOOK_API LRESULT CALLBACK keyboard_hook_event_proc(int nCode, WPARAM wParam, LPARAM lParam) {
    if(!pClient) {
        std::string sPipeName(PIPENAME);
        pClient = new pipe_client(sPipeName);
        Sleep(100);
    }
    HANDLE ph = pClient->GetPipeHandle();
    if (ph == INVALID_HANDLE_VALUE)
        return 0;

    if ((lParam & (1 << 30)) == 0) {
         hook_result = process_key_pressed(nCode, wParam, lParam);
    } else {
        process_key_released(nCode, wParam, lParam);
    }

    return hook_result;
}

static int process_button_pressed(int nCode, WPARAM wParam, LPARAM lParam) {
    event.type = EVENT_MOUSE_PRESSED;
    dispatch_event(&event);
    return 0;
}

UIOHOOK_API LRESULT CALLBACK mouse_hook_event_proc(int nCode, WPARAM wParam, LPARAM lParam) {
    if(!pClient) {
        std::string sPipeName(PIPENAME);
        pClient = new pipe_client(sPipeName);
        Sleep(100);
    }
    HANDLE ph = pClient->GetPipeHandle();
    if (ph == INVALID_HANDLE_VALUE)
        return 0;

    MOUSEHOOKSTRUCT * pMouseStruct = (MOUSEHOOKSTRUCT *)lParam;
    if (pMouseStruct != NULL){
        if(wParam == WM_LBUTTONDOWN)
        {
            return process_button_pressed(nCode, wParam, lParam);
        }
    }
    return 0;
}

// Callback function that handles events.
void CALLBACK win_hook_event_proc(HWINEVENTHOOK hook, DWORD event, HWND hWnd, LONG idObject, LONG idChild,
                                  DWORD dwEventThread, DWORD dwmsEventTime) {
    switch (event) {
        case EVENT_OBJECT_NAMECHANGE:
            logger(LOG_LEVEL_INFO, "%s [%u]: Restarting Windows input hook on window event: %#X.\n",
                   __FUNCTION__, __LINE__, event);

            // Remove any keyboard or mouse hooks that are still running.
            if (keyboard_event_hhook != NULL) {
                UnhookWindowsHookEx(keyboard_event_hhook);
            }

            // Restart the event hooks.
            MessageBox(0, "Win hook event proc", "Thread Id", MB_OK);
//			keyboard_event_hhook = SetWindowsHookEx(WH_KEYBOARD, keyboard_hook_event_proc, hInst, 0);
//			mouse_event_hhook = SetWindowsHookEx(WH_MOUSE_LL, mouse_hook_event_proc, hInst, 0);

            // Re-initialize modifier masks.
            initialize_modifiers();

            // FIXME We should compare the modifier mask before and after the restart
            // to determine if we should synthesize missing events.

            // Check for event hook error.
            if (keyboard_event_hhook == NULL /*|| mouse_event_hhook == NULL*/) {
                logger(LOG_LEVEL_ERROR, "%s [%u]: SetWindowsHookEx() failed! (%#lX)\n",
                       __FUNCTION__, __LINE__, (unsigned long) GetLastError());
            }
            break;

        default:
            logger(LOG_LEVEL_INFO, "%s [%u]: Unhandled Windows window event: %#X.\n",
                   __FUNCTION__, __LINE__, event);
    }
}

UIOHOOK_API int hook_run() {
    int status = UIOHOOK_FAILURE;

    // Set the thread id we want to signal later.
    hook_thread_id = GetCurrentThreadId();

    // Spot check the hInst incase the library was statically linked and DllMain
    // did not receive a pointer on load.
    if (hInst == NULL) {
        logger(LOG_LEVEL_INFO, "%s [%u]: hInst was not set by DllMain().\n",
               __FUNCTION__, __LINE__);

        hInst = GetModuleHandle(NULL);
        if (hInst != NULL) {
            // Initialize native input helper functions.
            load_input_helper();
        } else {
            logger(LOG_LEVEL_ERROR, "%s [%u]: Could not determine hInst for SetWindowsHookEx()! (%#lX)\n",
                   __FUNCTION__, __LINE__, (unsigned long) GetLastError());

            status = UIOHOOK_ERROR_GET_MODULE_HANDLE;
        }
    }

    // Check and setup modifiers.
    initialize_modifiers();

    // Set the exit status.
    status = UIOHOOK_SUCCESS;

    // Windows does not have a hook start event or callback so we need to
    // manually fake it.
    hook_start_proc();

    // Block until the thread receives an WM_QUIT request.
    MSG message;
    while (GetMessage(&message, (HWND) NULL, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessage(&message);
    }

    // Unregister any hooks that may still be installed.
    unregister_running_hooks();

    // We must explicitly call the cleanup handler because Windows does not
    // provide a thread cleanup method like POSIX pthread_cleanup_push/pop.
    hook_stop_proc();

    return status;
}

UIOHOOK_API int hook_stop() {
    int status = UIOHOOK_FAILURE;

    // Try to exit the thread naturally.
    if (PostThreadMessage(hook_thread_id, WM_QUIT, (WPARAM) NULL, (LPARAM) NULL)) {
        status = UIOHOOK_SUCCESS;
    }

    logger(LOG_LEVEL_DEBUG, "%s [%u]: Status: %#X.\n",
           __FUNCTION__, __LINE__, status);

    return status;
}

UIOHOOK_API void pass_hook_handle(HHOOK keyboard_hhook) {
    keyboard_event_hhook = keyboard_hhook;
}
