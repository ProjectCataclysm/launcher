package cataclysm.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.google.common.base.Charsets;

import cataclysm.launch.Launcher;
import cataclysm.utils.Log;
import cataclysm.utils.LoginHolder;
import cataclysm.utils.PlatformHelper;

/**
 * Created 28 ����. 2018 �. / 21:34:44
 * 
 * @author Knoblul
 */
public class ConfigFrame extends JDialog {
	private static final long serialVersionUID = -7097069158813087568L;

	private Properties props = new Properties();

	private File configFile;
	public File gameDirectory;
	public long memory;

	private JLabel loginStatusLabel;
	private JButton signOutButton;

	public ConfigFrame() {
		super(Launcher.frame);

		configFile = new File(Launcher.workDir, "config.cfg");
		gameDirectory = PlatformHelper.getDefaultGameDirectory();
		Log.msg("Config file %s", configFile);
		
		memory = PlatformHelper.getAvaibleMemory();
		Log.msg("Maximum memory usage %dmb", memory/1024/1024);

		loadConfig();

		setModalityType(ModalityType.DOCUMENT_MODAL);
		setSize(500, 260);
		setResizable(false);
		setTitle("Настройки");
		
		fill();
	}
	
	public void updateLogin() {
		LoginHolder holder = Launcher.loginFrame.getLoginHolder();
		if (holder != null) {
			loginStatusLabel.setText("Вы вошли как " + holder.getUsername());
			signOutButton.setEnabled(true);
		} else {
			loginStatusLabel.setText(" ");
			signOutButton.setEnabled(false);
		}
	}

	private void fill() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		
		loginStatusLabel = new JLabel(" ");
		loginStatusLabel.setVerticalAlignment(JLabel.CENTER);
		signOutButton = new JButton("Выход");
		signOutButton.setEnabled(false);
		signOutButton.addActionListener(e -> Launcher.loginFrame.signout(true));
		signOutButton.setMaximumSize(new Dimension(90, 30));
		signOutButton.setPreferredSize(signOutButton.getMaximumSize());
		
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(Box.createHorizontalBox(), gbc);
		
		gbc.insets.set(10, 0, 2, 0);
		gbc.gridy = 0;
		gbc.gridx = 1;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		add(loginStatusLabel, gbc);
		
		gbc.insets.set(2, 0, 2, 0);
		gbc.gridy = 0;
		gbc.gridx = 2;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		add(signOutButton, gbc);
		
		JLabel label = new JLabel("Папка с игрой:");
		label.setFont(label.getFont().deriveFont(Font.PLAIN, 20));

		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.insets.set(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		add(label, gbc);

		JTextField jtf = new JTextField();
		jtf.setEditable(false);
		jtf.setText(gameDirectory.getAbsolutePath());
		jtf.setPreferredSize(new Dimension(20, 28));
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets.set(0, 40, 0, 0);
		add(jtf, gbc);

		JButton select = new JButton("Выбрать");
		select.setMaximumSize(new Dimension(90, 30));
		select.setPreferredSize(select.getMaximumSize());
		select.addActionListener(e -> selectGameDir(jtf));
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.insets.set(0, 4, 0, 10);
		add(select, gbc);

		label = new JLabel("Выделяемая память (МБ):");
		label.setFont(label.getFont().deriveFont(Font.PLAIN, 20));
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.insets.set(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		add(label, gbc);
		
		if (memory < 256 * 1024 * 1024) {
			memory = 256 * 1024 * 1024;
		}
		SpinnerNumberModel model = new SpinnerNumberModel((int) (memory/1024/1024), 256, 16*1024, 1);

		JSpinner memSpinner = new JSpinner(model);
		memSpinner.setPreferredSize(new Dimension(200, 28));
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		gbc.insets.set(0, 40, 0, 0);
		add(memSpinner, gbc);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				memory = Long.valueOf(memSpinner.getModel().getValue().toString()) * 1024 * 1024;
				saveConfig();
			}
		});
	}
	
	private void selectGameDir(JTextField field) {
		JFileChooser jfc = new JFileChooser(gameDirectory);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setMultiSelectionEnabled(false);
		jfc.setDialogTitle("Выбор папки с игрой");
		
		int result = jfc.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			gameDirectory = jfc.getSelectedFile();
			field.setText(gameDirectory.getAbsolutePath());
		}
	}

	public void showDialog() {
		updateLogin();
		setLocationRelativeTo(Launcher.frame);
		setVisible(true);
	}

	public void loadConfig() {
		if (!configFile.exists()) {
			saveConfig();
		}

		try (InputStream in = new FileInputStream(configFile);
				InputStreamReader isr = new InputStreamReader(in, Charsets.UTF_8)) {
			Log.msg("Loading config file...");
			props.clear();
			props.load(in);

			gameDirectory = new File(props.getProperty("GameDirectory", gameDirectory.getAbsolutePath()));
			memory = Long.valueOf(props.getProperty("memory", Long.toString(memory)));
		} catch (IOException e) {
			Log.err(e, "Can't save config file");
		}
	}

	public void saveConfig() {
		try (OutputStream out = new FileOutputStream(configFile);
				OutputStreamWriter osw = new OutputStreamWriter(out, Charsets.UTF_8)) {
			Log.msg("Saving config file...");
			props.clear();

			props.setProperty("GameDirectory", gameDirectory.getAbsolutePath());
			props.setProperty("memory", Long.toString(memory));

			props.store(out, "");
		} catch (IOException e) {
			Log.err(e, "Can't save config file");
		}
	}
}
