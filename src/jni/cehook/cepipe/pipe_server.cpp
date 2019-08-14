#include <uiohook.h>
#include <cehook/include/uiohook.h>
#include "process.h"
#include "windows.h"
#include "tchar.h"
#include "pipe_server.h"
#include "org_iceterm_cehook_keyboard_NativeKeyEvent.h"
#include "org_iceterm_cehook_mouse_NativeMouseEvent.h"
#include "../jni_Globals.h"

pipe_server::pipe_server(void)
{
}

pipe_server::pipe_server(std::string& sName, JNIEnv * env, jclass cls)
	: m_sPipeName(sName), 
	m_hThread(NULL),
	m_nEvent(AU_INIT)
{
	jniEnv = env;
	jClass = cls;
    m_buffer = (char*)calloc(sizeof(uiohook_event), sizeof(char));
    Init();
}

pipe_server::~pipe_server(void)
{
    delete m_buffer;
    m_buffer = NULL;
}

int pipe_server::GetEvent() const
{
    return m_nEvent;
}

void pipe_server::SetEvent(int nEventID)
{
    m_nEvent = nEventID;
//    MessageBox(0, "Setting Event", "pipe_server", MB_OK);
}

HANDLE pipe_server::GetThreadHandle()
{
    return m_hThread;
}

HANDLE pipe_server::GetPipeHandle()
{
    return m_hPipe;
}

void pipe_server::SetData(uiohook_event sData)
{
    memset(&m_buffer[0], 0, sizeof(uiohook_event));
    memcpy(&m_buffer[0], reinterpret_cast<char*>(&sData), sizeof(uiohook_event));
}

// Get data from buffer
void pipe_server::GetData(uiohook_event &sData)
{
    sData.data = (reinterpret_cast<uiohook_event*>(m_buffer))->data;
    sData.time = (reinterpret_cast<uiohook_event*>(m_buffer))->time;
    sData.type = (reinterpret_cast<uiohook_event*>(m_buffer))->type;
    sData.mask = (reinterpret_cast<uiohook_event*>(m_buffer))->mask;
    sData.reserved = (reinterpret_cast<uiohook_event*>(m_buffer))->reserved;
}

void pipe_server::Init()
{
//    MessageBox(0, m_sPipeName.c_str(), "pipe_server", MB_OK);
    if(m_sPipeName.empty())
    {
        LOG << "Error: Invalid pipe name" << std::endl;
        return;
    }

    m_hPipe = ::CreateNamedPipe(
            m_sPipeName.c_str(),                // pipe name 
            PIPE_ACCESS_DUPLEX,       // read/write access 
            PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT,  // message-type pipe/message-read mode/blocking mode
            PIPE_UNLIMITED_INSTANCES, // max. instances  
            1024,              // output buffer size 
            1024,              // input buffer size 
            NMPWAIT_USE_DEFAULT_WAIT, // client time-out 
            NULL);                    // default security attribute 

    if(INVALID_HANDLE_VALUE == m_hPipe)
    {
        LOG << "Error: Could not create named pipe" << std::endl;
        OnEvent(AU_ERROR);
    }
    else
    {
        OnEvent(AU_SERV_RUN);
    }
}

void pipe_server::Run()
{
//    UINT uiThreadId = 0;
//    m_hThread = (HANDLE)_beginthreadex(NULL,
//        NULL,
//        PipeThreadProc,
//        this,
//        CREATE_SUSPENDED,
//        &uiThreadId);
	PipeThreadProc(this);

//    if(NULL == m_hThread)
//    {
//        OnEvent(AU_ERROR);
//    }
//    else
//    {
//        SetEvent(AU_INIT);
//        ::ResumeThread(m_hThread);
//    }
}

