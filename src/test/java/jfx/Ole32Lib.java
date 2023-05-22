package jfx;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 29.01.2022 17:34
 *
 * @author Knoblul
 */
public interface Ole32Lib extends Library {
	Ole32Lib INSTANCE = Native.loadLibrary("ole32.dll", Ole32Lib.class);

	int CoInitialize(Pointer lpReserved);

	int CoCreateInstance(GUID rclsid, Pointer pUnkOuter, int dwClsContext, GUID riid, PointerByReference ppv);

	void CoUninitialize();

	class ITaskbarList3 extends CPPClass {
		public ITaskbarList3(Pointer pointer) {
			super(pointer);
		}

		public void release() {
			invokeInt(2);
		}

		public int init() {
			// ITaskbarList::HrInit
			return invokeInt(6);
		}
	}

	class GUID extends Structure {
		private static final List<String> FIELD_ORDER = Collections.unmodifiableList(Arrays.asList("Data1", "Data2", "Data3", "Data4"));

		/** The Data1. */
		public int Data1;

		/** The Data2. */
		public short Data2;

		/** The Data3. */
		public short Data3;

		/** The Data4. */
		public byte[] Data4;

		public GUID(int data1, short data2, short data3, byte[] data4) {
			super();
			Data1 = data1;
			Data2 = data2;
			Data3 = data3;
			Data4 = data4;
			for (String name : getFieldOrder()) {
				writeField(name);
			}
		}

		@Override
		protected List<String> getFieldOrder() {
			return FIELD_ORDER;
		}
	}
}
