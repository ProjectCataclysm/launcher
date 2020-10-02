package cataclysm.ui;

import cataclysm.utils.PlatformHelper;

import javax.swing.*;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 16.06.2020 19:47
 *
 * @author Knoblul
 */
public class MemorySpinnerModel extends AbstractSpinnerModel {
	private int value;

	@Override
	public Object getValue() {
		return value;
	}

	public int getIntValue() {
		return value;
	}

	@Override
	public void setValue(Object value) {
		this.value = Math.min(PlatformHelper.getMaximumMemory(), Math.max((int) value, 512));
		fireStateChanged();
	}

	@Override
	public Object getNextValue() {
		return value << 1;
	}

	@Override
	public Object getPreviousValue() {
		return value >> 1;
	}
}
