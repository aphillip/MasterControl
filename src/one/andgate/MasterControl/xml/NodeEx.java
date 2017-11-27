package one.andgate.MasterControl.xml;

import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class NodeEx
{
	Node m_Node=null;
	
	String m_sStringRepresentation, m_sToStringExpression;
	
	public NodeEx(InputStream stream)
	{
		m_Node=getDocumentFromStream(stream);
		setToStringFromExpression("fn:name()");
	}
	
	public NodeEx(String fileName)
	{
		m_Node=getDocumentFromFile(fileName);
		setToStringFromExpression("fn:name()");
	}
	
	public NodeEx(Node aNode)
	{
		m_Node=aNode;
		setToStringFromExpression("fn:name()");
	}
	
	private static Document getDocumentFromFile(String fileName)
    {
        try
        {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( fileName );
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    private static Document getDocumentFromString(String docStr)
    {
		try
		{
		    return getDocumentFromStream(new ByteArrayInputStream(docStr.getBytes("UTF-8")));        
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
    }
    
    private static Document getDocumentFromStream(InputStream is)
    {
		Document doc=null;
        try
        {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( is );
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return doc;
	}
    
    public static NodeEx createFromXMLString(String xmlStr)
    {
		return new NodeEx(getDocumentFromString(xmlStr));
	}
    
    
    public List<NodeEx> getNodes(String strXPath)
    {
		if(m_Node==null) return null;
        try
        {
            XPath xpath = new DOMXPath(strXPath);
            List nodes = xpath.selectNodes(m_Node);
            List<NodeEx> retVal=new ArrayList<NodeEx>();
            for(Object o : nodes)
            {
				//All objects in nodes should be Node objects
				retVal.add(new NodeEx((Node)o));
			}
            return retVal; 
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }		
        return null;
	}
	
	public String getValue()
	{		
		return m_Node.getNodeValue();
	}
	
	public void setValue(String sValue)
	{		
		m_Node.setNodeValue(sValue);
	}
	
	public NodeEx getNode(String strXPath)
    {
		if(m_Node==null) return null;
        try
        {
            XPath xpath = new DOMXPath(strXPath);
            Object obj = xpath.selectSingleNode(m_Node); 
            if(obj!=null && obj instanceof Node)
				return new NodeEx((Node)obj);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
	}
	
	public String getString(String strXPath)
	{
		return getString(strXPath, "");
	}

	public String getString(String strXPath, String defaultValue)
    {
        if(m_Node==null) return defaultValue;
        try
        {
            XPath xpath = new DOMXPath(strXPath);
            Object obj = xpath.selectSingleNode(m_Node); 
            if(obj!=null)     
            {
				String retVal;
				if(obj instanceof Node)
    				retVal=((Node)obj).getNodeValue();
				else if(obj instanceof String)
				    retVal=(String)obj;
				else
				    retVal=obj.toString();

				if(retVal.equals(""))
					retVal=defaultValue;
				return retVal;
			}
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public String getToStringFromExpression()
    {
		return m_sToStringExpression;
	}
	
	public void refreshToStringExpression()
	{
		m_sStringRepresentation=getString(m_sToStringExpression);
	}
	
    public void setToStringFromExpression(String sStrExpr)
    {
		m_sToStringExpression=sStrExpr;
		if(m_sToStringExpression==null || m_sToStringExpression.length()==0)
		    m_sToStringExpression="fn:name()";
		m_sStringRepresentation=getString(m_sToStringExpression);
	}
    
    @Override public String toString()
    {
		return m_sStringRepresentation;
	}
	
	public void save(String sFilename)
	{
		try
		{
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.transform(new DOMSource(m_Node), new StreamResult(new FileOutputStream(sFilename) ));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void save(OutputStream ostream)
	{
		try
		{
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.transform(new DOMSource(m_Node), new StreamResult(ostream));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String getAttributeValue(String sAttributeName, String sDefault)
    {
        if(m_Node.getAttributes().getNamedItem(sAttributeName)!=null)
            return m_Node.getAttributes().getNamedItem(sAttributeName).getNodeValue();
        return sDefault;
    }

    public String getAttributeValue(String sAttributeName)
    {
        return getAttributeValue(sAttributeName, null);
    }
    
    public void setAttribute(String sAttributeName, String newValue)
    {
        if(m_Node.getAttributes().getNamedItem(sAttributeName)!=null)
        {            
			if(newValue!=null)
                m_Node.getAttributes().getNamedItem(sAttributeName).setNodeValue(newValue);
            else
                m_Node.getAttributes().removeNamedItem(sAttributeName);
        }
        else
        {
			if(newValue==null)
   			 return;
            Attr att=m_Node.getOwnerDocument().createAttribute(sAttributeName);
            att.setValue(newValue);
            m_Node.getAttributes().setNamedItem(att);
        }
    }

	public NodeEx cloneNodeForOwnerDocument(NodeEx aNode)
	{
		return cloneNodeForOwnerDocument(aNode, false);
	}
	
	public NodeEx cloneNodeForOwnerDocument(NodeEx aNode, boolean bDeep)
	{
		return new NodeEx(aNode.m_Node.getOwnerDocument().importNode(m_Node,bDeep));
	}
    
    public void addChild(NodeEx aNode)
    {
		m_Node.appendChild(aNode.m_Node);		
	}
	
	public void insertChild(int nIndex, NodeEx aNode)
    {
		if(0>nIndex || nIndex>m_Node.getChildNodes().getLength())
		    return;
		if(nIndex<m_Node.getChildNodes().getLength())
		{					
			int adjustedIndex=-1;
			Node nodeBefore=null;
			int nLen=m_Node.getChildNodes().getLength();
			for(int i=0;i<nLen;i++)
			{
				nodeBefore=m_Node.getChildNodes().item(i);
				if(nodeBefore.getNodeType()==Node.ELEMENT_NODE)
				{
					adjustedIndex++;
					if(adjustedIndex>=nIndex)
					    break;
				}
			}
			m_Node.insertBefore(aNode.m_Node,nodeBefore);
		}
		else
		{
			addChild(aNode);
		}
	}
	
	public void removeChild(NodeEx aNode)
    {
		m_Node.removeChild(aNode.m_Node);
	}
	
}
