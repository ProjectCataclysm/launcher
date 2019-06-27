package cataclysm.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cataclysm.launch.Launcher;
import cataclysm.utils.Icons;
import cataclysm.utils.VersionHelper;

/**
 * Created 20 окт. 2018 г. / 21:36:58 
 * @author Knoblul
 */
public class LaunchStatusFrame extends JFrame {
	private static final long serialVersionUID = 3075656257021690777L;
	private JPanel panel;
	
	public LaunchStatusFrame() {
		Icons.setupIcons(this);
		setTitle("SME LAUNCHER " + VersionHelper.VERSION);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 400);
		setResizable(false);
		setContentPane(panel = new JPanel(new GridBagLayout()));
		setTitle(Launcher.frame.getTitle());
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
