#include "org_iceterm_jni_GuiMacroExecutor_N.h"
#include "windows.h"
#include <iostream>
#include <comutil.h>
#include <locale>
#include <codecvt>
#include <CPipeServer.h>
#include <CPipeClient.h>
#include <stdio.h>
#include <tlhelp32.h>

typedef int (__stdcall *_GuiMacro)(LPCWSTR asInstance, LPCWSTR asMacro, BSTR* bsResult);

typedef void(__cdecl * _FuncInitPipeClient)();
_FuncInitPipeClient funcInitPipeClient;

DWORD demoSetWindowsHookEx(LPCSTR pszLibFile, DWORD dwProcessId, const char *strProcName);

HMODULE hConEmuCD;
_GuiMacro fnGuiMacro;

JNIEXPORT jlong JNICALL Java_org_iceterm_jni_GuiMacroExecutor_1N_N_1LoadConEmuDll(JNIEnv *env, jobject, jstring jsPath) {

	const char* sPath = env->GetStringUTFChars(jsPath, nullptr);
	char *charArr = new char[env->GetStringUTFLength(jsPath)];
	strcpy(charArr, sPath);
	env->ReleaseStringUTFChars(jsPath, sPath);


	hConEmuCD = LoadLibrary(charArr);
	if (!hConEmuCD) {
		std::cout << "could not load the dynamic library" << std::endl;
		return EXIT_FAILURE;
	}

	return (jlong) hConEmuCD;
}

JNIEXPORT jlong JNICALL Java_org_iceterm_jni_GuiMacroExecutor_1N_N_1InitGuiMacroFn (JNIEnv *, jobject) {

	if (!hConEmuCD) {
		std::cout << "could not load the dynamic library" << std::endl;
		return EXIT_FAILURE;
	}

	fnGuiMacro = (_GuiMacro) GetProcAddress(hConEmuCD, "GuiMacro");

	if (!fnGuiMacro) {
		std::cout << "could not locate the function" << std::endl;
		return EXIT_FAILURE;
	}

	return (jlong)fnGuiMacro;

}

std::wstring Java_To_WStr(JNIEnv *env, jstring javaString)
{
	const char *nativeString = env->GetStringUTFChars(javaString, 0);
	const size_t cSize = strlen(nativeString)+1;
    wchar_t* wc = new wchar_t[cSize];
    mbstowcs (wc, nativeString, cSize);
    env->ReleaseStringUTFChars(javaString, nativeString);
    return wc;
}

std::string utf8_encode(const std::wstring &wstr)
{
    if( wstr.empty() ) return std::string();
    int size_needed = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
    std::string strTo( size_needed, 0 );
    WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], size_needed, NULL, NULL);
    return strTo;
}

std::wstring s2ws(const std::string& str)
{
    using convert_typeX = std::codecvt_utf8<wchar_t>;
    std::wstring_convert<convert_typeX, wchar_t> converterX;

    return converterX.from_bytes(str);
}

std::string ws2s(const std::wstring& wstr)
{
    using convert_typeX = std::codecvt_utf8<wchar_t>;
    std::wstring_convert<convert_typeX, wchar_t> converterX;

    return converterX.to_bytes(wstr);
}

JNIEXPORT jint JNICALL Java_org_iceterm_jni_GuiMacroExecutor_1N_N_1ExecuteInProcess (JNIEnv *env, jobject, jstring nConEmuPid, jstring asMacro, jobject joResult)
{
	int iRc;
	const char *csResult;
	BSTR bsResult;

    const std::wstring wsInstance = Java_To_WStr(env, nConEmuPid);
    const std::wstring wsMacro = Java_To_WStr(env, asMacro);
    iRc = fnGuiMacro(wsInstance.c_str(), wsMacro.c_str(), &bsResult);

    jclass clazz = env->GetObjectClass(joResult);
    jmethodID appendId = env->GetMethodID(clazz, "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
	std::string sResult = ws2s(bsResult);
	csResult = sResult.c_str();
    jstring jsResult = env->NewStringUTF(csResult);

    env->CallObjectMethod(joResult, appendId, jsResult);
	return iRc;
}

JNIEXPORT void JNICALL Java_org_iceterm_jni_GuiMacroExecutor_1N_N_1RunPipeServer (JNIEnv *env, jclass cls)
{
    std::string sPipeName(PIPENAME);
    CPipeServer* pServer = new CPipeServer(sPipeName, env, cls);
	pServer->Run();
}

JNIEXPORT void JNICALL Java_org_iceterm_jni_GuiMacroExecutor_1N_N_1RunPipeClient(JNIEnv *, jclass, jint nConEmuPid) {
	try {
		demoSetWindowsHookEx("C:\\Users\\Milos\\IdeaProjects\\intellij-sdk-docs\\code_samples\\IdeaConEmu\\x64\\Debug\\PipeClient.dll", DWORD((int)nConEmuPid), "ConEmu64.exe");
	}
	catch (const std::exception& e) { // reference to the base of a polymorphic object
		 std::cout << e.what(); // information from length_error printed
	}
	std::cout << "LOADED SUCCESSFULLYY " << (int)nConEmuPid << std::endl;
}

DWORD getThreadID(DWORD pid)
{
	HANDLE h = CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0);
	if (h != INVALID_HANDLE_VALUE)
	{
		THREADENTRY32 te;
		te.dwSize = sizeof(te);
		if (Thread32First(h, &te))
		{
			do
			{
				if (te.dwSize >= FIELD_OFFSET(THREADENTRY32, th32OwnerProcessID) + sizeof(te.th32OwnerProcessID))
				{
					if (te.th32OwnerProcessID == pid)
					{
						HANDLE hThread = OpenThread(READ_CONTROL, FALSE, te.th32ThreadID);
						if (!hThread)
							printf(TEXT("[-] Error: Couldn't get thread handle\n"));
						else
							return te.th32ThreadID;
					}
				}
			} while (Thread32Next(h, &te));
		}
	}

	CloseHandle(h);
	return (DWORD)0;
}

DWORD demoSetWindowsHookEx(LPCSTR pszLibFile, DWORD dwProcessId, const char *strProcName)
{
	DWORD dwThreadId = getThreadID(dwProcessId);
	if (dwThreadId == (DWORD)0)
	{
		printf(TEXT("[-] Error: Cannot find thread"));
		return(1);
	}

	printf(TEXT("[+] Using Thread ID %u\n"), dwThreadId);

	HMODULE dll = LoadLibraryEx(pszLibFile, NULL, DONT_RESOLVE_DLL_REFERENCES);
	if (dll == NULL) 
	{
		printf(TEXT("[-] Error: The DLL could not be found.\n"));
		return(1);
	}

	HOOKPROC addr = (HOOKPROC)GetProcAddress(dll, "keyHandler");
	if (addr == NULL) 
	{
		printf(TEXT("[-] Error: The DLL exported function was not found.\n"));
		return(1);
	}

	HWND targetWnd = FindWindow(NULL, strProcName);
	GetWindowThreadProcessId(targetWnd, &dwProcessId);

	HHOOK handle = SetWindowsHookEx(WH_KEYBOARD, addr, dll, dwThreadId);
	if (handle == NULL)
	{
		printf(TEXT("[-] Error: The KEYBOARD could not be hooked.\n"));
		return(1);
	}
	else
	{
		printf(TEXT("[+] Program successfully hooked."));
	}

	return(0);
}
