// PipeClient.cpp : Defines the entry point for the console application.
//

#include <string>
#include <io.h>
#include <fcntl.h>
#include <thread>
#include "CPipeClient.h"

CPipeClient* pClient;
std::thread serverThread;

int main(int argc, CHAR* argv[])
{
    std::cout << "---------------------Pipe Client--------------------" << std::endl;
    std::string sPipeName(PIPENAME);
    pClient = new CPipeClient(sPipeName);

	while (true) {
		std::string str;
		std::getline(std::cin, str);
		pClient->SetData(str);
		pClient->SetEvent(AU_IOWRITE);
	}
    return 0;
}
