package one.andgate.MasterControl.swing;

//import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import one.andgate.MasterControl.xml.NodeEx;
import java.util.List;
import java.util.Vector;
import java.util.Properties;
import java.io.StringReader;
import java.io.OutputStream;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeModel;


public class XMLEditorPanel extends JPanel implements ActionListener, TreeSelectionListener, ComponentListener, KeyListener
{
	JTreeEx m_Tree;
	JPanel m_AttributePanel=new JPanel(), m_FlowPanel=new JPanel(new FlowLayout()), m_AttributeParentPanel=new JPanel(new BorderLayout());
	JSplitPane m_SplitPane;
	JScrollPane m_ScrollPane;
	Vector<JLabel> m_AttributeLabels=new Vector<JLabel>();
	GridLayout m_GridLayout=new GridLayout(0,2);
	Vector<JComponent> m_AttributeControls=new Vector<JComponent>();
	TreePath m_CachedTreeSelectionPath=null;
	NodeEx m_ValidationXml, m_RootNode;
	boolean m_bPreparedNode=false; //If a node has just been prepared, its default attribute will get focus
	
	
	
	public XMLEditorPanel(NodeEx aNode, NodeEx validationXml)
	{
		super();
		setLayout(new BorderLayout());
		m_RootNode=aNode;
		m_ValidationXml=validationXml;
		m_Tree=new JTreeEx(aNode,validationXml,this);
		m_GridLayout.setVgap(3);
		m_AttributePanel.setLayout(m_GridLayout);		
		m_FlowPanel.add(m_AttributePanel);
		m_AttributeParentPanel.add(new JLabel("Attributes"),BorderLayout.PAGE_START);
		//m_AttributeParentPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),"Attributes"));
		m_ScrollPane=new JScrollPane(m_FlowPanel);
		m_AttributeParentPanel.add(m_ScrollPane,BorderLayout.CENTER);
		m_AttributeParentPanel.addComponentListener(this);
		
		m_SplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(m_Tree), m_AttributeParentPanel);
		m_SplitPane.setOneTouchExpandable(true);
		m_SplitPane.setResizeWeight(1.0);
		m_SplitPane.setDividerLocation(300);

