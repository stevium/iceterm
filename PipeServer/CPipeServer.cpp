#include "process.h"
#include "windows.h"
#include "tchar.h"
#include "CPipeServer.h"


CPipeServer::CPipeServer(void)
{
}

CPipeServer::CPipeServer(std::string& sName, JNIEnv * env, jclass cls) 
	: m_sPipeName(sName), 
	m_hThread(NULL), 
	m_nEvent(AU_INIT)
{
	jniEnv = env;
	jClass = cls;
    m_buffer = (char*)calloc(AU_DATA_BUF, sizeof(char));
    Init();
}

CPipeServer::~CPipeServer(void)
{
    delete m_buffer;
    m_buffer = NULL;
}

int CPipeServer::GetEvent() const
{
    return m_nEvent;
}

void CPipeServer::SetEvent(int nEventID)
{
    m_nEvent = nEventID;
}

HANDLE CPipeServer::GetThreadHandle()
{
    return m_hThread;
}

HANDLE CPipeServer::GetPipeHandle()
{
    return m_hPipe;
}

void CPipeServer::SetData(std::string& sData)
{
    memset(&m_buffer[0], 0, AU_DATA_BUF);
    strncpy(&m_buffer[0], sData.c_str(), __min(AU_DATA_BUF, sData.size()));
}

// Get data from buffer
void CPipeServer::GetData(std::string& sData)
{
    sData.clear(); // Clear old data, if any
    sData.append(m_buffer);
}

void CPipeServer::Init()
{
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

    //Run();
}

void CPipeServer::Run()
{
    //UINT uiThreadId = 0;
    //m_hThread = (HANDLE)_beginthreadex(NULL,
    //    NULL,
    //    PipeThreadProc,
    //    this,
    //    CREATE_SUSPENDED,
    //    &uiThreadId);
	PipeThreadProc(this);

    //if(NULL == m_hThread)
    //{
    //    OnEvent(AU_ERROR);
    //}
    //else
    //{
    //    SetEvent(AU_INIT);
    //    ::ResumeThread(m_hThread);
    //}
}

UINT32 __stdcall CPipeServer::PipeThreadProc(void* pParam)
{
    CPipeServer* pPipe = reinterpret_cast<CPipeServer*>(pParam);
    if(pPipe == NULL)
        return 1L;

    pPipe->OnEvent(AU_THRD_RUN);
    while(true)
    {
        int nEventID = pPipe->GetEvent();
        if(nEventID == AU_ERROR || nEventID == AU_TERMINATE)
        {
            // Close pipe comm
            pPipe->Close();
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

void CPipeServer::OnEvent(int nEventID)
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
        std::string sData("Welcome pipe client!");
        SetData(sData);
        SetEvent(AU_IOWRITE);
        break;
        }

    case AU_READ:
	{
		std::string sData;
		GetData(sData);
		//LOG << "Message from client: " << sData << std::endl;

		if (strcmp(sData.c_str(), "close") == 0)
			SetEvent(AU_CLOSE);
		else {

		}
		try {
			//LOG << "Getting static method ID" << " for class " << jClass << " env " << jniEnv << std::endl;
			//auto method = jniEnv->GetMethodID(jClass, "keyEventReceived", "(I)I");
			auto method = jniEnv->GetStaticMethodID(jClass, "keyEventReceived", "(Ljava/lang/String;)V");
			//LOG << "Calling meghod with id " << method << " for class " << jClass << " env " << jniEnv << std::endl;
			const char * charData = sData.c_str();
			jstring data = jniEnv->NewStringUTF(charData);
			jniEnv->CallStaticVoidMethod(jClass, method, data);
			//LOG << "static int method called from .cpp " << std::endl;
			SetEvent(AU_IOREAD);
		}
		catch (const std::exception& e) { // reference to the base of a polymorphic object
			std::cout << e.what(); // information from length_error printed

		}
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

void CPipeServer::WaitForClient()
{
    OnEvent(AU_CLNT_WAIT);
    if(FALSE == ConnectNamedPipe(m_hPipe, NULL)) // Wait for client to get connected
    {
        //SetEventData("Error connecting to pipe client");
        OnEvent(AU_ERROR);
    }
    else
    {
        OnEvent(AU_CLNT_CONN);
    }
}

void CPipeServer::Close()
{
    ::CloseHandle(m_hPipe);
    m_hPipe = NULL;
}

bool CPipeServer::Read()
{
    DWORD drBytes = 0;
    BOOL bFinishedRead = FALSE;
    int read = 0;
    do
    {
        bFinishedRead = ::ReadFile( 
            m_hPipe,            // handle to pipe 
            &m_buffer[read],    // buffer to receive data 
            AU_DATA_BUF,        // size of buffer 
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
        return false;
    }

    return true;
}

bool CPipeServer::Write()
{
    DWORD dwBytes;
    BOOL bResult = ::WriteFile(
        m_hPipe,                  // handle to pipe
        m_buffer,                 // buffer to write from
        ::strlen(m_buffer)*sizeof(char) + 1,     // number of bytes to write, include the NULL
        &dwBytes,                 // number of bytes written
        NULL);                  // not overlapped I/O

    if(FALSE == bResult || strlen(m_buffer)*sizeof(char) + 1 != dwBytes)
    {
        //SetEventData("WriteFile failed");
        return false;
    }

    return true;
}
