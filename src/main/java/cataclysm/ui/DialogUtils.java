package cataclysm.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.common.base.Throwables;

import cataclysm.launch.Launcher;
import cataclysm.utils.Log;

/**
 * Created 16 ���. 2018 �. / 22:17:44 
 * @author Knoblul
 */
public class DialogUtils {
	public static void showError(String message, Throwable t) {
		Log.err(t, message);
		
		JDialog dialog = new JDialog(Launcher.frame, "Ошибка", true);
		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dialog.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1;
		gbc.insets.set(10, 10, 10, 10);
		dialog.add(new JLabel(message), gbc);
		dialog.setSize(500, 400);
		dialog.setLocationRelativeTo(Launcher.frame);

		gbc.weightx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets.set(0, 10, 0, 10);
		
		JTextArea ta = new JTextArea();
		ta.setFont(new Font("Consolas", Font.PLAIN, 12));
		ta.setText(Throwables.getStackTraceAsString(t));

		JScrollPane jscp = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jscp.setMaximumSize(new Dimension(400, 500));
		dialog.add(jscp, gbc);
		
		JButton ok = new JButton("Ок");
		ok.addActionListener(e -> dialog.setVisible(false));
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets.set(10, 10, 10, 10);
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		dialog.add(ok, gbc);
		
		dialog.setVisible(true);
	}
}
