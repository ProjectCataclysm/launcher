#include <Windows.h>
#include <shellapi.h>
#include <Shobjidl.h>

#include "WinJFX.h"

#define NOTIFICATION_TRAY_ICON_MSG (WM_USER + 0x100)

void ThrowIllegalStateException(JNIEnv *env, char *message)
{
	jclass exClass = env->FindClass("java/lang/IllegalStateException");
	env->ThrowNew(exClass, message);
}

JNIEXPORT jlong JNICALL Java_ru_knoblul_winjfx_WinJFX_getWindowStyleFlags(JNIEnv *env, jclass callerClass, jlong windowHandle)
{
	// для того чтобы сделать прозрачное окно с нормальным
	// поведением в винде - убираем нужные флаги окна
	HWND hWnd = (HWND)windowHandle;
	return GetWindowLong(hWnd, GWL_STYLE);
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_setWindowStyleFlags(JNIEnv *env, jclass callerClass, jlong windowHandle, jlong windowStyleFlags)
{
	// для того чтобы сделать прозрачное окно с нормальным
	// поведением в винде - убираем нужные флаги окна
	HWND hWnd = (HWND)windowHandle;
	SetWindowLong(hWnd, GWL_STYLE, (LONG)windowStyleFlags);
}

// /*
//  * Class:     ru_knoblul_winjfx_WinJFX
//  * Method:    bringWindowToTop
//  * Signature: (J)V
//  */
// JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_bringWindowToTop(JNIEnv *env, jclass callerClass, jlong windowHandle)
// {
// 	BringWindowToTop((HWND) windowHandle);
// }

LRESULT CALLBACK WndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
	POINT curPoint;
	UINT clicked;
	TrayIconData *trayIconData = (TrayIconData *)GetWindowLongPtr(hWnd, GWLP_USERDATA);
	if (uMsg == NOTIFICATION_TRAY_ICON_MSG)
	{
		if ((lParam & WM_LBUTTONUP) == 0)
		{
			trayIconData->jni_env->CallStaticVoidMethod(trayIconData->winJFX_class, trayIconData->popupLmbCallback_methodId,
														trayIconData->trayIconInstance);
		}
		else if (lParam == WM_RBUTTONUP)
		{
			SetForegroundWindow(hWnd);
			GetCursorPos(&curPoint);
			clicked = TrackPopupMenu(
				trayIconData->hMenu,
				TPM_RETURNCMD,
				curPoint.x,
				curPoint.y,
				0,
				hWnd,
				NULL);

			trayIconData->jni_env->CallStaticVoidMethod(trayIconData->winJFX_class, trayIconData->popupRmbCallback_methodId,
														trayIconData->trayIconInstance, clicked);
		}
	}

	return DefWindowProc(hWnd, uMsg, wParam, lParam);
}

void FreeTrayIconData(TrayIconData *trayIconData)
{
	if (!trayIconData)
	{
		return;
	}

	HWND hWnd = trayIconData->hWnd;
	if (hWnd)
	{
		NOTIFYICONDATAW nid = {};
		nid.cbSize = sizeof(NOTIFYICONDATAW);
		nid.hWnd = hWnd;
		nid.uID = 0;
		Shell_NotifyIconW(NIM_DELETE, &nid);
		DestroyWindow(hWnd);
	}

	HMENU hMenu = trayIconData->hMenu;
	if (hMenu)
	{
		DestroyMenu(hMenu);
	}

	if (trayIconData->jni_env && trayIconData->trayIconInstance)
	{
		trayIconData->jni_env->DeleteGlobalRef(trayIconData->trayIconInstance);
	}

	free(trayIconData);
}

