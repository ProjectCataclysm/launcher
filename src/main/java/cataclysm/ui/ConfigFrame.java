package cataclysm.ui;

import cataclysm.launch.Launcher;
import cataclysm.utils.Log;
import cataclysm.utils.LoginHolder;
import cataclysm.utils.PlatformHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created 28 ����. 2018 �. / 21:34:44
 *
 * @author Knoblul
 */
public class ConfigFrame extends JDialog {
	public Path gameDirectoryPath;
	public int memory;
	public boolean limitMemory;
	public int texturesQuality = 2;
	private Properties props = new Properties();
	private Path configFile;
	private JLabel loginStatusLabel;
	private JButton signOutButton;

	public ConfigFrame() {
		super(Launcher.frame);

		configFile = Launcher.workDirPath.resolve("launcher.cfg");
		gameDirectoryPath = PlatformHelper.getDefaultGameDirectory();
		Log.msg("Config file %s", configFile);

		memory = PlatformHelper.getMaximumMemory();
		limitMemory = false;
		Log.msg("Maximum memory=%dmb", memory);

		loadConfig();

		setModalityType(ModalityType.DOCUMENT_MODAL);
		setSize(500, 400);
		setResizable(false);
		setTitle("Настройки");

		fill();
	}

	/**
	 * Returns the input value rounded up to the next highest power of two.
	 */
	public static int roundUpToPowerOfTwo(int value) {
		int result = value - 1;
		result |= result >> 1;
		result |= result >> 2;
		result |= result >> 4;
		result |= result >> 8;
		result |= result >> 16;
		return result + 1;
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
		signOutButton.addActionListener(e -> Launcher.loginFrame.logout(true));
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
		jtf.setText(gameDirectoryPath.toAbsolutePath().toString());
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

		JCheckBox limitMemCheckBox = new JCheckBox("Выделяемая память (МБ):", limitMemory);
		limitMemCheckBox.setFont(limitMemCheckBox.getFont().deriveFont(Font.PLAIN, 20));
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.insets.set(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		add(limitMemCheckBox, gbc);

		MemorySpinnerModel memSpinnerModel = new MemorySpinnerModel();

		JSpinner memSpinner = new JSpinner(memSpinnerModel);
		memSpinner.setPreferredSize(new Dimension(200, 28));
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		gbc.insets.set(0, 40, 0, 0);
		add(memSpinner, gbc);

		limitMemCheckBox.addActionListener(e -> {
			limitMemory = limitMemCheckBox.isSelected();
			saveConfig();
			memSpinner.setEnabled(limitMemory);
			memSpinnerModel.setValue(limitMemory ? memory : 0);
		});
		memSpinner.setEnabled(limitMemory);
		memSpinnerModel.setValue(limitMemory ? memory : 0);

		label = new JLabel("Качество текстур");
		label.setFont(label.getFont().deriveFont(Font.PLAIN, 20));
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.insets.set(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		add(label, gbc);

		JComboBox<String> texturesQualityComboBox = new JComboBox<>(new String[] { "Низк.", "Средн.", "Выс." });
		texturesQualityComboBox.setFont(texturesQualityComboBox.getFont().deriveFont(Font.PLAIN, 18));
		texturesQualityComboBox.setSelectedIndex(texturesQuality);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.insets.set(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		add(texturesQualityComboBox, gbc);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				memory = Math.min(PlatformHelper.getMaximumMemory(), roundUpToPowerOfTwo(memSpinnerModel.getIntValue()));
				memSpinnerModel.setValue(memory);
				texturesQuality = texturesQualityComboBox.getSelectedIndex();
				saveConfig();
			}
		});
	}

	private void selectGameDir(JTextField field) {
		JFileChooser jfc = new JFileChooser(gameDirectoryPath.toFile());
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setMultiSelectionEnabled(false);
		jfc.setDialogTitle("Выбор папки с игрой");

		int result = jfc.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			gameDirectoryPath = jfc.getSelectedFile().toPath();
			field.setText(gameDirectoryPath.toAbsolutePath().toString());
		}
	}

	public void showDialog() {
		updateLogin();
		setLocationRelativeTo(Launcher.frame);
		setVisible(true);
	}

	public void loadConfig() {
		try (InputStream in = Files.newInputStream(configFile)) {
			Log.msg("Loading config file...");
			props.clear();
			props.load(in);

			gameDirectoryPath = Paths.get(props.getProperty("GameDirectory", gameDirectoryPath.toString()));
			memory = roundUpToPowerOfTwo(Integer.parseInt(props.getProperty("memory", Integer.toString(memory))));
			limitMemory = Boolean.parseBoolean(props.getProperty("limitMemory", Boolean.toString(limitMemory)));
			texturesQuality = Math.min(Math.max(Integer.parseInt(props.getProperty("texturesQuality", Integer.toString(texturesQuality))), 0), 3);
		} catch (FileNotFoundException | NoSuchFileException e) {
			saveConfig();
		} catch (Exception e) {
			Log.err(e, "Can't load config file");
		}

		if (memory < 256 || memory > 16 * 1024) {
			memory = 1024;
		}
	}

	public void saveConfig() {
		try (OutputStream out = Files.newOutputStream(configFile)) {
			Log.msg("Saving config file...");
			props.clear();

			props.setProperty("GameDirectory", gameDirectoryPath.toAbsolutePath().toString());
			props.setProperty("memory", Long.toString(memory));
			props.setProperty("limitMemory", Boolean.toString(limitMemory));
			props.setProperty("texturesQuality", Integer.toString(texturesQuality));
			props.store(out, "");
		} catch (IOException e) {
			Log.err(e, "Can't save config file");
		}
	}
}
