import cataclysm.utils.jfx.Kernel32Lib;
import cataclysm.utils.jfx.Ole32Lib;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 29.01.2022 16:53
 *
 * @author Knoblul
 */
public class WinTest {


	public static void main(String[] args) {
		Kernel32Lib.OSVERSIONINFO struct = new Kernel32Lib.OSVERSIONINFO();
		if (!Kernel32Lib.INSTANCE.GetVersionExW(struct)) {
			throw new RuntimeException("Windows error: " + Kernel32Lib.INSTANCE.GetLastError());
		}

		int version = struct.getDwMajorVersion() * 10 + struct.getDwMinorVersion();
		if (version < 61) {

		}

		int S_OK = 0;
		int S_FALSE = 1;
		int CLSCTX_INPROC_SERVER = 0x1;
		Ole32Lib.GUID CLSID_ITaskbarList = new Ole32Lib.GUID(0x56FDF344, (short) 0xFD6D, (short) 0x11d0, new byte[] {(byte) 0x95, (byte) 0x8A, (byte) 0x00, (byte) 0x60, (byte) 0x97, (byte) 0xC9, (byte) 0xA0, (byte) 0x90});
		Ole32Lib.GUID IID_ITaskbarList3 = new Ole32Lib.GUID(0xea1afb91, (short) 0x9e28, (short) 0x4b86, new byte[] {(byte) 0x90, (byte) 0xe9, (byte) 0x9e, (byte) 0x9f, (byte) 0x8a, (byte) 0x5e, (byte) 0xef, (byte) 0xaf});

		int result = Ole32Lib.INSTANCE.CoInitialize(null);
		if (result != S_OK && result != S_FALSE) {
			throw new RuntimeException("CoInitialize failed: " + result);
		}

		result = Ole32Lib.INSTANCE.CoInitialize(null);
		if (result != S_OK && result != S_FALSE) {
			throw new RuntimeException("CoInitialize failed: " + result);
		}

		PointerByReference ptr = new PointerByReference();
		result = Ole32Lib.INSTANCE.CoCreateInstance(CLSID_ITaskbarList, null, CLSCTX_INPROC_SERVER, IID_ITaskbarList3, ptr);
		if (result != S_OK) {
			throw new RuntimeException("CoCreateInstance failed: " + result);
		}

		Ole32Lib.ITaskbarList3 taskbar = new Ole32Lib.ITaskbarList3(ptr.getValue());

		result = taskbar.init();
		if (result != S_OK) {
			throw new RuntimeException("ITaskbarList::HrInit failed: " + result);
		}



		taskbar.release();

		Ole32Lib.INSTANCE.CoUninitialize();
	}
}
