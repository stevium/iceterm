#include "process.h"
#include "pipe_client.h"


pipe_client::pipe_client(void)
{
}

pipe_client::pipe_client(std::string& sName) : m_sPipeName(sName), 
                                                m_hThread(NULL), 
                                                m_nEvent(AU_INIT)
{
    m_buffer = (char*)calloc(AU_DATA_BUF, sizeof(char));
    Init();
}

pipe_client::~pipe_client(void)
{
    delete m_buffer;
    m_buffer = NULL;
}

int pipe_client::GetEvent() const
{
    return m_nEvent;
}

void pipe_client::SetEvent(int nEventID)
{
    m_nEvent = nEventID;
}

HANDLE pipe_client::GetThreadHandle()
{
    return m_hThread;
}

HANDLE pipe_client::GetPipeHandle()
{
    return m_hPipe;
}

void pipe_client::SetData(std::string sData)
{
    memset(&m_buffer[0], 0, AU_DATA_BUF);
//    memcpy(&m_buffer[0], sData.c_str(), __min(AU_DATA_BUF, sData.size()));
    strncpy(&m_buffer[0], sData.c_str(), __min(AU_DATA_BUF, sData.size()));
}

// Get data from buffer
void pipe_client::GetData(std::string& sData)
{
    sData.clear(); // Clear old data, if any
    sData.append(m_buffer);
}

void pipe_client::Init()
{
//    MessageBox(0, m_sPipeName.c_str(), "pipe_client", MB_OK);
    if(m_sPipeName.empty())
    {
        // Invalid pipe name
        return;
    }

    Run();
}

void pipe_client::Run()
{
    UINT uiThreadId = 0;
    m_hThread = (HANDLE)::_beginthreadex(NULL,
        NULL,
        PipeThreadProc,
        this,
        CREATE_SUSPENDED,
        &uiThreadId);

    if(NULL == m_hThread)
    {
//        MessageBox(0, "m_hThread is null", "pipe_client", MB_OK);
        OnEvent(AU_ERROR);
    }
    else
    {
        SetEvent(AU_INIT);
        ::ResumeThread(m_hThread);
    }
}

UINT32 __stdcall pipe_client::PipeThreadProc(void* pParam)
{
    pipe_client* pPipe = reinterpret_cast<pipe_client*>(pParam);
    if(pPipe == NULL)
        return 1L;

    pPipe->OnEvent(AU_THRD_RUN);
    while(true)
    {
        int nEventID = pPipe->GetEvent();
        if(nEventID == AU_ERROR || nEventID == AU_TERMINATE)
        {
            // Close pipe comm
//            MessageBox(0, "CLosing pipe", "pipe_client", MB_OK);
            pPipe->Close();
            break;
        }

        switch(nEventID)
        {
            case AU_INIT:
            {
                pPipe->ConnectToServer();
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
                if(pPipe->Write())
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

void pipe_client::ConnectToServer()
{
    OnEvent(AU_CLNT_TRY);
    m_hPipe = ::CreateFile(
        m_sPipeName.c_str(),      // pipe name
        GENERIC_READ | GENERIC_WRITE, // read and write access
        0,              // no sharing
        NULL,           // default security attributes
        OPEN_EXISTING,  // opens existing pipe
        0,              // default attributes
        NULL);          // no template file

    if(INVALID_HANDLE_VALUE == m_hPipe)
    {
        //SetEventData("Could not connect to pipe server");
//        MessageBox(0, "Could not connect to pipe server", "pipe_client", MB_OK);
//        OnEvent(AU_ERROR);
    }
    else
    {
        OnEvent(AU_CLNT_CONN);
    }
}

void pipe_client::OnEvent(int nEventID)
{
    switch(nEventID)
    {
    case AU_THRD_RUN:
        LOG << "Thread running" << std::endl;
        break;

    case AU_INIT:
        LOG << "Initializing pipe comm" << std::endl;
        break;

    case AU_CLNT_TRY:
        LOG << "Trying to connect to pipe server" << std::endl;
        break;

    case AU_CLNT_CONN:
        {
        LOG << "Connected to server" << std::endl;
        SetEvent(AU_IOREAD);
        break;
        }

    case AU_READ:
        {
        std::string sData;
        GetData(sData);
        LOG << "Message from server: " << sData << std::endl;
        sData.clear();
        //sData.append("close");
        //SetData(sData);
        SetEvent(AU_IOPENDING);
        break;
        }

    case AU_WRITE:
        LOG << "Wrote data to pipe" << std::endl;
        SetEvent(AU_IOPENDING);
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

void pipe_client::Close()
{
    ::CloseHandle(m_hPipe);
    m_hPipe = NULL;
}

bool pipe_client::Read()
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
//        MessageBox(0, "ReadFile failed", "pipe_client", MB_OK);
        return false;
    }

    return true;
}

bool pipe_client::Write()
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
//        SetEventData("WriteFile failed");
//        MessageBox(0, "WriteFile failed", "pipe_client", MB_OK);
        std::string result = std::to_string(bResult);
//        MessageBox(0, result.c_str(), "pipe_client", MB_OK);
        return false;
    }

    return true;
}