		add(m_SplitPane,BorderLayout.CENTER);
		m_Tree.addTreeSelectionListener(this);	
		m_Tree.addKeyListener(this);	
	}		
	
	int parseInt(String num){ return parseInt(num,0); }
	
	int parseInt(String num, int defaultInt)
	{
		try 
		{ 
			if(num==null || num.length()==0)
			    return defaultInt;
			return Integer.parseInt(num);
	    }
		catch(Exception e){ }
		return defaultInt;
	}
	
	void setAttributePanelSize()
	{
		int w=((int)getSize().getWidth())-m_SplitPane.getDividerLocation();
		Dimension d=new Dimension(w-30,m_AttributeControls.size() * (20 + m_GridLayout.getVgap()));	
	    m_ScrollPane.setPreferredSize(d);
	    m_FlowPanel.setPreferredSize(d);
	    m_FlowPanel.setMinimumSize(d);
	    m_AttributePanel.setPreferredSize(d);
	    m_AttributePanel.setMinimumSize(d);
	}
	
	
	public void componentHidden(ComponentEvent e){}
    public void componentShown(ComponentEvent e){}     
    
    public void componentMoved(ComponentEvent e){}
      
    public void componentResized(ComponentEvent e)
    {
		if(e.getSource()==m_AttributeParentPanel)
		{
			//Rebuilding attribute panel overcomes 
			//resizing issues when only setAttributePanelSize()
			//is called instead.
			valueChanged(null);
		}
	}
         
    

	private boolean valueIsPasswordType(String sAttributeName)
    {
        if(sAttributeName.equalsIgnoreCase("password") ||
           sAttributeName.equalsIgnoreCase("pass") ||
           sAttributeName.equalsIgnoreCase("passwd") ||
           sAttributeName.equalsIgnoreCase("pwd") ||
           sAttributeName.equalsIgnoreCase("pw") ||
           sAttributeName.toLowerCase().indexOf("password")>-1)
           return true;
        return false;
    }

	public void saveAttributeChanges()
	{
		if(m_CachedTreeSelectionPath!=null)
		{
			Object obj=m_CachedTreeSelectionPath.getLastPathComponent();
			if(obj instanceof JTreeXMLNode)
			{
				NodeEx userObj=(NodeEx)((JTreeXMLNode)obj).getUserObject();
				int nRowCount=m_AttributeControls.size();
				for(int i=0;i<nRowCount;i++)
				{
					NodeEx n=userObj.getNode("./@" + m_AttributeLabels.elementAt(i).getText());

					if(n!=null)
					{
						JComponent comp=m_AttributeControls.elementAt(i);
						if(comp instanceof JComboBox)
						    n.setValue(((JComboBox)comp).getSelectedItem().toString());
						else if(comp instanceof JPasswordFieldWithHashIndicator)
						{
							JPasswordFieldWithHashIndicator p=(JPasswordFieldWithHashIndicator)comp;

							String pwStr=new String(p.getPassword());
							if( pwStr.equals(p.getInitialValue()) )
							    continue;

							if(p.isHashOn() && pwStr!=null && pwStr.length()>0)
							    n.setValue( String.valueOf( pwStr.hashCode() ) );
							else
  						            n.setValue(pwStr);
						}
						else if(comp instanceof JTextField)
						    n.setValue(((JTextField)comp).getText());
					}
				}
				userObj.refreshToStringExpression();
				((DefaultTreeModel)m_Tree.getModel()).nodeChanged( (JTreeXMLNode)obj );
			}
		}
		m_CachedTreeSelectionPath=m_Tree.getSelectionPath();
	}

	private Object getSelectedNode()
	{
		TreePath selectedPath = m_Tree.getSelectionPath();
		if(selectedPath==null)
		    return null;
		return selectedPath.getLastPathComponent();
	}
	
	public void valueChanged(TreeSelectionEvent event)
	{
		JComponent focusedComponent=null;	
		try
		{
			saveAttributeChanges();
			TreePath selectedPath = m_Tree.getSelectionPath();	
			m_AttributePanel.removeAll();
			m_AttributeLabels.clear();
			m_AttributeControls.clear();
			
			if(selectedPath==null)
			{
				setAttributePanelSize(); //m_AttributeParentPanel.validate() is not refreshing m_AttributeParentPanel without this? Not sure why.
				m_AttributeParentPanel.validate();
				return;
			}
			
			Object obj=selectedPath.getLastPathComponent();		
						
			if(obj instanceof JTreeXMLNode)
			{
				NodeEx userObj=(NodeEx)((JTreeXMLNode)obj).getUserObject();            
				List<NodeEx> attrs=userObj.getNodes("./@*");							

				for(NodeEx n : attrs)
				{				
					String attrName=n.getString("fn:name()");
					JLabel l=new JLabel(attrName);
					m_AttributeLabels.add(l);									
					
					String configStr=m_ValidationXml.getString( ((JTreeXMLNode)obj).getPathAsString() + "/@" + attrName);
					Properties p=new Properties();
					p.load(new StringReader(configStr.replace(";","\n")));
					
					if(p.getProperty("hidden")!=null && p.getProperty("hidden").equalsIgnoreCase("true"))
					    continue;
										
					if(p.getProperty("options")!=null)
					{
						String sOptionsStr=p.getProperty("options");
						String[] options;
						boolean bIsEditable=false;
						if(sOptionsStr.endsWith(":editable"))
						{
							sOptionsStr=sOptionsStr.replaceAll(":editable","");
							bIsEditable=true;
						}
						
						if(sOptionsStr.startsWith("xpath:"))
						{
							sOptionsStr=sOptionsStr.replace("xpath:","");
							Vector<String> vecItems=new Vector<String>();
							List<NodeEx> items=m_RootNode.getNodes(sOptionsStr);
							
							int nLen=items.size();
							options=new String[nLen];
							for(int i=0;i<nLen;i++)
							{
								options[i]=items.get(i).getValue();
							}
							
						}
						else
						{
							options=p.getProperty("options").replaceAll(" ,",",").replaceAll(", ",",").split(",");
						}
						@SuppressWarnings("unchecked") JComboBox cbo=new JComboBox(options);						
						if(bIsEditable)
						{							
							cbo.setEditable(true);
							cbo.setSelectedItem(n.getString("text()"));
						}
						else
						{
						    cbo.setSelectedItem(n.getString("text()"));
						}
						m_AttributeControls.add(cbo);
					}
					else if(valueIsPasswordType(attrName))
					{
						JPasswordFieldWithHashIndicator pw=new JPasswordFieldWithHashIndicator();
						String sValue=n.getString("text()");
						if(sValue!=null && sValue.length()>0)
    						    pw.setText(sValue);
						pw.setInitialValue(n.getString("text()"));
						if(p.getProperty("hash")!=null && p.getProperty("hash").equalsIgnoreCase("true"))
					        pw.setHashOn(true);
						m_AttributeControls.add(pw);
					}
					else
					{
						JTextField t=new JTextField();
						t.setText(n.getString("text()"));
						m_AttributeControls.add(t);		
					}					
					if(p.getProperty("focus")!=null && p.getProperty("focus").equalsIgnoreCase("true"))
					    focusedComponent=m_AttributeControls.get(m_AttributeControls.size()-1);
				}
				
				//If attributes in attrs are in the reverse order, add in the correct order
				if(m_AttributeLabels.size()>1 && m_AttributeLabels.get(0).getText().compareToIgnoreCase(m_AttributeLabels.get(m_AttributeLabels.size()-1).getText())<0)
				{
					for(int i=0;i<m_AttributeControls.size();i++)
					{					
						m_AttributePanel.add(m_AttributeLabels.get(i));
						m_AttributePanel.add(m_AttributeControls.get(i));
						if(focusedComponent==null)
						    focusedComponent=m_AttributeControls.get(i);
					}
				}
				else
				{
					for(int i=m_AttributeControls.size()-1;i>-1;i--)
					{					
						m_AttributePanel.add(m_AttributeLabels.get(i));
						m_AttributePanel.add(m_AttributeControls.get(i));
						if(focusedComponent==null)
						    focusedComponent=m_AttributeControls.get(i);
					}
				}
				
			}				
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		setAttributePanelSize();		
		m_AttributeParentPanel.validate();
		if(m_AttributeControls.size()>0 && m_bPreparedNode)
		{			
			focusedComponent.requestFocusInWindow();				 
	    	if(focusedComponent instanceof JTextField)
	    	    ((JTextField)focusedComponent).selectAll();
	    	else if(focusedComponent instanceof JPasswordFieldWithHashIndicator)
	    	    ((JPasswordFieldWithHashIndicator)focusedComponent).selectAll();
		}
		m_bPreparedNode=false;
	}
	
	private void prepareNode(NodeEx aNode)
	{	
		try
		{
			m_bPreparedNode=true;
			aNode.setAttribute("node-tostringexpression",null);
			aNode.setAttribute("node-minimum",null);
			aNode.setAttribute("node-maximum",null);
			aNode.setAttribute("node-uses-children-of",null);
			List<NodeEx> attrs=aNode.getNodes("./@*");
			for(NodeEx n : attrs)
			{
				Properties p=new Properties();
				p.load(new StringReader(n.getValue().replace(";","\n")));
				
				if(p.getProperty("default")!=null)
					n.setValue(p.getProperty("default"));
				else
					n.setValue("");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	void addChild(JTreeXMLNode jn)
	{
		Vector<String> optionVector=m_Tree.getChildLists(jn.getPathAsString());
		Object[] options=optionVector==null ? null : optionVector.toArray(); 
		String selectedChildName=null;
		if(options!=null && options.length>0)
		{
			if(options.length==1)
			{
				selectedChildName=options[0].toString();
			}
			else
			{
				selectedChildName = (String)JOptionPane.showInputDialog(this,
					"Select a child type:",
					"Child Type Select",
					JOptionPane.PLAIN_MESSAGE,
					UIManager.getIcon("OptionPane.informationIcon"),
					options,
					options[0]);
				
				if(selectedChildName==null)
					return;
			}
		}
		else return;
		
		NodeEx n=m_ValidationXml.getNode(jn.getPathAsString() + "/" + selectedChildName);		
		
		
		if(n!=null)
		{	
			NodeEx jnNodeEx=(NodeEx)jn.getUserObject();
			List<NodeEx> children=jnNodeEx.getNodes("./" + selectedChildName);
			
			int nMaxAmount=parseInt(n.getString("./@node-maximum"),0);
			if(nMaxAmount==0 || children==null || children.size()<nMaxAmount)
			{
				NodeEx newNode=n.cloneNodeForOwnerDocument(jnNodeEx);
				prepareNode(newNode);					
				JTreeXMLNode childNode=new JTreeXMLNode(newNode,jn.getPathAsString(),m_ValidationXml);
				jn.addChild(childNode,(DefaultTreeModel)m_Tree.getModel());
				m_Tree.setSelectionPath(new TreePath(childNode.getPath()));
			}
			else
			{
				JOptionPane.showMessageDialog(this,"Unable to add any more of this child type to the parent node.");
			}
		}
	}
	
	void addSibling(JTreeXMLNode jn)
	{
		Object parentObj=jn.getParent();				
				
		if(parentObj instanceof JTreeXMLNode)
		{
			JTreeXMLNode jnParent=(JTreeXMLNode)parentObj;
			
			Vector<String> optionVector=m_Tree.getChildLists(jnParent.getPathAsString());
			Object[] options=optionVector==null ? null : optionVector.toArray(); 
			String selectedSiblingName=null;
			if(options!=null && options.length>0)
			{
				if(options.length==1)
				{
					selectedSiblingName=options[0].toString();
				}
				else
				{
					selectedSiblingName = (String)JOptionPane.showInputDialog(this,
						"Select a sibling type:",
						"Sibling Type Select",
						JOptionPane.PLAIN_MESSAGE,
						UIManager.getIcon("OptionPane.informationIcon"),
						options,
						options[0]);
					if(selectedSiblingName==null)
						return;
				}
			}
			else return;
			
			NodeEx n=m_ValidationXml.getNode(jnParent.getPathAsString() + "/" + selectedSiblingName);		
			
			if(n!=null)
			{	
				NodeEx jnNodeEx=(NodeEx)jnParent.getUserObject();
				List<NodeEx> children=jnNodeEx.getNodes("./" + selectedSiblingName);
				
				int nMaxAmount=parseInt(n.getString("./@node-maximum"),0);
				if(nMaxAmount==0 || children==null || children.size()<nMaxAmount)
				{
					NodeEx newNode=n.cloneNodeForOwnerDocument((NodeEx)jnParent.getUserObject());
					prepareNode(newNode);						
					JTreeXMLNode siblingNode=new JTreeXMLNode(newNode,jnParent.getPathAsString(),m_ValidationXml);
					jnParent.insertChild(jnParent.getIndex(jn)+1, siblingNode,(DefaultTreeModel)m_Tree.getModel());
					m_Tree.setSelectionPath(new TreePath(siblingNode.getPath()));
				}
				else
				{
					JOptionPane.showMessageDialog(this,"Unable to add any more of this child type to the parent node.");
				}

			}
			
		}
	}
	
	void removeNode(JTreeXMLNode jn)
	{
		Object parentObj=jn.getParent();
		if(parentObj!=null && parentObj instanceof JTreeXMLNode)
		{
			JTreeXMLNode jnParent=(JTreeXMLNode)parentObj;
			NodeEx jnNodeEx=(NodeEx)jn.getUserObject();
			NodeEx jnParentNodeEx=(NodeEx)jnParent.getUserObject();
			NodeEx n=m_ValidationXml.getNode(jnParent.getPathAsString() + "/" + jnNodeEx.getString("fn:name()"));
			List<NodeEx> children=jnParentNodeEx.getNodes("./" + jnNodeEx.getString("fn:name()"));
			
			int nMinAmount=parseInt(n.getString("./@node-minimum"),0);
			if(nMinAmount==0 || (children!=null && children.size()>nMinAmount))
			{
				jnParent.removeChild(jn,(DefaultTreeModel)m_Tree.getModel());					
			}
			else
			{
				JOptionPane.showMessageDialog(this,"Unable to remove any more of this child type from the parent node.");
			}
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object obj=getSelectedNode();
		if(obj instanceof JTreeXMLNode)
		{
			if(e.getActionCommand()=="Add Child...")
			{
				addChild((JTreeXMLNode)obj);
			}
			else if(e.getActionCommand()=="Add Sibling...")
			{
				addSibling((JTreeXMLNode)obj);
			}
			else if(e.getActionCommand()=="Remove Item")
			{
				removeNode((JTreeXMLNode)obj);
			}
		}
	}	
	
	public void keyPressed(KeyEvent e)
	{
		Object obj=getSelectedNode();
		if( !(obj instanceof JTreeXMLNode))
		    return;
		if(KeyEvent.getKeyModifiersText(e.getModifiers()).toLowerCase().equals("ctrl"))
		{
			if(e.getKeyCode()==KeyEvent.VK_C)
			{
		        addChild((JTreeXMLNode)obj);
			}
			else if(e.getKeyCode()==KeyEvent.VK_S)
			{
		        addSibling((JTreeXMLNode)obj);
			}
		}
		else if(e.getKeyCode()==KeyEvent.VK_DELETE)
		{
			removeNode((JTreeXMLNode)obj);
		}
	}
	
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){}
	 
	
	
	public void saveTreeToXMLFile(String sFileName)
	{
		if(m_Tree.getModel().getRoot()!=null)
		{	
			saveAttributeChanges();
		    ((NodeEx)((JTreeXMLNode)m_Tree.getModel().getRoot()).getUserObject()).save(sFileName);
		}
	}  
	
	public void saveTreeToOutputStream(OutputStream ostream)
	{
		if(m_Tree.getModel().getRoot()!=null)
		{	
			saveAttributeChanges();
		    ((NodeEx)((JTreeXMLNode)m_Tree.getModel().getRoot()).getUserObject()).save(ostream);
		}
	} 
}
