package jfx;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 29.01.2022 17:36
 *
 * @author Knoblul
 */
public class HANDLE extends IntegerType {
	public HANDLE() {
		super(Native.POINTER_SIZE);
	}
}
