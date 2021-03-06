/**
 * Description: CPipeClient class implements pipe client related functionality.
 *              Wraps the underlying ReadFile/WriteFile functions to read/write
 *              data to the pipe. Provides an event-based mechanism to handle
 *              pipe communication. An independent thread processes all the pipe
 *              related events. This implemenation is Windows specific.
 */

#pragma once

#include "windows.h"
#include <string>
#include <uiohook.h>
#include "pipe_const.h"

extern uiohook_event *escape_key;

class pipe_client
{
public:

    /**
     * Constructor
     * @paramIn sName: Pipe name
     */
    pipe_client(std::string& sName);

    /**
     * Destructor
     */
    virtual ~pipe_client(void);

    /**
     * Get event ID
     * @paramOut: Event ID
     */
    int GetEvent() const;

    /**
     * Set event ID
     * @paramIn: Event ID
     */
    void SetEvent(int nEventID);

    /**
     * Get handle of the thread that processes pipe I/O events
     * @paramOut: Thread HANDLE
     */
    HANDLE GetThreadHandle();

    /**
     * Get pipe handle
     * @paramOut: Pipe HANDLE
     */
    HANDLE GetPipeHandle();

    /**
     * Write data to buffer
     * @paramIn sData: string data to be copied to buffer
     */
    void SetData(uiohook_event sData);

    /**
     * Read data from buffer
     * @paramOut: Data will be copied from buffer
     */
    void GetData(uiohook_event * sData);

    /**
     * Connect pipe client to pipe server
     */
    void ConnectToServer();

    /**
     * Invoked whenever there is a pipe event
     * @paramIn nEvent: Event type received
     */
    void OnEvent(int nEvent);

    /**
     * Thread callback function which processes the pipe I/O events
     * @paramIn param: CPipeClient object
     */
    static UINT32 __stdcall PipeThreadProc(void* param); 

    /**
     * Close the pipe client
     */
    void Close();

    /**
     * Read data from pipe. This is a blocking call.
     * @return: true if success else false
     */
    bool Read();

    /**
     * Writes data from buffer into the pipe
     * @return: true if success else false
     */
    bool Write();

private:

    /**
     * Default constructor
     */
    pipe_client(void);

    /**
     * Initializes pipe client. A thread is created and starts running
     */
    void Init();

    /**
     * Starts the pipe thread
     */
    void Run();

    const std::string m_sPipeName; // Pipe name
    HANDLE m_hPipe;                 // Pipe handle
    HANDLE m_hThread;               // Pipe thread
    int    m_nEvent;                // Pipe event
    char* m_buffer;              // Buffer to hold data

};
