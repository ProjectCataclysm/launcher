package cataclysm.launcher.ui;

import cataclysm.launcher.utils.Log;
import com.google.common.base.Throwables;

import javax.swing.*;
import java.awt.*;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.08.2022 15:16
 *
 * @author Knoblul
 */
public class AwtErrorDialog {
	public static void showError(String message, Throwable t) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
		}

		Log.err(t, message);

		if (t == null) {
			JOptionPane.showMessageDialog(null, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
		} else {
			JDialog dialog = new JDialog((Frame) null, "Ошибка", true);
			dialog.setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.weightx = 1;
			gbc.insets.set(10, 10, 10, 10);
			dialog.add(new JLabel(message), gbc);
			dialog.setSize(500, 400);
			dialog.setLocationRelativeTo(null);

			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.insets.set(0, 10, 0, 10);

			JTextArea ta = new JTextArea();
			ta.setFont(new Font("Consolas", Font.PLAIN, 12));
			ta.setText(Throwables.getStackTraceAsString(t));

			JScrollPane scroll = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setMaximumSize(new Dimension(400, 500));
			dialog.add(scroll, gbc);

			JButton ok = new JButton("Ок");
			ok.addActionListener(e -> dialog.setVisible(false));

			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets.set(10, 10, 10, 10);
			gbc.gridy = 2;
			gbc.weightx = 0;
			gbc.weighty = 0;
			dialog.add(ok, gbc);

			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}

		System.exit(1);
	}
}