HICON ReadTrayIconBytes(JNIEnv *env, jbyteArray iconBytes)
{
	char message[256];
	jbyte *iconBytesPtr = env->GetByteArrayElements(iconBytes, NULL);
	if (!iconBytesPtr)
	{
		snprintf(message, sizeof(message), "Icon: GetByteArrayElements() failed: %d", GetLastError());
		ThrowIllegalStateException(env, message);
		return NULL;
	}

	jsize iconBytesCount = env->GetArrayLength(iconBytes);
	if (!iconBytesCount)
	{
		env->ReleaseByteArrayElements(iconBytes, iconBytesPtr, JNI_ABORT);
		snprintf(message, sizeof(message), "Icon: GetArrayLength() returns 0: %d", GetLastError());
		ThrowIllegalStateException(env, message);
		return NULL;
	}

	HBITMAP hBitmap = CreateBitmap(16, 16, 1, 32, iconBytesPtr);
	if (!hBitmap)
	{
		env->ReleaseByteArrayElements(iconBytes, iconBytesPtr, JNI_ABORT);
		snprintf(message, sizeof(message), "Icon: CreateBitmap() failed: %d", GetLastError());
		ThrowIllegalStateException(env, message);
		return NULL;
	}

	env->ReleaseByteArrayElements(iconBytes, iconBytesPtr, JNI_ABORT);

	HDC DC = GetDC(NULL);
	if (!DC)
	{
		DeleteObject(hBitmap);
		snprintf(message, sizeof(message), "Icon: GetDC() failed: %d", GetLastError());
		ThrowIllegalStateException(env, message);
		return NULL;
	}

	ICONINFO ii = {};
	ii.fIcon = TRUE;
	ii.xHotspot = 0;
	ii.yHotspot = 0;
	ii.hbmMask = CreateCompatibleBitmap(DC, 16, 16);
	ii.hbmColor = hBitmap;

	HICON hIcon = CreateIconIndirect(&ii);
	if (!hIcon)
	{
		DeleteObject(hBitmap);
		snprintf(message, sizeof(message), "Icon: CreateIconIndirect() failed: %d", GetLastError());
		ThrowIllegalStateException(env, message);
		return NULL;
	}

	DeleteObject(hBitmap);
	return hIcon;
}

