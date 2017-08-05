package tech.conexus.alkharidkiller.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public class ScriptFrame extends JFrame{
	public ScriptFrame() {
		super("Al Kharid Warrior Killer");
		
		JList list = createLootList();
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.setPreferredSize(new Dimension(100, 300));
		
		int border = 5;
		panel.setBorder(new EmptyBorder(border, border, border, border));
		panel.add(list, BorderLayout.CENTER);
		panel.add(new JButton("Add"), BorderLayout.SOUTH);

		add(panel, BorderLayout.WEST);
		setSize(400, 500);
		//pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public JFrame addFrame() {
		JFrame addFrame = new JFrame("Add");
		
		JPanel wrapper = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Local loot");
		JList list = createLootList();
		list.setPreferredSize(new Dimension(200, 200));
		
		JPanel manualPanel = new JPanel();
		manualPanel.add(new JTextField("Enter ID..."));
		manualPanel.add(new JButton("Add"));
		
		wrapper.add(label, BorderLayout.NORTH);
		wrapper.add(list, BorderLayout.CENTER);
		
		addFrame.add(wrapper, BorderLayout.NORTH);
		addFrame.add(manualPanel, BorderLayout.SOUTH);
		addFrame.setSize(300, 400);
		addFrame.setLocationRelativeTo(null);
		addFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		return addFrame;
	}
	
	public JList createLootList() {
		JList list = new JList<>(new String[] {"Hello"});
		return list;
	}
	
	public static final void main(String[] args) {
		new ScriptFrame().addFrame().setVisible(true);
	}
}
