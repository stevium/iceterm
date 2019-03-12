#include "conemu_jni_GuiMacroExecutor_N.h"
#include "windows.h"
#include <iostream>
#include <comutil.h>
#include <locale>
#include <codecvt>

typedef int (__stdcall *_GuiMacro)(LPCWSTR asInstance, LPCWSTR asMacro, BSTR* bsResult);

HMODULE hConEmuCD;
_GuiMacro fnGuiMacro;

JNIEXPORT jlong JNICALL Java_conemu_jni_GuiMacroExecutor_1N_N_1LoadConEmuDll(JNIEnv *env, jobject, jstring jsPath) {

	const char* sPath = env->GetStringUTFChars(jsPath, nullptr);
	char *charArr = new char[env->GetStringUTFLength(jsPath)];
	strcpy(charArr, sPath);
	env->ReleaseStringUTFChars(jsPath, sPath);


	hConEmuCD = LoadLibrary(charArr);
	if (!hConEmuCD) {
		std::cout << "could not load the dynamic library" << std::endl;
		return EXIT_FAILURE;
	}

	//delete[] charArr;

	return (jlong) hConEmuCD;
}

JNIEXPORT jlong JNICALL Java_conemu_jni_GuiMacroExecutor_1N_N_1InitGuiMacroFn (JNIEnv *, jobject) {

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
    //std::wstring value;
    //const jchar *raw = env->GetStringChars(javaString, 0);
    //jsize len = env->GetStringLength(javaString);
    //value.assign(raw, raw + len);

	const char *nativeString = env->GetStringUTFChars(javaString, 0);
	const size_t cSize = strlen(nativeString)+1;
    wchar_t* wc = new wchar_t[cSize];
    mbstowcs (wc, nativeString, cSize);

	std::cout << "char string: " << nativeString << std::endl;

	std::wstring ws(wc);
	std::string str(ws.begin(), ws.end());
	std::cout << "wchar string: " << ws.c_str() << std::endl;

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

JNIEXPORT jint JNICALL Java_conemu_jni_GuiMacroExecutor_1N_N_1ExecuteInProcess (JNIEnv *env, jobject, jstring nConEmuPid, jstring asMacro, jobject joResult)
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

