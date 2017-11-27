package one.andgate.MasterControl.swing;

import javax.swing.JPasswordField;

public class JPasswordFieldWithHashIndicator extends JPasswordField
{
	boolean m_bHashOn=false;	
	String m_InitialValue;
	public boolean isHashOn(){ return m_bHashOn; }
	public void setHashOn(boolean bHashOn){ m_bHashOn=bHashOn; }
	public String getInitialValue(){ return m_InitialValue; }
	public void setInitialValue(String aValue){ m_InitialValue=aValue; }
}
