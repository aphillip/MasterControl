package one.andgate.MasterControl.swing;

import javax.swing.JButton;
import one.andgate.MasterControl.xml.NodeEx;


public class JButtonWithNodeEx extends JButton
{
	NodeEx m_Node=null;
	
	public JButtonWithNodeEx(String strLabel, NodeEx aNode)
	{
		super(strLabel);
		m_Node=aNode;
	}
	
	NodeEx getNodeEx(){ return m_Node; }
}
