package cataclysm.launcher.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cataclysm.launcher.Launcher;
import cataclysm.launcher.utils.Icons;
import cataclysm.launcher.selfupdate.LauncherVersionManager;

/**
 * Created 20 ���. 2018 �. / 21:36:58 
 * @author Knoblul
 */
public class LaunchStatusFrame extends JFrame {
	private static final long serialVersionUID = 3075656257021690777L;
	private JPanel panel;
	
	public LaunchStatusFrame() {
		Icons.setupIcons(this);
		setTitle("Project Cataclysm Launcher v" + LauncherVersionManager.VERSION);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 400);
		setResizable(false);
		setContentPane(panel = new JPanel(new GridBagLayout()));
		setTitle(Launcher.frame.getTitle());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void fill(JComponent component) {
		setLayout(new BorderLayout());
		panel.removeAll();
		panel.add(component);
		panel.revalidate();
		panel.repaint();
		panel.validate();
	}
	
	public void showFrame() {
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
