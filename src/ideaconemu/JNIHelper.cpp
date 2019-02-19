//#include <jni.h>
//#include <jawt.h>
//#include <jawt_md.h>
//#include <assert.h>

#include <iostream>
#include "ideaconemu_JNIHelper.h"

using namespace std;

// Implementation of the native method sayHello()
//JNIEXPORT void JNICALL Java_ideaconemu_JNIHelper_sayHello(JNIEnv *env, jclass thisClass) {
//	cout << "Hello World from C++!" << endl;
//   return;
//}

JNIEXPORT void JNICALL Java_ideaconemu_JNIHelper_N_1SetParent
  (JNIEnv *env, jobject thisObject, jlong hwndChild, jlong hwndParent) {
    HWND hChild = (HWND)(LONG_PTR)hwndChild;
    HWND hParent = (HWND)(LONG_PTR)hwndParent;
    HWND resultHwnd = SetParent(hChild, hParent);
	cout << "hChild: " << (long) hChild << endl;
	cout << "hParent: " << (long) hParent << endl;
	cout << "Return Hwnd: " << (long) resultHwnd << endl;
 }

//JNIEXPORT jint JNICALL Java_ideaconemu_JNIHelper_GetHwndOfCanvas(JNIEnv *env, jclass cls, jobject canvas) {
//  JAWT awt;
//  JAWT_DrawingSurface* ds;
//  JAWT_DrawingSurfaceInfo* dsi;
//  JAWT_Win32DrawingSurfaceInfo* dsi_win;
//  jboolean bGetAwt;
//  jint lock;
//
//  // Get the AWT.
//  awt.version = JAWT_VERSION_1_4;
//  bGetAwt = JAWT_GetAWT(env, &awt);
//  assert(bGetAwt != JNI_FALSE);
//
//  // Get the drawing surface.
//  ds = awt.GetDrawingSurface(env, canvas);
//  assert(ds != NULL);
//
//  // Lock the drawing surface.
//  lock = ds->Lock(ds);
//  assert((lock & JAWT_LOCK_ERROR) == 0);
//
//  // Get the drawing surface info.
//  dsi = ds->GetDrawingSurfaceInfo(ds);
//  if (dsi == NULL) {
//    return 0;
//  }
//
//  // Get the platform-specific drawing info.
//  dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
//  HWND result = dsi_win->hwnd;
//
//  // Free the drawing surface info
//  ds->FreeDrawingSurfaceInfo(dsi);
//  // Unlock the drawing surface
//  ds->Unlock(ds);
//  // Free the drawing surface
//  awt.FreeDrawingSurface(ds);
//
//  return result;
//  }
//

