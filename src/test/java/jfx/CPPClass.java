package jfx;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 29.01.2022 18:09
 *
 * @author Knoblul
 */
public class CPPClass {
	private final Pointer pointer;

	public CPPClass(Pointer pointer) {
		this.pointer = pointer;
	}

	public int invokeInt(int vtableId) {
		Pointer mPtr = pointer.getPointer(0);
		return Function.getFunction(mPtr.getPointer((long) vtableId * Native.POINTER_SIZE)).invokeInt(new Object[]{pointer});
	}
}
