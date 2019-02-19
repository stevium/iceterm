#include "conemu_jni_GuiMacroExecutor_N.h"
#include "windows.h"
#include <iostream>

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

	delete[] charArr;

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

std::wstring Java_To_WStr(JNIEnv *env, jstring string)
{
    std::wstring value;

    const jchar *raw = env->GetStringChars(string, 0);
    jsize len = env->GetStringLength(string);

    value.assign(raw, raw + len);

    env->ReleaseStringChars(string, raw);

    return value;
}

JNIEXPORT jint JNICALL Java_conemu_jni_GuiMacroExecutor_1N_N_1ExecuteInProcess (JNIEnv *env, jobject, jstring nConEmuPid, jstring asMacro)
{
	int iRc;

    const std::wstring wsInstance = Java_To_WStr(env, nConEmuPid);
    const std::wstring wsMacro = Java_To_WStr(env, asMacro);

    BSTR bsResult;
    iRc = fnGuiMacro(wsInstance.c_str(), wsMacro.c_str(), &bsResult);

	return iRc;
}

