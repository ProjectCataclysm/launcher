package cataclysm.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import cataclysm.io.sanitization.SanitizationManager;
import cataclysm.launch.Launcher;
import cataclysm.utils.HttpHelper;
import cataclysm.utils.LoginHolder;
import cataclysm.utils.PasswordUtil;

/**
 * 
 * <br><br><i>Created 23 июл. 2019 г. / 17:23:03</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * @author Knoblul
 */
public class LoginFrame extends JDialog {
	private static final long serialVersionUID = 5381275121483993759L;
	
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$");
	
	private File sessionFile;
	
	private JPanel contentPanel;
	
	private JLabel iconLoading;
	private JLabel errorLabel;
	private JTextField emailField;
	private JPasswordField passwordField;
	private JButton loginButton;
	
	private LoginHolder loginHolder;
	private String accessDeniedString;
	
	public LoginFrame() {
		super(Launcher.frame);
		
		sessionFile = new File(Launcher.workDir, "session.txt");
		
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setSize(400, 300);
		setResizable(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!isLoggedIn()) {
					System.exit(0);
				}
			}
		});
		setTitle("Вход");
		fill();
	}
	
	private void fill() {
		GridBagConstraints gbc = new GridBagConstraints();
		
		contentPanel = new JPanel();
		
		Font font = new Font("Dialog", Font.BOLD, 16);
		Font titleFont = new Font("Dialog", Font.BOLD, 24);
		Color color = new Color(0.3F, 0.3F, 0.3F, 1);
		
		contentPanel.setLayout(new GridBagLayout());
		
		gbc.insets.set(8, 8, 8, 8);

		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 1;
		JLabel label = new JLabel("Вход");
		label.setForeground(color);
		label.setFont(titleFont);
		contentPanel.add(label, gbc);
		
		gbc.gridy++;
		contentPanel.add(Box.createVerticalStrut(10), gbc);
		
		gbc.gridy++;
		errorLabel = new JLabel(" ");
		errorLabel.setForeground(Color.RED.brighter());
		contentPanel.add(errorLabel, gbc);
		
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.ipadx = 20;
		
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		label = new JLabel("Email");
		label.setForeground(color);
		label.setFont(font);
		contentPanel.add(label, gbc);
		emailField = new JTextField();
		emailField.setMinimumSize(new Dimension(30, 30));
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		contentPanel.add(emailField, gbc);
		
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		label = new JLabel("Пароль");
		label.setForeground(color);
		label.setFont(font);
		contentPanel.add(label, gbc);
		passwordField = new JPasswordField();
		passwordField.setMinimumSize(new Dimension(30, 30));
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		contentPanel.add(passwordField, gbc);
		
		gbc.ipadx = 0;
		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		contentPanel.add(Box.createVerticalStrut(2), gbc);
		
		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		loginButton = new JButton("Войти");
		getRootPane().setDefaultButton(loginButton);
		loginButton.addActionListener(e -> performLogin());
		loginButton.setMinimumSize(new Dimension(150, 30));
		contentPanel.add(loginButton, gbc);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		setLoadingVisible(false);
		
		ImageIcon icon = new ImageIcon(SanitizationManager.class.getResource("/icons/loading.gif"));
		iconLoading = new JLabel();
		iconLoading.setIcon(icon);
		iconLoading.setOpaque(false);
		iconLoading.setHorizontalAlignment(JLabel.CENTER);
	}
	
	private void setLoadingVisible(boolean visible) {
		loginButton.setEnabled(!visible);
		getRootPane().setDefaultButton(!visible ? loginButton : null);
		setContentPane(visible ? iconLoading : contentPanel);
		revalidate();
		repaint();
	}
	
	private void performLogin() {
		if (emailField.getText().trim().isEmpty()) {
			errorLabel.setText("Заполните поле 'email'");
			return;
		}
		
		if (!EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
			errorLabel.setText("Некорректный email");
			return;
		}
		
		if (passwordField.getPassword().length == 0) {
			errorLabel.setText("Заполните поле 'Пароль'");
			return;
		}
		
		setLoadingVisible(true);
		new Thread(this::performAuthenticateRequest).start();
	}
	
	private void saveSessionFile() {
		try (PrintWriter pw = new PrintWriter(sessionFile)) {
			pw.println("uuid:"+ loginHolder.getUUID());
			pw.println("sessionId:"+ loginHolder.getSessionId());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSessionFile() {
		if (!sessionFile.exists()) {
			return;
		}
		
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(sessionFile), Charsets.UTF_8))) {
			String sessionId = null;
			String uuid = null;
			String ln;
			while ((ln = reader.readLine()) != null) {
				String[] spl = ln.split(":");
				if (spl.length == 2) {
					String k = spl[0].trim();
					String v = spl[1].trim();
					if (k.equals("sessionId")) {
						sessionId = v;
					} else if (k.equals("uuid")) {
						uuid = v;
					}
				}
			}
			
			if (sessionId != null && uuid != null) {
				performValidateRequest(sessionId, uuid);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createLoginHolder(String result) {
		String[] spl = result.split(":");
		loginHolder = new LoginHolder(spl[0].trim(), spl[1].trim(), spl[2].trim());
		accessDeniedString = null;
		if (spl[3].equalsIgnoreCase("false")) {
			accessDeniedString = "У вас нет пропуска";
		}
		errorLabel.setText(" ");
		Launcher.frame.updateLogin();
	}
	
	private void performValidateRequest(String sessionId, String uuid) {
		Map<String, String> args = Maps.newHashMap();
		args.put("action", "validate");
		args.put("uuid", uuid);
		args.put("sessionId", sessionId);
		String result = HttpHelper.postRequest(HttpHelper.AUTH_SCRIPT, args);
		if (result.startsWith("error: ")) {
			errorLabel.setText(translateError(result));
			return;
		}
		createLoginHolder(result);
	}
	
	private void performAuthenticateRequest() {
		Map<String, String> args = Maps.newHashMap();
		args.put("action", "authenticate");
		args.put("email", emailField.getText().trim());
		args.put("password", PasswordUtil.hashPassword(new String(passwordField.getPassword())));
		String result = HttpHelper.postRequest(HttpHelper.AUTH_SCRIPT, args);
		
		setLoadingVisible(false);
		
		if (result.startsWith("error: ")) {
			errorLabel.setText(translateError(result));
			return;
		}
		
		createLoginHolder(result);
		saveSessionFile();
		setVisible(false);
	}
	
	private String translateError(String result) {
		String code = result.substring("error: ".length());
		switch (code) {
		case "invalid credentials":
			return "Неверный логин или пароль";
		case "invalid request":
			return "Некорректный запрос";
		case "you are banned":
			return "Вы забанены :/";
		case "invalid session":
			return "Недействительная сессия";
		}
		
		if (code.startsWith("db query failed: ")) {
			return "Ошибка бд: " + code.substring("db query failed: ".length());
		}
		
		return code;
	}

	public void initialize() {
		loadSessionFile();
		if (!isLoggedIn()) {
			signout(true);
		}
	}
	
	public LoginHolder getLoginHolder() {
		return loginHolder;
	}
	
	public boolean isLoggedIn() {
		return loginHolder != null;
	}
	
	public String getAccessDeniedString() {
		return accessDeniedString;
	}

	public void showFrame() {
		emailField.setText("");
		passwordField.setText("");
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

	public void signout(boolean showLoginFrame) {
		sessionFile.delete();
		loginHolder = null;
		Launcher.frame.updateLogin();
		if (showLoginFrame) {
			showFrame();
		}
	}
}
