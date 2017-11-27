
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;


/*
 * TCPCommunicator is a class that can be used to 
 * send or receive TCP traffic over a network.  
 */


class SocketWorker implements Runnable 
{
	private Socket client;
	private String sResponse;
	public SocketWorker(Socket clientSock, String sRes)
	{
        client=clientSock;
        sResponse=sRes;
	}  
	
	public boolean processCommand()
	{
		try
		{			
			byte[] cmdBytes=new byte[256];      
			int b=0;
			int nBytesRead=0;
			while(b!='\r' && b!=-1 && nBytesRead<256)
			{				
			    b=client.getInputStream().read();			    
			    nBytesRead++;
			    if(b!=-1)
					cmdBytes[nBytesRead-1]=(byte)b;	    
			}
			
			if(nBytesRead==1 && b==-1)//-1)
			{
				System.out.println("Disconnecting: " + client.getInetAddress());
			    return false;
			}
			    
			String sCmd=new String(cmdBytes,0,nBytesRead,"UTF-8");
			System.out.println("Processing command: " + sCmd);
			
			System.out.println("Sending response: " + sResponse);
			client.getOutputStream().write(sResponse.getBytes("UTF-8"));			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void run()
	{
		try
		{
			while(processCommand()){}
			client.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("Server " + client.getInetAddress() + " has disconnected.");
	}
}


public class TCPCommunicator {
	
   public static void asServer(int port,String sResponse)
   {
	   try 
	   {
         ServerSocket svr = new ServerSocket(port);
         System.out.println("Listening on port " + String.valueOf(port) + "...");
         System.out.println("--------------------------------------");
         boolean bRun=true;
         while(bRun)
         {			 
			 Socket sock = svr.accept();
			 System.out.println("Server " + sock.getInetAddress() + " has connected!");			 
			 Thread t = new Thread(new SocketWorker(sock,sResponse));
			 t.start();
		 }
         svr.close();
                  
      }
      catch(Exception e) 
      {
         e.printStackTrace();
      }
   }	
   
   public static void asClient(String ipAddress, int port)
   {
	   try 
	   {
		 boolean bRun=true;		 
		 InputStreamReader converter = new InputStreamReader(System.in);
		 BufferedReader in = new BufferedReader(converter);
		 
		 
	     Socket skt = new Socket(ipAddress, port);
		 while(bRun)
		 {			 
			 System.out.println("---------------------------------");			 
			 System.out.print("Enter a command: ");
			 String data=in.readLine();
			 if(data.equalsIgnoreCase("exit"))
			 {    
			     break;
			 }		
			 
			 //Write a command to the server	 
			 byte[] cmdBytes=(data + "\r").getBytes("UTF-8");			         
			 skt.getOutputStream().write(cmdBytes);

             //Recieve the response
			 System.out.print("Received response: ");
			 byte[] respBytes=new byte[256];
			 skt.getInputStream().read(respBytes);
			 String sRsp=new String(respBytes,"UTF-8");
			 System.out.println(sRsp);   
		 }
		 skt.close(); 
      }
      catch(Exception e) {
         e.printStackTrace();
      }

   }
   
   public static boolean isInteger( String input )  
   {  
		try
		{  
			Integer.parseInt( input );  
		    return true;  
		}  
		catch( Exception e ) 
		{  
			return false;  
		}    
   } 
   
   public static void printUsage()
   {
	   System.out.println("\nUsage:\njava TCPCommunicator server [port] [msg]");
	   System.out.println("java TCPCommunicator client [port] [ipaddress]");
	   System.out.println("\nExample:\n\nStart a server using:\n java TCPCommunicator server 8000 OK");
	   System.out.println("\nThen, in another terminal, connect to the server using:\n java TCPCommunicator client 127.0.0.1 8000\n");
   }
	
   public static void main(String args[]) 
   {  
	  if(args.length<3)
	  {
		  System.out.println("\nNot enough parameters");
		  printUsage();
		  return;
	  } 	  
	  
      if(args[0].equalsIgnoreCase("server"))
      {	
		  if(!isInteger(args[1]))
		  {
			  System.out.println("\nPort is not a number: " + args[1]);
			  printUsage();
			  return;
		  }	  
		  asServer(Integer.parseInt(args[1]),args[2] + "\r");
	  }
	  else if(args[0].equalsIgnoreCase("client"))
	  {
		  if(!isInteger(args[2]))
		  {
			  System.out.println("\nPort is not a number: " + args[2]);
			  printUsage();
			  return;
		  }
		  asClient(args[1],Integer.parseInt(args[2]));
	  }
	  else 
	  {
		  System.out.println("\nExpecting \"client\" or \"server\". Found: " + args[0]);
		  printUsage();
	  }
	  
	  System.out.println("\nDone.");
   }
}
