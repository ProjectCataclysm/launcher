package cataclysm.launcher.ui;

import cataclysm.launcher.Launcher;
import cataclysm.launcher.account.Profile;
import cataclysm.launcher.account.Session;
import cataclysm.launcher.download.santation.SanitationManager;
import cataclysm.launcher.utils.HttpHelper;
import com.google.common.collect.Sets;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <br><br><i>Created 23 июл. 2019 г. / 17:23:03</i><br>
 * SME REDUX / NOT FOR FREE USE!
 *
 * @author Knoblul
 */
public class LoginFrame extends JDialog {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-_.+]*[\\w-_.]@([\\w]+\\.)+[\\w]+[\\w]$");

	private final Path sessionFilePath;

	private JPanel contentPanel;

	private JLabel iconLoading;
	private JLabel errorLabel;
	private JTextField loginField;
	private JPasswordField passwordField;
	private JButton loginButton;

	private Session session;

	@SuppressWarnings("unused") // XXX мб потом :)
	private String accessDeniedString;

	public LoginFrame() {
		super(Launcher.frame);

		sessionFilePath = Launcher.workDirPath.resolve("session.txt");

		setModalityType(ModalityType.DOCUMENT_MODAL);
		setSize(400, 300);
		setResizable(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (isNotLoggedIn()) {
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
		errorLabel.setMaximumSize(new Dimension(350, 80));
		errorLabel.setHorizontalAlignment(JLabel.CENTER);
		errorLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		contentPanel.add(errorLabel, gbc);

		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.ipadx = 20;

		gbc.gridy++;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		label = new JLabel("Логин");
		label.setForeground(color);
		label.setFont(font);
		contentPanel.add(label, gbc);
		loginField = new JTextField();
		loginField.setMinimumSize(new Dimension(30, 30));
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		contentPanel.add(loginField, gbc);

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

		ImageIcon icon = new ImageIcon(
				Objects.requireNonNull(SanitationManager.class.getResource("/icons/loading.gif")));
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
		if (loginField.getText().trim().isEmpty()) {
			setError("Заполните поле 'Логин'");
			return;
		}

		if (loginField.getText().contains("@") && !EMAIL_PATTERN.matcher(loginField.getText().trim()).matches()) {
			setError("Некорректный email");
			return;
		}

		if (passwordField.getPassword().length == 0) {
			setError("Заполните поле 'Пароль'");
			return;
		}

		setLoadingVisible(true);
		new Thread(this::performAuthenticateRequest).start();
	}

	private void saveSessionFile() {
		try {
			Files.write(sessionFilePath, Collections.singletonList(session.getAccessToken()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadSessionFile() {
		try {
			String sessionId = String.join("", Files.readAllLines(sessionFilePath));
			if (!sessionId.isEmpty()) {
				validateAccessToken(sessionId);
			}
		} catch (FileNotFoundException | NoSuchFileException ignored) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void parseSession(JSONObject response) {
		JSONObject userNode = (JSONObject) response.get("user");
		JSONObject profileNode = (JSONObject) userNode.get("profile");
		Profile profile = new Profile((String) profileNode.get("uuid"), (String) profileNode.get("email"),
				(String) profileNode.get("username"));

		Set<String> ticketsRaw = Sets.newHashSet();
		JSONArray ticketsNode = (JSONArray) userNode.get("tickets");
		for (Object o : ticketsNode) {
			JSONObject node = (JSONObject) o;
			ticketsRaw.add(node.get("type") + ":" + node.get("name"));
		}

		session = new Session(profile, (String) response.get("accessToken"), ticketsRaw,
				((Number) userNode.get("balance")).intValue());
		setError(null);
		Launcher.frame.updateLogin();
	}

	@SuppressWarnings("unchecked")
	private void validateAccessToken(String accessToken) {
		try {
			JSONObject request = new JSONObject();
			request.put("accessToken", accessToken);
			parseSession(HttpHelper.postJsonRequest("client/validate", request));
		} catch (IOException e) {
			e.printStackTrace();
			setError(translateError(e));
			session = null;
		}
	}

	@SuppressWarnings("unchecked")
	private void performAuthenticateRequest() {
		try {
			JSONObject request = new JSONObject();
			request.put("login", loginField.getText().trim());
			request.put("password", new String(passwordField.getPassword()));

			JSONObject response = HttpHelper.postJsonRequest("client/auth", request);
			setLoadingVisible(false);
			parseSession(response);
			saveSessionFile();
			setVisible(false);
		} catch (IOException e) {
			setLoadingVisible(false);
			e.printStackTrace();
			setError(translateError(e));
		}
	}

	private static String translateError(IOException e) {
		String code = e.getLocalizedMessage();
		switch (code) {
			case "api.error.unavailable":
				return "Сервис временно недоступен";
			case "api.error.internal":
				return "Внутренняя ошибка сервера";
			case "api.error.invalidSession":
				return "Сессия устарела, повторите вход";
			case "api.error.clientAuth.invalidCredentials":
				return "Неверно введен логин или пароль";
			case "api.error.clientAuth.limitReached":
				return "Превышено максимальное число допустимых попыток. Попробуйте через 5 минут.";
			case "api.error.clientBanned":
				return "Вы забанены :/";
			default:
				return "Неизвестная ошибка (" + code + ")";
		}
	}

	private void setError(String error) {
		if (error == null || error.trim().length() == 0) {
			error = "";
		}
		errorLabel.setText("<html>" + error + "</html>");
	}

	public void initialize() {
		loadSessionFile();
		if (isNotLoggedIn()) {
			logout(true);
		}
	}

	public Session getSession() {
		return session;
	}

	public boolean isNotLoggedIn() {
		return session == null;
	}

	public String getAccessDeniedString() {
		return accessDeniedString;
	}

	public void showFrame() {
		loginField.setText("");
		passwordField.setText("");
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

	@SuppressWarnings("unchecked")
	public void logout(boolean showLoginFrame) {
		if (session != null) {
			JSONObject request = new JSONObject();
			request.put("sessionId", session.getAccessToken());
			try {
				HttpHelper.postJsonRequest("client/invalidate", request);
			} catch (IOException e) {
				e.printStackTrace();
				setError(translateError(e));
			}
		}

		try {
			Files.deleteIfExists(sessionFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		session = null;
		Launcher.frame.updateLogin();
		if (showLoginFrame) {
			showFrame();
		}
	}
}
