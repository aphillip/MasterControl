package one.andgate.MasterControl.swing;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import one.andgate.MasterControl.xml.NodeEx;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.Inflater;
import java.util.zip.Deflater;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.awt.Robot;
import java.util.regex.Pattern;
import java.awt.MouseInfo;
import java.awt.Font;
import java.awt.Color;



public class MasterControl extends JFrame implements ActionListener, WindowListener, KeyListener
{
	HashMap<String,NodeEx> m_ServerNodes=new HashMap<String,NodeEx>();
	HashMap<String,Socket> m_ServerSocketConnections=new HashMap<String,Socket>();
	HashMap<String,NodeEx> m_ButtonNodes=new HashMap<String,NodeEx>();
	HashMap<String,ArrayList<Process> > m_ProcessGroups=new HashMap<String,ArrayList<Process> >();
	JPanel m_MainPanel=null;
	boolean m_bFullscreen;
	Dimension m_ScreenDims, m_FrameDims;
	JDialog m_ConfigFrame=null;
	XMLEditorPanel m_ConfigPanel=null;
	String m_sConfigFile;
	boolean m_bContinueProcessButton=false;
	Timer mouseMonitor=null;

	public MasterControl(String sConfigFile)
	{
		super();
		m_sConfigFile=sConfigFile;
		m_ScreenDims=Toolkit.getDefaultToolkit().getScreenSize();
		setTitle("Master Control");
		addWindowListener(this);
		addKeyListener(this);	
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		//Menu definition
		JMenuBar mb=new JMenuBar();

		JMenu menu=new JMenu("File");
		
		JMenuItem mi=new JMenuItem("New Configuration");
		mi.addActionListener(this);
		menu.add(mi);		 	
		
		mi=new JMenuItem("Load Configuration...");
		mi.addActionListener(this);
		menu.add(mi);
		
		mi=new JMenuItem("Save Configuration As...");
		mi.addActionListener(this);
		menu.add(mi);
		
		menu.addSeparator();
		
		mi=new JMenuItem("Modify Configuration");
		mi.addActionListener(this);
		menu.add(mi);
		
		menu.addSeparator();
		
		mi=new JMenuItem("Exit");
		mi.addActionListener(this);
		menu.add(mi);
		mb.add(menu);
		setJMenuBar(mb);

		m_FrameDims=new Dimension(640,480);

		loadConfiguration();
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
		catch(Exception e){}
		return defaultInt;
	}


	private static byte[] compressBytes(byte[] myByte)
    {
        // Compression level of best compression
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);

        // Give the Compressor the input data to compress
        compressor.setInput(myByte);
        compressor.finish();

        // Create an expandable byte array to hold the compressed data.
        // It is not necessary that the compressed data will be smaller than the
        // uncompressed data.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(myByte.length);

        // Compress the data
        byte[] buf = new byte[1024];
        while (!compressor.finished())
        {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }

        try
        {
            bos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Get the compressed data
        return bos.toByteArray();
    }

    private static byte[] decompressBytes(byte[] compressedMyByte)
    {

        Inflater decompressor = new Inflater();
        byte[] buf = new byte[1024];
        decompressor.setInput(compressedMyByte);

        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedMyByte.length);

        // Decompress the data
        buf = new byte[1024];
        while (!decompressor.finished())
        {
            try
            {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                try
                {
                    bos.close();
                }
                catch (Exception ex){ ex.printStackTrace(); }
                return null;
            }
        }

        try
        {
            bos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Get the decompressed data
        return bos.toByteArray();
    }

