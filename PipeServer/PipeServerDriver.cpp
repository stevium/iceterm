// PipeServer.cpp : Defines the entry point for the console application.
//

#include <io.h>
#include <fcntl.h>
#include <thread>
#include "CPipeServer.h"

int main(int argc, CHAR* argv[])
{
    //_setmode(_fileno(stdout), _O_U16TEXT);
    std::cout << "---------------------Pipe Server--------------------" << std::endl;
    std::string sPipeName(PIPENAME);
    CPipeServer* pServer = new CPipeServer(sPipeName);
    std::thread::id this_id = std::this_thread::get_id();
    std::cout << "thread " << this_id << " sleeping...\n";
	std::this_thread::sleep_for(std::chrono::milliseconds(4000));
    ::WaitForSingleObject(pServer->GetThreadHandle(), INFINITE);
	//std::thread serverThread(pServer->GetThreadHandle());
    delete pServer;
    pServer = NULL;

    return 0;
}
