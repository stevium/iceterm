/**
 * Description: CPipeServer class implements pipe server related functionality.
 *              Wraps the underlying ReadFile/WriteFile functions to read/write
 *              data to the pipe. Provides an event-based mechanism to handle
 *              pipe communication. An independent thread processes all the pipe
 *              related events. This implemenation is Windows specific.
 */

#pragma once

#include "windows.h"
#include <string>
#include "PipeConst.h"
#include <jni.h>

class CPipeServer
{
public:

    /**
     * Constructor
     * @paramIn sName: Pipe name
     */
    CPipeServer(std::string& sName, JNIEnv *env, jclass cls);

    /**
     * Destructor
     */
    virtual ~CPipeServer(void);

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
    void SetData(std::string& sData);

    /**
     * Read data from buffer
     * @paramOut: Data will be copied from buffer
     */
    void GetData(std::string& sData);

    /**
     * Invoked whenever there is a pipe event
     * @paramIn nEvent: Event type received
     */
    void OnEvent(int nEvent);

    /**
     * Thread callback function which processes the pipe I/O events
     * @paramIn param: CPipeClient object
     */
    static UINT32 __stdcall PipeThreadProc(void*); 

    /**
     * Wait for a pipe client to get connected
     */
    void WaitForClient();

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

    /**
     * Starts the pipe thread
     */
    void Run();

private:

    /**
     * Default constructor
     */
    CPipeServer(void);

    /**
     * Initializes pipe client. A thread is created and starts running
     */
    void Init();


    const std::string m_sPipeName; // Pipe name
    HANDLE m_hPipe;                 // Pipe handle
    HANDLE m_hThread;               // Pipe thread
    JNIEnv * jniEnv;
    jclass jClass;
    int    m_nEvent;                // Pipe event
    char* m_buffer;              // Buffer to hold data

};