    public static byte[] readFileAsBytes(String filePath)
    {
        try
        {
            FileInputStream inFile=new FileInputStream(filePath);
            ByteArrayOutputStream fileData = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int numRead=0;
            while((numRead=inFile.read(buf)) != -1)
            {
                fileData.write(buf,0,numRead);
            }
            inFile.close();
            return fileData.toByteArray();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    NodeEx getNewFileConfiguration()
    {		
		return new NodeEx(getClass().getResourceAsStream("settings.xml"));
	}

	NodeEx getConfiguration()
	{
		try
		{
			if(m_sConfigFile!=null && new File(m_sConfigFile).exists())
			{
				byte[] configData=readFileAsBytes(m_sConfigFile);
				byte[] decompressedData=decompressBytes(configData);
				return new NodeEx(new ByteArrayInputStream(decompressedData));
			}
			else
			{
				m_sConfigFile=null;
				return getNewFileConfiguration();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	void setFullscreen(boolean bFullscreen)
	{
		m_bFullscreen=bFullscreen;
		if(m_bFullscreen)
		{
			setUndecorated(true);
			setBounds(0,0,m_ScreenDims.width,m_ScreenDims.height);       
		}
		else
		{
			setUndecorated(false);
			setSize(m_FrameDims.width,m_FrameDims.height);
			setLocationRelativeTo(null);
		}

	}
	
	Color getColor(String sColor)
	{
		if(sColor==null) return null;
		if(sColor.equalsIgnoreCase("red")) return Color.RED;
		if(sColor.equalsIgnoreCase("green")) return Color.GREEN;
		if(sColor.equalsIgnoreCase("blue")) return Color.BLUE;
		if(sColor.equalsIgnoreCase("yellow")) return Color.YELLOW;
		if(sColor.equalsIgnoreCase("orange")) return new Color(255,79,0);
		if(sColor.equalsIgnoreCase("lightorange")) return Color.ORANGE;
		if(sColor.equalsIgnoreCase("gray")) return Color.GRAY;
		if(sColor.equalsIgnoreCase("darkgray")) return Color.DARK_GRAY;
		if(sColor.equalsIgnoreCase("lightgray")) return Color.LIGHT_GRAY;
		if(sColor.equalsIgnoreCase("pink")) return Color.PINK;
		if(sColor.equalsIgnoreCase("magenta")) return Color.MAGENTA;
		if(sColor.equalsIgnoreCase("cyan")) return Color.CYAN;
		if(sColor.equalsIgnoreCase("black")) return Color.BLACK;
		if(sColor.equalsIgnoreCase("white")) return Color.WHITE;
		return null;
	}
	

	void loadConfiguration()
	{
		getContentPane().removeAll();

		 NodeEx config=getConfiguration();
		 
		 //Set Mouse Restriction
		 if(config.getString("/config/interface/@restrictmouse").equalsIgnoreCase("True"))
		 {			 
			 ActionListener taskPerformer = new ActionListener() {
				public void actionPerformed(ActionEvent evt)
				{					    
					
					int minX=getLocation().x, minY=getLocation().y;
					int maxX=minX+(int)getSize().getWidth(), maxY=minY+(int)getSize().getHeight();
					int mouseX=MouseInfo.getPointerInfo().getLocation().x, mouseY=MouseInfo.getPointerInfo().getLocation().y;
					boolean bAlteredMousePos=false;
								
					if(mouseX>maxX) { mouseX=maxX; bAlteredMousePos=true; }
					if(mouseX<minX) { mouseX=minX; bAlteredMousePos=true; }
					if(mouseY>maxY) { mouseY=maxY; bAlteredMousePos=true; }
					if(mouseY<minY) { mouseY=minY; bAlteredMousePos=true; }
					if(bAlteredMousePos)
					{
						try
						{
					        new Robot().mouseMove(mouseX,mouseY);
						}
						catch(Exception erx){ erx.printStackTrace(); }
					}
				}
			};
			new Timer(100, taskPerformer).start();
		}

		 //Set Always On Top
		 if(config.getString("/config/interface/@alwaysontop").equalsIgnoreCase("True"))
		     setAlwaysOnTop(true);
		 else
		     setAlwaysOnTop(false);

		 //Get Servers
		 List<NodeEx> tmpNodeList=config.getNodes("/config/servers/server");

		 for(NodeEx n: tmpNodeList)
		 {
			 m_ServerNodes.put(n.getString("./@name"),n);
		 }

		 //Construct Interface
		 setTitle(config.getString("/config/interface/@title"));
		 m_FrameDims.width=parseInt(config.getString("/config/interface/@width"),640);
		 m_FrameDims.height=parseInt(config.getString("/config/interface/@height"),480);

         if(!isShowing())
		     setFullscreen(config.getString("/config/interface/@fullscreen").equalsIgnoreCase("true"));

		 m_MainPanel=new JPanel();
		 m_MainPanel.setLayout(new BoxLayout(m_MainPanel, BoxLayout.Y_AXIS));
		 m_MainPanel.addKeyListener(this);

		 tmpNodeList=config.getNodes("/config/interface/section");
		 m_MainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
		 Font fnt=new Font("Dialog",Font.BOLD,parseInt(config.getString("/config/interface/@fontsize"),12));
		 for(NodeEx sectionNode: tmpNodeList)
		 {
			 JPanel sectionPanel=new JPanel();
			 sectionPanel.setFont(fnt);
			 sectionPanel.addKeyListener(this);
			 TitledBorder tb=BorderFactory.createTitledBorder(sectionNode.getString("./@name"));
			 tb.setTitleFont(fnt);
			 sectionPanel.setBorder(tb);
			 sectionPanel.setLayout(new BorderLayout());

			 List<NodeEx> buttonList=sectionNode.getNodes("./button");
			 JPanel buttonPanel=new JPanel();
			 buttonPanel.addKeyListener(this);
			 buttonPanel.setLayout(new GridBagLayout());
			 GridBagConstraints gbc=new GridBagConstraints();
			 gbc.weightx=0.5;
			 gbc.weighty=0.5;
			 gbc.gridx = 0;
	         gbc.gridy = 0;
	         gbc.fill=GridBagConstraints.BOTH;	         
			 for(NodeEx buttonNode : buttonList)
			 {
				 JButtonWithNodeEx button=new JButtonWithNodeEx(buttonNode.getString("./@label"),buttonNode);
				 if(buttonNode.getString("./@tooltip")!=null && buttonNode.getString("./@tooltip").length()>0)
				     button.setToolTipText(buttonNode.getString("./@tooltip"));
				 button.addActionListener(this);
				 button.addKeyListener(this);
				 button.setFont(fnt);
				 Color c=getColor(buttonNode.getString("./@color"));
				 if(c!=null)
				     button.setBackground(c);				 
				 buttonPanel.add(button,gbc);
				 gbc.gridx++;
			 }
			 sectionPanel.add(buttonPanel);
			 m_MainPanel.add(sectionPanel);
			 m_MainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
		 }
		 
		if(m_ConfigFrame==null)
		{
			NodeEx validationXml=new NodeEx(getClass().getResourceAsStream("validation.xml"));
			//Update all nodes that have a "node-uses-children-of" attribute
			List<NodeEx> ucNodes=validationXml.getNodes("//*[@node-uses-children-of!='']");
			
			for(NodeEx uc : ucNodes)
			{				
				String ucPath=uc.getString("./@node-uses-children-of");
				if(ucPath!=null && ucPath.length()>0)
				{
					NodeEx refNode=validationXml.getNode(ucPath);
					if(refNode!=null)
					{
						List<NodeEx> refNodeKids=refNode.getNodes("./*");
						if(refNodeKids!=null)
						{
						    for(NodeEx srcNode : refNodeKids)
						    {
								uc.addChild(srcNode.cloneNodeForOwnerDocument(uc,true));
							}
						}
					}
				}
			}
			
			m_ConfigFrame=new JDialog(this,"Configuration",true);
			m_ConfigPanel=new XMLEditorPanel(config ,validationXml);
			m_ConfigFrame.getContentPane().add(m_ConfigPanel,BorderLayout.CENTER);
			m_ConfigFrame.setSize(640,480);
			m_ConfigFrame.addWindowListener(this);
		}
			
		 getContentPane().add(Box.createRigidArea(new Dimension(15, 0)),BorderLayout.WEST);
		 getContentPane().add(m_MainPanel,BorderLayout.CENTER);
		 getContentPane().add(Box.createRigidArea(new Dimension(15, 0)),BorderLayout.EAST);
		 validate();
	}
	
	JFileChooser getFileChooser()
    {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.resetChoosableFileFilters();
        return fileChooser;
    }
    
    public boolean login()
    {
		NodeEx config=getConfiguration();
		int passHash=parseInt(config.getString("/config/@password"));
		if(passHash!=0)
		{
			JLabel passwordLabel=new JLabel("Please enter the Configuration password:");
			final JPasswordField password = new JPasswordField();
			Object[] ob = { passwordLabel, password};

			//Some shenanigans to get the password field
			//to have focus when the JOptionPane for
			//entering the password is displayed.
			ActionListener taskPerformer = new ActionListener() {
				public void actionPerformed(ActionEvent evt)
				{
					password.requestFocusInWindow();
					((Timer)evt.getSource()).stop();
				}
			};
			new Timer(250, taskPerformer).start();
			//Shenanigans over! It is possible for it to fail
			//if it takes longer the 250ms to show the JOptionPane

			int result = JOptionPane.showConfirmDialog(this, ob, "Configuration Access", JOptionPane.OK_CANCEL_OPTION);
			if(result==JOptionPane.OK_OPTION)
			{
				String sPassValue=new String( password.getPassword() );
				int hashedValue=sPassValue.hashCode();
				if(hashedValue!=passHash)
				{
					JOptionPane.showMessageDialog(this,"Invalid password.  Settings access denied.");
					return false;
				}
			}
			else
				return false;
		}
		return true;
	}


    public void actionPerformed(ActionEvent e)
    {
		if(e.getActionCommand()=="Exit")
		{
             if(atExit())
                 System.exit(0);
		}
		else if(e.getActionCommand()=="New Configuration")
		{
			if(!login()) return;
			m_sConfigFile=null;
			m_ConfigFrame=null;
			loadConfiguration();
		}
		else if(e.getActionCommand()=="Load Configuration...")
		{
			if(!login()) return;
			JFileChooser fc=new JFileChooser(System.getProperty("user.dir"));
			fc.setDialogTitle("Load Configuration");
			if(fc.showOpenDialog(this)==fc.APPROVE_OPTION)
			{
				m_sConfigFile=fc.getSelectedFile().getPath();
				m_ConfigFrame=null;
				loadConfiguration();
			}
		}
		else if(e.getActionCommand()=="Save Configuration As...")
		{
			if(!login()) return;
			JFileChooser fc=new JFileChooser(System.getProperty("user.dir"));
			fc.setDialogTitle("Save Configuration");
			if(fc.showSaveDialog(this)==fc.APPROVE_OPTION)
			{
				m_sConfigFile=fc.getSelectedFile().getPath();
				saveConfiguration(m_sConfigFile);
				
			}
		}
		else if(e.getActionCommand()=="Modify Configuration")
		{
			if(!login()) return;
			m_ConfigFrame.setLocationRelativeTo(null);
		    m_ConfigFrame.setVisible(true);
		}
		else if(e.getSource() instanceof JButtonWithNodeEx)
		{
			processButtonNodeEx( ((JButtonWithNodeEx)e.getSource()).getNodeEx() );
		}
	}


	boolean processButtonNodeEx(NodeEx aNode)
	{
		boolean bCompleted=true;
		if(aNode==null)
		    return true;
		List<NodeEx> commands=aNode.getNodes("./*");
		for(NodeEx n : commands)
		{
			String nodeName=n.getString("fn:name()");

			if(nodeName.equalsIgnoreCase("tcpsend"))
			{       
				String strTargets=n.getString("./@target");
				String[] targets=strTargets.split(",");
				if(targets.length==1 && targets[0].equalsIgnoreCase("all"))
				{
					Object[] keys=m_ServerNodes.keySet().toArray();
					targets=new String[keys.length];
					for(int i=0;i<keys.length;i++)
					{
						targets[i]=keys[i].toString();
					}
				}

				for(String s : targets)
				{
					NodeEx serverNode=m_ServerNodes.get(s);

		            if(!m_ServerSocketConnections.containsKey(s))
		            {
						try
						{							
							Socket skt=new Socket();
							skt.connect(new InetSocketAddress(serverNode.getString("./@ip"), parseInt(serverNode.getString("./@port"),4518)), parseInt(serverNode.getString("./@timeout"),100));
		                    m_ServerSocketConnections.put(s,skt);
						}
						catch(Exception e2)
						{
							e2.printStackTrace();
							JOptionPane.showMessageDialog(this,"TCPSEND Unable to connect to server \"" + serverNode.getString("./@name") + "\".  Aborting button execution.");
							closeConnections();
							//don't block File->Exit, on exception.
							return true;
						}
		            }
		            /*
		            else
		            {
						System.out.println("Already connected.");
					}
					*/
		            if(!m_ServerSocketConnections.containsKey(s))
		                continue;


		            processTCPSendCommand(m_ServerSocketConnections.get(s),
										  n.getString("./@value"),
	   									  n.getString("./@valueformat","Text"),
		                                  serverNode.getString("./@messageterminator"),
		                                  serverNode.getString("./@responsesize","0"),
		                                  n.getString("./@readresponse"));
				}


			}
			else if(nodeName.equalsIgnoreCase("wait"))
			{
				int waitTime=parseInt(n.getString("./@time"),1000);
				wait(waitTime);
			}
			else if(nodeName.equalsIgnoreCase("confirminput"))
			{
				String sInputStr=JOptionPane.showInputDialog(this,n.getString("./@prompt"),n.getString("./@default"));				
				if(sInputStr==null || !sInputStr.equals(n.getString("./@matchvalue")))
				{
					bCompleted=false;
				    break;
				}
			}
			else if(nodeName.equalsIgnoreCase("confirm"))
			{
				if(JOptionPane.showConfirmDialog(this,n.getString("./@prompt"),n.getString("./@title"),JOptionPane.YES_NO_OPTION)==JOptionPane.NO_OPTION)
				{
					bCompleted=false;
					break;
				}
			}
			else if(nodeName.equalsIgnoreCase("exec"))
			{
				try
				{
					if(n.getString("./@command")!=null && n.getString("./@command").length()>0)
					{

						Process p=Runtime.getRuntime().exec(n.getString("./@command"));
						if(n.getString("./@wait").equalsIgnoreCase("true"))
						{
							setEnabled(false);
							p.waitFor();
							setEnabled(true);
						}
						else
						{
							ArrayList<Process> ls=m_ProcessGroups.get(n.getString("./@groupname"));
							if(ls==null)
							{
								ls=new ArrayList<Process>();
								m_ProcessGroups.put(n.getString("./@groupname"),ls);
							}
							ls.add(p);
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					JOptionPane.showMessageDialog(this,"Unabled to execute desired program.  Aborting button execution.");
					closeConnections();
					//don't block File->Exit, on exception.
					return true;
				}
			}
			else if(nodeName.equalsIgnoreCase("kill"))
			{
				if(n.getString("./@groupname")!=null && n.getString("./@groupname").length()>0)
				{
					if(n.getString("./@groupname").equalsIgnoreCase("all"))
					{
						for(ArrayList<Process> ls : m_ProcessGroups.values())
						{
							for(Process p : ls)
							{
								p.destroy();
							}
							ls.clear();
						}
					}
					else
					{
						ArrayList<Process> ls=m_ProcessGroups.get(n.getString("./@groupname"));
						if(ls!=null)
						{
							for(Process p : ls)
							{
								p.destroy();
							}
							ls.clear();
						}
					}
				}
				
			}
			else if(nodeName.equalsIgnoreCase("sendkey"))
			{
				String sKeyCombo=n.getString("./@keylist");
				if(sKeyCombo!=null && sKeyCombo.length()>0)
				{
				    String[] keys=sKeyCombo.toLowerCase().replace(" ", "").split(Pattern.quote("+"));
				    try
				    {
						Robot robot= new Robot();
						for(String s : keys)
						{
							int nKey=getKeyCode(s.trim());
							if(nKey>0) robot.keyPress(nKey);
						}
						
						int minWait=parseInt(n.getString("./@presstime"),200);
						minWait=minWait<200 ? 200 : minWait;
						wait(minWait);
						
						for(String s : keys)
						{
							int nKey=getKeyCode(s);
							if(nKey>0) robot.keyRelease(nKey);
						}
					}
					catch(Exception er)
					{
						er.printStackTrace();
					}
				}
			}
			/*
			else if(nodeName.equalsIgnoreCase("httprequest"))
			{
			}
			*/
		}

		//Disconnect Sockets
		closeConnections();
		return bCompleted;
	}
	
	int getKeyCode(String sChar)
	{
		if(sChar==null)
		    return 0;
		if(sChar.equalsIgnoreCase("a")) return KeyEvent.VK_A;
        if(sChar.equalsIgnoreCase("b")) return KeyEvent.VK_B;
        if(sChar.equalsIgnoreCase("c")) return KeyEvent.VK_C;
        if(sChar.equalsIgnoreCase("d")) return KeyEvent.VK_D;
        if(sChar.equalsIgnoreCase("e")) return KeyEvent.VK_E;
        if(sChar.equalsIgnoreCase("f")) return KeyEvent.VK_F;
        if(sChar.equalsIgnoreCase("g")) return KeyEvent.VK_G;
        if(sChar.equalsIgnoreCase("h")) return KeyEvent.VK_H;
        if(sChar.equalsIgnoreCase("i")) return KeyEvent.VK_I;
        if(sChar.equalsIgnoreCase("j")) return KeyEvent.VK_J;
        if(sChar.equalsIgnoreCase("k")) return KeyEvent.VK_K;
        if(sChar.equalsIgnoreCase("l")) return KeyEvent.VK_L;
        if(sChar.equalsIgnoreCase("m")) return KeyEvent.VK_M;
        if(sChar.equalsIgnoreCase("n")) return KeyEvent.VK_N;
        if(sChar.equalsIgnoreCase("o")) return KeyEvent.VK_O;
        if(sChar.equalsIgnoreCase("p")) return KeyEvent.VK_P;
        if(sChar.equalsIgnoreCase("q")) return KeyEvent.VK_Q;
        if(sChar.equalsIgnoreCase("r")) return KeyEvent.VK_R;
        if(sChar.equalsIgnoreCase("s")) return KeyEvent.VK_S;
        if(sChar.equalsIgnoreCase("t")) return KeyEvent.VK_T;
        if(sChar.equalsIgnoreCase("u")) return KeyEvent.VK_U;
        if(sChar.equalsIgnoreCase("v")) return KeyEvent.VK_V;
        if(sChar.equalsIgnoreCase("w")) return KeyEvent.VK_W;
        if(sChar.equalsIgnoreCase("x")) return KeyEvent.VK_X;
        if(sChar.equalsIgnoreCase("y")) return KeyEvent.VK_Y;
        if(sChar.equalsIgnoreCase("z")) return KeyEvent.VK_Z;        
        if(sChar.equalsIgnoreCase("`")) return KeyEvent.VK_BACK_QUOTE;
        if(sChar.equalsIgnoreCase("0")) return KeyEvent.VK_0;
        if(sChar.equalsIgnoreCase("1")) return KeyEvent.VK_1;
        if(sChar.equalsIgnoreCase("2")) return KeyEvent.VK_2;
        if(sChar.equalsIgnoreCase("3")) return KeyEvent.VK_3;
        if(sChar.equalsIgnoreCase("4")) return KeyEvent.VK_4;
        if(sChar.equalsIgnoreCase("5")) return KeyEvent.VK_5;
        if(sChar.equalsIgnoreCase("6")) return KeyEvent.VK_6;
        if(sChar.equalsIgnoreCase("7")) return KeyEvent.VK_7;
        if(sChar.equalsIgnoreCase("8")) return KeyEvent.VK_8;
        if(sChar.equalsIgnoreCase("9")) return KeyEvent.VK_9;
        if(sChar.equalsIgnoreCase("-")) return KeyEvent.VK_MINUS;
        if(sChar.equalsIgnoreCase("=")) return KeyEvent.VK_EQUALS;
        if(sChar.equalsIgnoreCase("!")) return KeyEvent.VK_EXCLAMATION_MARK;
        if(sChar.equalsIgnoreCase("@")) return KeyEvent.VK_AT;
        if(sChar.equalsIgnoreCase("#")) return KeyEvent.VK_NUMBER_SIGN;
        if(sChar.equalsIgnoreCase("$")) return KeyEvent.VK_DOLLAR;
        if(sChar.equalsIgnoreCase("^")) return KeyEvent.VK_CIRCUMFLEX;
        if(sChar.equalsIgnoreCase("&")) return KeyEvent.VK_AMPERSAND;
        if(sChar.equalsIgnoreCase("*")) return KeyEvent.VK_ASTERISK;
        if(sChar.equalsIgnoreCase("(")) return KeyEvent.VK_LEFT_PARENTHESIS;
        if(sChar.equalsIgnoreCase(")")) return KeyEvent.VK_RIGHT_PARENTHESIS;
        if(sChar.equalsIgnoreCase("_")) return KeyEvent.VK_UNDERSCORE;
        if(sChar.equalsIgnoreCase("+")) return KeyEvent.VK_PLUS;
        if(sChar.equalsIgnoreCase("\t")) return KeyEvent.VK_TAB;
        if(sChar.equalsIgnoreCase("\n")) return KeyEvent.VK_ENTER;
        if(sChar.equalsIgnoreCase("[")) return KeyEvent.VK_OPEN_BRACKET;
        if(sChar.equalsIgnoreCase("]")) return KeyEvent.VK_CLOSE_BRACKET;
        if(sChar.equalsIgnoreCase("\\")) return KeyEvent.VK_BACK_SLASH;        
        if(sChar.equalsIgnoreCase(";")) return KeyEvent.VK_SEMICOLON;
        if(sChar.equalsIgnoreCase(":")) return KeyEvent.VK_COLON;
        if(sChar.equalsIgnoreCase("\'")) return KeyEvent.VK_QUOTE;
        if(sChar.equalsIgnoreCase("\"")) return KeyEvent.VK_QUOTEDBL;
        if(sChar.equalsIgnoreCase(",")) return KeyEvent.VK_COMMA;
        if(sChar.equalsIgnoreCase("<")) return KeyEvent.VK_LESS;
        if(sChar.equalsIgnoreCase(".")) return KeyEvent.VK_PERIOD;
        if(sChar.equalsIgnoreCase(">")) return KeyEvent.VK_GREATER;
        if(sChar.equalsIgnoreCase("/")) return KeyEvent.VK_SLASH;
        if(sChar.equalsIgnoreCase(" ")) return KeyEvent.VK_SPACE;  
        if(sChar.equalsIgnoreCase("ctrl")) return KeyEvent.VK_CONTROL;  
        if(sChar.equalsIgnoreCase("alt")) return KeyEvent.VK_ALT;  
        if(sChar.equalsIgnoreCase("shift")) return KeyEvent.VK_SHIFT; 
        if(sChar.equalsIgnoreCase("enter")) return KeyEvent.VK_ENTER; 
        if(sChar.equalsIgnoreCase("caps")) return KeyEvent.VK_CAPS_LOCK; 
        if(sChar.equalsIgnoreCase("esc")) return KeyEvent.VK_ESCAPE; 
        if(sChar.equalsIgnoreCase("tab")) return KeyEvent.VK_TAB; 
        if(sChar.equalsIgnoreCase("backspace")) return KeyEvent.VK_BACK_SPACE; 
        if(sChar.equalsIgnoreCase("delete")) return KeyEvent.VK_DELETE;
        if(sChar.equalsIgnoreCase("home")) return KeyEvent.VK_HOME;
        if(sChar.equalsIgnoreCase("end")) return KeyEvent.VK_END;
        if(sChar.equalsIgnoreCase("pageup")) return KeyEvent.VK_PAGE_UP;
        if(sChar.equalsIgnoreCase("pagedown")) return KeyEvent.VK_PAGE_DOWN;
        if(sChar.equalsIgnoreCase("up")) return KeyEvent.VK_UP;
        if(sChar.equalsIgnoreCase("down")) return KeyEvent.VK_DOWN;
        if(sChar.equalsIgnoreCase("right")) return KeyEvent.VK_RIGHT;
        if(sChar.equalsIgnoreCase("left")) return KeyEvent.VK_LEFT;
        if(sChar.equalsIgnoreCase("insert")) return KeyEvent.VK_INSERT;
        if(sChar.equalsIgnoreCase("numlock")) return KeyEvent.VK_NUM_LOCK;
        if(sChar.equalsIgnoreCase("F1")) return KeyEvent.VK_F1;
        if(sChar.equalsIgnoreCase("F2")) return KeyEvent.VK_F2;
        if(sChar.equalsIgnoreCase("F3")) return KeyEvent.VK_F3;
        if(sChar.equalsIgnoreCase("F4")) return KeyEvent.VK_F4;
        if(sChar.equalsIgnoreCase("F5")) return KeyEvent.VK_F5;
        if(sChar.equalsIgnoreCase("F6")) return KeyEvent.VK_F6;
        if(sChar.equalsIgnoreCase("F7")) return KeyEvent.VK_F7;
        if(sChar.equalsIgnoreCase("F8")) return KeyEvent.VK_F8;
        if(sChar.equalsIgnoreCase("F9")) return KeyEvent.VK_F9;
        if(sChar.equalsIgnoreCase("F10")) return KeyEvent.VK_F10;
        if(sChar.equalsIgnoreCase("F11")) return KeyEvent.VK_F11;
        if(sChar.equalsIgnoreCase("F12")) return KeyEvent.VK_F12;
		return 0;
	}

	void wait(int millSecs)
    {
		try
		{
			Thread.sleep(millSecs);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	void closeConnections()
	{
		try
		{
			for(Socket sock : m_ServerSocketConnections.values())
			{
				sock.close();
			}
			m_ServerSocketConnections.clear();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
		    data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
		                         + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	void processTCPSendCommand(Socket skt, String value, String valueformat, String terminator, String responseSize, String readResponse)
	{
		try
	    {
			//Write a command to the server
			String termStr=terminator.replace("0x0D","\r").replace("0x0A","\n").replace("0x0","\0");    

			byte[] cmdBytes=(valueformat.equalsIgnoreCase("text")) ? (value + termStr).getBytes("UTF-8") : hexStringToByteArray(value);
			skt.getOutputStream().write(cmdBytes);
			
			//Recieve the response
			if(readResponse.equalsIgnoreCase("true"))
			{
				byte[] respBytes=new byte[parseInt(responseSize,255)];
				if(respBytes.length>0)
				{
					skt.getInputStream().read(respBytes);
					String sRsp=new String(respBytes,"UTF-8");
				}
			}
        }
        catch(Exception e) {
           e.printStackTrace();
        }
	}
	
	public void saveConfiguration(String sFileName)
	{
		try
		{
			if(sFileName==null && m_sConfigFile==null )
			{
				if(new File("settings.dat").exists())
				{
					JFileChooser fc=new JFileChooser(System.getProperty("user.dir"));
					fc.setDialogTitle("Save Configuration");
					if(fc.showSaveDialog(this)==fc.APPROVE_OPTION)
					{
						m_sConfigFile=fc.getSelectedFile().getPath();	
						sFileName=m_sConfigFile;
					}
					else
					{
						return;
					}
				}
				else
				{
					m_sConfigFile="settings.dat";
					sFileName=m_sConfigFile;
				}
			}
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			m_ConfigPanel.saveTreeToOutputStream(bos);
			byte[] configData=bos.toByteArray();
			byte[] compressedData=compressBytes(configData);
			FileOutputStream fout = new FileOutputStream(sFileName);
			fout.write(compressedData);
			fout.flush();
			fout.close();			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) { }

    public void windowClosing(WindowEvent e)
    {
		if(e.getSource()==this)
		{
			if(atExit())
		        System.exit(0);
		}
		else if(e.getSource()==m_ConfigFrame)
		{
			saveConfiguration(m_sConfigFile);
			loadConfiguration();
		}
	}

    public void windowDeactivated(WindowEvent e) {  }
    public void windowDeiconified(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) { }

    public boolean atExit()
    {
        NodeEx n=getConfiguration();   
        NodeEx atExitNode=n.getNode("/config/atexit");
        if(atExitNode!=null && atExitNode.getString("./@enabled").equalsIgnoreCase("true"))
            return processButtonNodeEx(atExitNode);        
        return true;
    }
    
    public void keyPressed(KeyEvent e)
	{
	}
	
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){}

   public static void main(String args[])
   {
       /*
       try
       {
           UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
       }
       catch(Exception e)
       {
           e.printStackTrace();
       }
       */

       String sConfigFile="settings.dat";

       if(args.length>0)
       {
           sConfigFile=args[0];
	   }
	   
	   MasterControl frame=new MasterControl(sConfigFile);
           
	   if(args.length>3)
	   {
		   try
		   {
			   if(args[1].equalsIgnoreCase("--set"))
			   {
				   String[] path=args[2].split(Pattern.quote("/@"));
				   NodeEx cfgfile=frame.getConfiguration();
				   if(path.length<2)
				   {
					   System.out.println("Path is not a valid attribute");
					   System.exit(0);
					   return;
				   }
				   
				   if(!frame.login()) 
				   {
					   System.out.println("Invalid password.");
					   System.exit(0);
					   return;
				   }
				   
				   List<NodeEx> ns=cfgfile.getNodes(path[0]);
				   if(ns!=null)
				   {
					   for(NodeEx n : ns)
					   {
						   if(!args[3].equals("null"))
							   n.setAttribute(path[1],args[3]);
						   else
							   n.setAttribute(path[1],null);					   
					   }
				   }
				   
				   ByteArrayOutputStream bos=new ByteArrayOutputStream();
	    		   cfgfile.save(bos);
    			   byte[] configData=bos.toByteArray();
				   byte[] compressedData=compressBytes(configData);
				   FileOutputStream fout = new FileOutputStream(sConfigFile);
				   fout.write(compressedData);
				   fout.flush();
				   fout.close();		
			   }
		   }
		   catch(Exception e)
		   {
			   System.out.println("Error setting attribute.");
			   e.printStackTrace();
		   }
		   System.out.println("Attribute set.");
		   System.exit(0);
	   }
       frame.setVisible(true);
   }
}
