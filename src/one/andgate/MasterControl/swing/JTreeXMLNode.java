package one.andgate.MasterControl.swing;

import one.andgate.MasterControl.xml.NodeEx;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;

public class JTreeXMLNode extends DefaultMutableTreeNode
{
	String m_sParentPath, m_sPath;
	
	NodeEx m_ValidationXml;
	
	public JTreeXMLNode(NodeEx aNode)
	{
		this(aNode, "",null);
	}
	
	public JTreeXMLNode(NodeEx aNode, NodeEx validationXml)
	{
		this(aNode, "",validationXml);
	}
			
	public JTreeXMLNode(NodeEx aNode, String parentPath, NodeEx validationXml)
	{
		super();		
		m_ValidationXml=validationXml;
		m_sParentPath=parentPath;
        NodeEx userObj=aNode;
        if(userObj==null) return;
		if(userObj.getString("fn:name()")==null || userObj.getString("fn:name()").equals(""))
		{
		    userObj=aNode.getNode("/*");				 
		}
		if(userObj==null) return;			
		setUserObject(userObj);				
		m_sPath=m_sParentPath + "/" + userObj.getString("fn:name()");
		
		
		if(m_ValidationXml!=null)
		{
			String strToExpr=m_ValidationXml.getString(m_sPath + "/@node-tostringexpression");
			userObj.setToStringFromExpression(strToExpr);
		}
		
		List<NodeEx> childlist=userObj.getNodes("./*");				
		
		for(NodeEx n : childlist)
		{
			add(new JTreeXMLNode(n,m_sPath,m_ValidationXml));
		}		
	}
		
	public String getPathAsString(){ return m_sPath; }
	public String getParentPathAsString(){ return m_sParentPath; }
	
	public void addChild(JTreeXMLNode childNode, DefaultTreeModel model)
	{
		if(getUserObject() instanceof NodeEx && childNode.getUserObject() instanceof NodeEx)
		{
			((NodeEx)getUserObject()).addChild( (NodeEx)childNode.getUserObject() );
			model.insertNodeInto(childNode, this, getChildCount());
		}
	}
	
	public void insertChild(int nIndex, JTreeXMLNode childNode, DefaultTreeModel model)
	{
		if(getUserObject() instanceof NodeEx && childNode.getUserObject() instanceof NodeEx)
		{
			NodeEx thisnode=(NodeEx)getUserObject();
			if(nIndex<0 || nIndex>getChildCount())
			    return;
			if(nIndex<getChildCount())
			{
				thisnode.insertChild( nIndex, (NodeEx)childNode.getUserObject() );
				model.insertNodeInto(childNode, this, nIndex);
			}
			else
			{
				addChild(childNode,model);
			}
		}
	}
	
	public void removeChild(JTreeXMLNode childNode, DefaultTreeModel model)
	{
		if(getUserObject() instanceof NodeEx && childNode.getUserObject() instanceof NodeEx)
		{
			((NodeEx)getUserObject()).removeChild( (NodeEx)childNode.getUserObject() );
			model.removeNodeFromParent(childNode);
		}
	}
}
