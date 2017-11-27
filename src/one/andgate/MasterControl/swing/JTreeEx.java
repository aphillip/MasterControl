package one.andgate.MasterControl.swing;

import one.andgate.MasterControl.xml.NodeEx;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.DropMode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import javax.swing.tree.TreePath;
import java.awt.Cursor;
import java.awt.dnd.DragSource;




public class JTreeEx extends JTree implements MouseListener
{
	JPopupMenu m_Popup;
	JMenuItem m_AddChildMenuItem, m_AddSiblingMenuItem, m_RemoveItemMenuItem;
	HashMap<String,Vector<String>> m_ChildLists=new HashMap<String,Vector<String>>();
	NodeEx m_ValidationXml;
	
	public JTreeEx(NodeEx aNode, NodeEx validationXml, ActionListener al)
	{		
		m_ValidationXml=validationXml;
		JTreeXMLNode root=new JTreeXMLNode(aNode,validationXml);
		setModel(new DefaultTreeModel(root));
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);		
		setShowsRootHandles(true);
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {							
				TreePath path = getClosestPathForLocation(e.getX(), e.getY());
				if (getSelectionPath().getParentPath().toString().equals(path.getParentPath().toString())) 
				{
					setCursor(DragSource.DefaultMoveDrop);
				} 
				else 
				{
					setCursor(DragSource.DefaultMoveNoDrop);
				}
			}
			
			public void mouseMoved(MouseEvent e) { setCursor(Cursor.getDefaultCursor()); }
		});

		
		m_Popup=new JPopupMenu();
		JMenuItem  mi=new JMenuItem("Add Child...");
		mi.addActionListener(al);
		m_AddChildMenuItem=mi;
		m_Popup.add(mi);
		mi=new JMenuItem("Add Sibling...");
		mi.addActionListener(al);
		m_AddSiblingMenuItem=mi;
		m_Popup.add(mi);
		m_Popup.addSeparator();
		mi=new JMenuItem("Remove Item");
		mi.addActionListener(al);
		m_RemoveItemMenuItem=mi;
		m_Popup.add(mi);		
		
		addMouseListener(this);
	}
	
	public void mouseClicked(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e)
	{
	}
	
	public void mouseReleased(MouseEvent e) 
	{
		if(getCursor()==DragSource.DefaultMoveDrop && e.getButton()==MouseEvent.BUTTON1)
		{
			TreePath path=getClosestPathForLocation(e.getX(), e.getY());
			TreePath selectedPath=getSelectionPath();			
			if(selectedPath!=path && selectedPath.getParentPath().toString().equals(path.getParentPath().toString()) &&
			   path.getLastPathComponent() instanceof JTreeXMLNode &&
			   selectedPath.getLastPathComponent() instanceof JTreeXMLNode)
			{
				JTreeXMLNode destPath=(JTreeXMLNode)path.getLastPathComponent();
				JTreeXMLNode srcPath=(JTreeXMLNode)selectedPath.getLastPathComponent();
				
				if(srcPath.getParent()!=null && srcPath.getParent() instanceof JTreeXMLNode)
				{					
					JTreeXMLNode srcParent=(JTreeXMLNode)srcPath.getParent();
					int nIndexAdjust=0;
					if(srcParent.getIndex(srcPath)<srcParent.getIndex(destPath))
					    nIndexAdjust=1;
					srcParent.removeChild(srcPath,(DefaultTreeModel)getModel());
					srcParent.insertChild(srcParent.getIndex(destPath)+nIndexAdjust, srcPath,(DefaultTreeModel)getModel());
					setSelectionPath(new TreePath(srcPath.getPath()));					
				}
			}
		}
		else if(e.getClickCount() == 1 && e.getButton()==MouseEvent.BUTTON3)
		{
			int row = getClosestRowForLocation(e.getX(), e.getY());  
			setSelectionRow(row);
			if(getSelectionPath()!=null)
			{
				Object obj=getSelectionPath().getLastPathComponent();
				if(obj instanceof JTreeXMLNode)
				{
					if(getChildLists( ((JTreeXMLNode)obj).getPathAsString() )==null)
					    m_AddChildMenuItem.setEnabled(false);
					else
					    m_AddChildMenuItem.setEnabled(true);
					
					if(((JTreeXMLNode)obj).getParent()==null)
					{
						m_AddSiblingMenuItem.setEnabled(false);
					    m_RemoveItemMenuItem.setEnabled(false);
					}
					else
					{
						m_AddSiblingMenuItem.setEnabled(true);
					    m_RemoveItemMenuItem.setEnabled(true);
					}
				}
			}
		    m_Popup.show(this, e.getX(), e.getY());
		}
		setCursor(Cursor.getDefaultCursor()); 
	}
	
	public Vector<String> getChildLists(String nodePath)
	{
		Vector<String> retVal=m_ChildLists.get(nodePath);
		if(retVal==null)
		{
			List<NodeEx> childlist=m_ValidationXml.getNodes(nodePath + "/*");
			if(childlist!=null && childlist.size()>0)
			{
				retVal=new Vector<String>();
				
				for(NodeEx n : childlist)
				{			
					retVal.add(n.getString("fn:name()"));
				}
				m_ChildLists.put(nodePath,retVal);
			}			
		}
		return retVal;
	}
		
}
