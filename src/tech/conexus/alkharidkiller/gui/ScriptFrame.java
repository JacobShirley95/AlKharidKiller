package tech.conexus.alkharidkiller.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class ScriptFrame extends JFrame{
	public ScriptFrame() {
		super("Al Kharid Warrior Killer");
		
		JList list = createLootList();
		
		JPanel panel = new JPanel(new GridLayout());
		panel.setPreferredSize(new Dimension(100, 300));
		panel.add(list);

		add(panel, BorderLayout.WEST);
		setSize(400, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public JList createLootList() {
		JList list = new JList<>(new String[] {"Hello"});
		return list;
	}
	
	public static final void main(String[] args) {
		new ScriptFrame().setVisible(true);
	}
}