JNIEXPORT jlong JNICALL Java_ru_knoblul_winjfx_WinJFX_createTrayIcon(JNIEnv *env, jclass callerClass,
																	 jobject trayIconInstance, jbyteArray iconBytes)
{
	char message[256];
	HWND hWnd;
	NOTIFYICONDATAW nid;
	TrayIconData *trayIconData;

	trayIconData = (TrayIconData *)calloc(1, sizeof(TrayIconData));
	if (trayIconData == NULL)
	{
		snprintf(message, sizeof(message), "Failed to malloc TrayIconData");
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	hWnd = CreateWindow("STATIC", "Tray", 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL);
	if (!hWnd)
	{
		FreeTrayIconData(trayIconData);
		snprintf(message, sizeof(message), "CreateWindow() failed: %d", GetLastError());
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	trayIconData->hWnd = hWnd;
	trayIconData->jni_env = env;

	trayIconData->winJFX_class = env->FindClass("ru/knoblul/winjfx/WinJFX");
	if (trayIconData->winJFX_class == NULL)
	{
		FreeTrayIconData(trayIconData);
		snprintf(message, sizeof(message), "WinJFX class not found");
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	trayIconData->popupLmbCallback_methodId = env->GetStaticMethodID(trayIconData->winJFX_class, "onTrayIconClicked", "(Ljava/lang/Object;)V");
	if (trayIconData->popupLmbCallback_methodId == NULL)
	{
		FreeTrayIconData(trayIconData);
		snprintf(message, sizeof(message), "onTrayIconClicked() not found");
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	trayIconData->popupRmbCallback_methodId = env->GetStaticMethodID(trayIconData->winJFX_class, "onTrayIconPopupSelected", "(Ljava/lang/Object;I)V");
	if (trayIconData->popupRmbCallback_methodId == NULL)
	{
		FreeTrayIconData(trayIconData);
		snprintf(message, sizeof(message), "onTrayIconPopupSelected() not found");
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	nid = {};
	nid.cbSize = sizeof(NOTIFYICONDATAW);
	nid.hWnd = hWnd;
	nid.uID = 0;
	nid.uFlags = NIF_MESSAGE | NIF_ICON;
	nid.uCallbackMessage = NOTIFICATION_TRAY_ICON_MSG;
	nid.hIcon = ReadTrayIconBytes(env, iconBytes);

	trayIconData->hMenu = CreatePopupMenu();
	trayIconData->trayIconInstance = env->NewGlobalRef(trayIconInstance);

	Shell_NotifyIconW(NIM_ADD, &nid);
	SetWindowLongPtr(hWnd, GWLP_WNDPROC, (LONG_PTR)&WndProc);
	SetWindowLongPtr(hWnd, GWLP_USERDATA, (LONG_PTR)trayIconData);
	return (jlong)hWnd;
}

void CopyJavaString(JNIEnv *env, WCHAR *dest, size_t dest_len, jstring src)
{
	if (src)
	{
		const jchar *nativeString = env->GetStringChars(src, NULL);
		if (nativeString[0] == '\0')
		{
			dest[0] = '\0';
		}
		else
		{
			wcscpy(dest, (LPCWSTR)nativeString);
		}
		env->ReleaseStringChars(src, nativeString);
	}
	else
	{
		dest[0] = '\0';
	}
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_setTrayIconTooltip(JNIEnv *env, jclass callerClass, jlong trayIconWindowHandle, jstring tooltip)
{
	HWND hWnd = (HWND)trayIconWindowHandle;
	NOTIFYICONDATAW nid;
	TrayIconData *trayIconData;

	trayIconData = (TrayIconData *)GetWindowLongPtr(hWnd, GWLP_USERDATA);

	nid = {};
	nid.cbSize = sizeof(NOTIFYICONDATAW);
	nid.uFlags = NIF_TIP;
	nid.hWnd = hWnd;

	CopyJavaString(env, nid.szTip, 64, tooltip);
	Shell_NotifyIconW(NIM_MODIFY, &nid);
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_updateTrayIconPopupItem(JNIEnv *env, jclass callerClass, jlong trayIconWindowHandle, jint itemIdentifier,
																			 jint itemTypeFlags, jstring itemName)
{
	HWND hWnd;
	TrayIconData *trayIconData;

	hWnd = (HWND)trayIconWindowHandle;
	trayIconData = (TrayIconData *)GetWindowLongPtr(hWnd, GWLP_USERDATA);
	if (!trayIconData->hMenu)
	{
		char message[256];
		snprintf(message, sizeof(message), "Corrupted tray icon data pointer");
		ThrowIllegalStateException(env, message);
		return;
	}

	if (itemName != NULL)
	{
		const jchar *nativeString = env->GetStringChars(itemName, NULL);
		AppendMenuW(trayIconData->hMenu, itemTypeFlags, itemIdentifier, (LPCWSTR)nativeString);
		env->ReleaseStringChars(itemName, nativeString);
	}
	else
	{
		RemoveMenu(trayIconData->hMenu, (UINT)itemIdentifier, 0);
	}
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_trayIconShowMessage(JNIEnv *env, jclass callerClass, jlong trayIconWindowHandle, jstring title, jstring message, jint type)
{
	HWND hWnd;
	TrayIconData *trayIconData;
	NOTIFYICONDATAW nid;

	hWnd = (HWND)trayIconWindowHandle;
	trayIconData = (TrayIconData *)GetWindowLongPtr(hWnd, GWLP_USERDATA);
	nid = {};
	nid.cbSize = sizeof(NOTIFYICONDATAW);
	nid.uFlags = NIF_INFO;
	nid.hWnd = hWnd;
	nid.uTimeout = 10000; // 10 sec
	CopyJavaString(env, nid.szInfoTitle, 64, title);
	CopyJavaString(env, nid.szInfo, 256, message);

	switch (type)
	{
	case 0:
		nid.dwInfoFlags = NIIF_ERROR;
		break;
	case 1:
		nid.dwInfoFlags = NIIF_WARNING;
		break;
	case 2:
		nid.dwInfoFlags = NIIF_INFO;
		break;
	case 3:
		nid.dwInfoFlags = NIIF_USER;
		break;
	default:
		nid.dwInfoFlags = NIIF_NONE;
		break;
	}

	Shell_NotifyIconW(NIM_MODIFY, &nid);
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_destroyTrayIcon(JNIEnv *env, jclass callerClass, jlong trayIconWindowHandle)
{
	HWND hWnd;
	TrayIconData *trayIconData;

	hWnd = (HWND)trayIconWindowHandle;
	trayIconData = (TrayIconData *)GetWindowLongPtr(hWnd, GWLP_USERDATA);
	FreeTrayIconData(trayIconData);
}

JNIEXPORT jboolean JNICALL Java_ru_knoblul_winjfx_WinJFX_isTaskbarProgressSupported(JNIEnv *env, jclass callerClass)
{
	OSVERSIONINFO osvi;
	ZeroMemory(&osvi, sizeof(OSVERSIONINFO));
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
	GetVersionEx(&osvi);
	return (jboolean)(osvi.dwMajorVersion >= 6 && osvi.dwMinorVersion >= 1);
}

JNIEXPORT jlong JNICALL Java_ru_knoblul_winjfx_WinJFX_createTaskbar(JNIEnv *env, jclass callerClass)
{
	static const GUID CLSID_ITaskbarList = {0x56FDF344, 0xFD6D, 0x11d0, {0x95, 0x8A, 0x00, 0x60, 0x97, 0xC9, 0xA0, 0x90}};
	static const IID IID_ITaskbarList3 = {0xea1afb91, 0x9e28, 0x4b86, {0x90, 0xe9, 0x9e, 0x9f, 0x8a, 0x5e, 0xef, 0xaf}};
	char message[256];
	HRESULT result;

	ITaskbarList3 *taskbar;
	result = CoCreateInstance(CLSID_ITaskbarList, 0, CLSCTX_INPROC_SERVER, IID_ITaskbarList3, (void **)&taskbar);
	if (S_OK != result)
	{
		snprintf(message, sizeof(message), "CoCreateInstance() failed: %d", result);
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	result = taskbar->HrInit();
	if (S_OK != result)
	{
		snprintf(message, sizeof(message), "HrInit() failed: %d", result);
		ThrowIllegalStateException(env, message);
		return (jlong) nullptr;
	}

	return (jlong)taskbar;
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_setTaskbarProgressState(JNIEnv *env, jclass callerClass, jlong windowHandle, jlong taskbarHandle, jint state)
{
	char message[256];
	HWND hWnd = (HWND)windowHandle;
	TBPFLAG tbpFlags;
	ITaskbarList3 *taskbar = (ITaskbarList3 *)taskbarHandle;

	switch (state)
	{
	case 0:
		tbpFlags = TBPF_NOPROGRESS;
		break;
	case 1:
		tbpFlags = TBPF_INDETERMINATE;
		break;
	case 2:
		tbpFlags = TBPF_NORMAL;
		break;
	case 3:
		tbpFlags = TBPF_ERROR;
		break;
	case 4:
		tbpFlags = TBPF_PAUSED;
		break;
	default:
		snprintf(message, sizeof(message), "Invalid taskbar state: %d", state);
		ThrowIllegalStateException(env, message);
		return;
	}

	HRESULT result = taskbar->SetProgressState(hWnd, tbpFlags);
	if (S_OK != result)
	{
		snprintf(message, sizeof(message), "SetProgressState() failed: %d", result);
		ThrowIllegalStateException(env, message);
		return;
	}
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_setTaskbarProgress(JNIEnv *env, jclass callerClass, jlong windowHandle, jlong taskbarHandle, jlong completed, jlong total)
{
	ITaskbarList3 *taskbar = (ITaskbarList3 *)taskbarHandle;
	taskbar->SetProgressValue((HWND)windowHandle, (ULONGLONG)completed, (ULONGLONG)total);
}

JNIEXPORT void JNICALL Java_ru_knoblul_winjfx_WinJFX_destroyTaskbar(JNIEnv *env, jclass callerClass, jlong taskbarHandle)
{
	ITaskbarList3 *taskbar = (ITaskbarList3 *)taskbarHandle;
	taskbar->Release();
}