UINT32 __stdcall pipe_server::PipeThreadProc(void* pParam)
{
    pipe_server* pPipe = reinterpret_cast<pipe_server*>(pParam);
    if(pPipe == NULL)
        return 1L;

    pPipe->OnEvent(AU_THRD_RUN);
    while(true)
    {
        int nEventID = pPipe->GetEvent();
        if(nEventID == AU_ERROR || nEventID == AU_TERMINATE)
        {
            // Close pipe comm
            MessageBox(0, "Closing server", "pipe_server", MB_OK);
//            pPipe->Close();
            break;
        }

        switch(nEventID)
        {
        case AU_INIT:
            {
                pPipe->WaitForClient();
                break;
            }

        case AU_IOREAD:
            {
                if(pPipe->Read())
                    pPipe->OnEvent(AU_READ);
                else
                    pPipe->OnEvent(AU_ERROR);

                break;
            }

        case AU_IOWRITE:
            {
//                MessageBox(0, "IOWRITE", "pipe_server", MB_OK);
                if(pPipe->Write())
                    pPipe->OnEvent(AU_WRITE);
                else
                    pPipe->OnEvent(AU_ERROR);
            }
            break;

        case AU_CLOSE:
            {
                pPipe->OnEvent(AU_CLOSE);
                break;
            }

        case AU_IOWRITECLOSE:
            {
                if(pPipe->Write())
                    pPipe->OnEvent(AU_CLOSE);
                else
                    pPipe->OnEvent(AU_ERROR);

                break;
            }

        case AU_IOPENDING:
        default:
            Sleep(10);
            continue;
        };

        Sleep(10);
    };

    return 0;
}

void pipe_server::OnEvent(int nEventID)
{
    switch(nEventID)
    {
    case AU_THRD_RUN:
        LOG << "Thread running" << std::endl;
        break;

    case AU_INIT:
        LOG << "Initializing pipe comm" << std::endl;
        break;

    case AU_SERV_RUN:
        LOG << "Pipe server running" << std::endl;
        break;

    case AU_CLNT_WAIT:
        LOG << "Waiting for client" << std::endl;
        break;

    case AU_CLNT_CONN:
        {
//        std::string sData("Welcome pipe client!");
//        uiohook_event event;
//        event.data.keyboard.keychar = 109;

        LOG << "Client connected. Sending reserved: " << prefix_event.reserved << std::endl;
        LOG << "Client connected. Sending mask: " << prefix_event.mask << std::endl;
        SetData(prefix_event);
        SetEvent(AU_IOWRITE);
        break;
        }

    case AU_READ:
	{
		uiohook_event event;
		GetData(event);

        JNIEnv *env;
        if ((*jvm).GetEnv((void **)(&env), jvm_attach_args.version) == JNI_OK) {
            jobject NativeInputEvent_obj = NULL;
            jint location = org_iceterm_cehook_keyboard_NativeKeyEvent_LOCATION_UNKNOWN;

            switch (event.type) {
                case EVENT_KEY_PRESSED:
                case EVENT_KEY_RELEASED:
                case EVENT_KEY_TYPED:
                {
                    NativeInputEvent_obj = (*env).NewObject(
                            org_iceterm_cehook_keyboard_NativeKeyEvent->cls,
                            org_iceterm_cehook_keyboard_NativeKeyEvent->init,
                            org_iceterm_cehook_keyboard_NativeKeyEvent_NATIVE_KEY_PRESSED,
                            (jint) event.mask,
                            (jint) event.data.keyboard.rawcode,
                            (jint) event.data.keyboard.keycode,
                            (jchar) event.data.keyboard.keychar,
                            location);

                    break;
                }
                case EVENT_MOUSE_PRESSED: {
                    NativeInputEvent_obj = (*env).NewObject(
                            org_iceterm_cehook_mouse_NativeMouseEvent->cls,
                            org_iceterm_cehook_mouse_NativeMouseEvent->init,
                            org_iceterm_cehook_mouse_NativeMouseEvent_NATIVE_MOUSE_PRESSED,
                            (jint) event.mask,
                            (jint) event.data.mouse.x,
                            (jint) event.data.mouse.y,
                            (jint) event.data.mouse.clicks,
                            (jint) event.data.mouse.button);
                    break;
                }
            }

            if (NativeInputEvent_obj != NULL) {
                LOG << "Message from client: "  << std::endl;
                LOG << "client data: type " << event.type << std::endl;

                // Set the private when field to the native event time.
                (*env).SetShortField(
                        NativeInputEvent_obj,
                        org_iceterm_cehook_NativeInputEvent->when,
                        (jlong)	event.time);

                // Dispatch the event.
                (*env).CallStaticVoidMethod(
                        org_iceterm_cehook_GlobalScreen$NativeHookThread->cls,
                        org_iceterm_cehook_GlobalScreen$NativeHookThread->dispatchEvent,
                        NativeInputEvent_obj);

                SetEvent(AU_IOREAD);
            }

        }

//		if (strcmp(sData.c_str(), "close") == 0)
//			SetEvent(AU_CLOSE);
//		else {
//            SetEvent(AU_IOREAD);
    //		try {
    //			//LOG << "Getting static method ID" << " for class " << jClass << " env " << jniEnv << std::endl;
    //			//auto method = jniEnv->GetMethodID(jClass, "keyEventReceived", "(I)I");
    //			auto method = jniEnv->GetStaticMethodID(jClass, "dataReceived", "(Ljava/lang/String;)V");
    //			//LOG << "Calling meghod with id " << method << " for class " << jClass << " env " << jniEnv << std::endl;
    //			const char * charData = sData.c_str();
    //			jstring data = jniEnv->NewStringUTF(charData);
    //			jniEnv->CallStaticVoidMethod(jClass, method, data);
    //			//LOG << "static int method called from .cpp " << std::endl;
    //			SetEvent(AU_IOREAD);
    //		}
    //		catch (const std::exception& e) { // reference to the base of a polymorphic object
    //            SetEvent(AU_IOREAD);
    //			std::cout << e.what(); // information from length_error printed
    //
    //		}
//		}
		break;
	}
    case AU_WRITE:
        LOG << "Wrote data to pipe" << std::endl;
        SetEvent(AU_IOREAD);
        break;

    case AU_ERROR:
        LOG << "ERROR: Pipe error" << std::endl;
        SetEvent(AU_ERROR);
        break;

    case AU_CLOSE:
        LOG << "Closing pipe" << std::endl;
        SetEvent(AU_TERMINATE);
        break;
    };
}

