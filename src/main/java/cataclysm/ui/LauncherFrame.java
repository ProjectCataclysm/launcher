package cataclysm.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import cataclysm.launch.Launcher;
import cataclysm.utils.Icons;
import cataclysm.utils.VersionHelper;

/**
 * Created 28 сент. 2018 г. / 16:55:45 
 * @author Knoblul
 */
public class LauncherFrame extends JFrame {
	private static final long serialVersionUID = 397595061352916545L;
	
	private JProgressBar progress;
	private JPanel launcherContent;
	
	public LauncherFrame() {
		Icons.setupIcons(this);
		setTitle("Project Cataclysm Launcher v" + VersionHelper.VERSION);
		setSize(350, 400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(null);
		fill();
	}
	
	private void fill() {
		launcherContent = new JPanel();
		launcherContent.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		JButton configButton = new JButton();
		configButton.addActionListener(e -> Launcher.config.showDialog());
		
		try {
			configButton.setIcon(new ImageIcon(Icons.readImage("settings.png")));
			configButton.setRolloverIcon(new ImageIcon(Icons.readImage("settings_r.png")));
		} catch (IOException e) {
			e.printStackTrace();
			configButton.setText("<SETTINGS>");
		}
		
		configButton.setContentAreaFilled(false);
		configButton.setFocusPainted(false);
		configButton.setBorderPainted(false);
		configButton.setPreferredSize(new Dimension(64, 64));
		configButton.setMaximumSize(configButton.getPreferredSize());
		
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		launcherContent.add(configButton, gbc);
		
		gbc.weightx = 0;
		gbc.weighty = 0;
		
		JButton startButton = new JButton("Запуск");
		startButton.addActionListener(e -> Launcher.launch());
		startButton.setPreferredSize(new Dimension(startButton.getPreferredSize().width, 50));
		
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.set(8, 8, 8, 8);
		gbc.gridy = 1;
		launcherContent.add(startButton, gbc);
		
		gbc.insets.set(0, 0, 0, 0);
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.SOUTH;

		progress = new JProgressBar() {
			private static final long serialVersionUID = -3797303991723075250L;

			@Override
			public void paint(Graphics g) {
				if (getValue() > getMinimum() && getValue() < getMaximum()) {
					super.paint(g);
				}
			}
		};
		progress.setPreferredSize(new Dimension(20, 8));
		progress.setBorder(null);
		progress.setForeground(Color.GREEN.darker());
//		launcherContent.add(progress, gbc);
	}
	
	public void showVersionChecker(JComponent component) {
		setContentPane(component);
		setVisible(true);
		revalidate();
	}
	
	public void showLauncher() {
		setContentPane(launcherContent);
		revalidate();
	}
}
