//
// Created by Milos on 4/30/19.
//
#include "pipe_server.h"

int main(int argc, CHAR* argv[])
{
    std::wcout << "---------------------Pipe Server--------------------" << std::endl;
    std::string sPipeName(PIPENAME);
    pipe_server* pServer = new pipe_server(sPipeName, nullptr, nullptr);
    ::WaitForSingleObject(pServer->GetThreadHandle(), INFINITE);
    delete pServer;
    pServer = NULL;

    return 0;
}