void pipe_server::WaitForClient()
{
    OnEvent(AU_CLNT_WAIT);
    if(FALSE == ConnectNamedPipe(m_hPipe, NULL)) // Wait for client to get connected
    {
        //SetEventData("Error connecting to pipe client");
        MessageBox(0, "Error connecting to pipe client", "pipe_server", MB_OK);
        OnEvent(AU_ERROR);
    }
    else
    {
        OnEvent(AU_CLNT_CONN);
    }
}

void pipe_server::Close()
{
    ::CloseHandle(m_hPipe);
    m_hPipe = NULL;
}

bool pipe_server::Read()
{
    DWORD drBytes = 0;
    BOOL bFinishedRead = FALSE;
    int read = 0;
    do
    {
        bFinishedRead = ::ReadFile( 
            m_hPipe,            // handle to pipe 
            &m_buffer[read],    // buffer to receive data 
            sizeof(uiohook_event),        // size of buffer
            &drBytes,           // number of bytes read 
            NULL);              // not overlapped I/O 

        if(!bFinishedRead && ERROR_MORE_DATA != GetLastError())
        {
            bFinishedRead = FALSE;
            break;
        }
        read += drBytes;

    }while(!bFinishedRead);

    if(FALSE == bFinishedRead || 0 == drBytes)
    {
        //SetEventData("ReadFile failed");
//        MessageBox(0, "ReadFile failed", "pipe_server", MB_OK);
        return false;
    }

    return true;
}

bool pipe_server::Write()
{
    DWORD dwBytes;
    BOOL bResult = ::WriteFile(
        m_hPipe,                  // handle to pipe
        m_buffer,                 // buffer to write from
        sizeof(uiohook_event),     // number of bytes to write, include the NULL
        &dwBytes,                 // number of bytes written
        NULL);                  // not overlapped I/O

//    MessageBox(0, "Writing to Client", "pipe_server", MB_OK);

    if(FALSE == bResult || sizeof(uiohook_event) != dwBytes)
    {
        //SetEventData("WriteFile failed");
        MessageBox(0, "WriteFile failed", "pipe_server", MB_OK);
        return false;
    }

    return true;
}

void pipe_server::SetPrefixData(uiohook_event prefix) {
    LOG << "Setting Prefix data. Reserved " << prefix.reserved << std::endl;
    LOG << "Setting Prefix data. Keycode " << prefix.data.keyboard.keycode << std::endl;
    LOG << "Setting Prefix data. Rawcode " << prefix.data.keyboard.rawcode << std::endl;
    LOG << "Setting Prefix data. KeyChar " << prefix.data.keyboard.keychar  << std::endl;
    LOG << "Setting Prefix data. Modifiers " << prefix.mask << std::endl;
    prefix_event = prefix;
}
