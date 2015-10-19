package com.adefreitas.gcf.desktop;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.messages.CommMessage;
import com.google.gson.Gson;

public class TCPCommThread extends CommThread
{
	private boolean DEBUG = false;
	
	private String 			 serverIP;
	private int 			 port;
    private Socket 	   		 sock;
    private OutputStream  	 sendStream;
    private InputStream   	 recvStream;
    private Gson 		     gson;
    private String           buffer;
	
    private String 			 deviceID;
    private Date   			 lastPing 	   = new Date();
    private static final int PING_DURATION = 30000;
    
    // Connection Status Variable
    private boolean connected;	// Flag used to determine if the socket is still connected
    private boolean alive;		// Flag used to determine if this thread should still be running and trying to connect
    
    /**
     * Constructor
     * @param port		- the port
     * @param serverIP	- the IP address of the destination machine (the TCP relay)
     * @param processor - where fully formed messages should be delivered once assembled
     */
	public TCPCommThread(CommManager commManager, String deviceID, int port, String serverIP)  
	{
		super(commManager);
		
		this.deviceID  = deviceID;
		this.port      = port;
		this.serverIP  = serverIP;
	    gson 	   	   = new Gson();
	    
	    // Repeatedly Tries to Connect
		connect(serverIP, port);
    }
     
	/**
	 * Repeatedly attempts to connect to the destination
	 * @param serverIP - the IP address of the destination machine (the TCP relay?)
	 * @param port 	   - the port
	 */
	public void connect(String serverIP, int port)
	{
		super.connect(serverIP, port);
		
		// Initializes Connection Status Variables
		connected = false;
				
		// Erases the Buffer
		this.buffer = "";
		
		try 
	    {
			if (sendStream != null)
			{
				sendStream.close();
				sendStream = null;
			}
			
			if (recvStream != null)
			{
				recvStream.close();
				recvStream = null;
			}
			
			if (sock != null)
			{
				sock.close();
				sock = null;
			}
	    } 
	    catch (Exception ex) 
	    {
		   System.err.println("Error in close");
		   ex.printStackTrace();
	    }
		
		while (!connected)
		{
			try
			{
				log("Connecting to " + serverIP + ":" + port);
			    sock 	   	     = new Socket(serverIP, port);
			    sendStream 	     = sock.getOutputStream();
			    recvStream 	     = sock.getInputStream();
			    buffer 	   	     = "";
			    this.send("USER " + deviceID);
			    connected 	     = true;
			}
			catch (Exception ex)
			{
				log("Connection Attempt Failed");
				
				try
				{
					Thread.sleep(1000);
					
					if (sock != null)
					{
						sock.close();
					}
				}
				catch (Exception sleepEx)
				{
					log("A Problem Occurred While Sleeping?");
				}
			}	
		}
	}

	/**
	 * Closes the Socket Connection
	 */
    public void close() 
    {
		try 
	    {
			if (sendStream != null)
			{				
				sendStream.close();
				sendStream = null;
			}
			
			if (recvStream != null)
			{
				recvStream.close();
				recvStream = null;
			}
			
			if (sock != null)
			{
				sock.close();
				sock = null;
			}
	    } 
	    catch (Exception ex) 
	    {
		   System.err.println("Error in close");
		   ex.printStackTrace();
	    }
		
		connected = false;
		alive 	  = false;
    }
	
	/**
	 * Repeatedly Listens for Packets as they Arrive
	 */
    public void run()
    {
    	alive = true;
    	
     	while (alive)
     	{
     		try
     		{
     			// Gets the STRING message from the server
         		buffer += getResponse();
         		
         		if (connected)
         		{
             		String currentMessage = getNextJSON(buffer);
             		
             		while (currentMessage != null)
             		{     			
             			// Converts the string message into a MESSAGE object
             			CommMessage msg = CommMessage.jsonToMessage(currentMessage);
             			
             			// Allows this Thread to Track WHO it has Seen Messages From
             			this.addToArp(msg.getDeviceID());
             			
             			// Sends the message to the GUI
             			if (msg != null)
             			{             				
             				((DesktopCommManager)this.getCommManager()).onMessage(msg);	
             			}
             						
             			// Removes the Parsed string from the buffer
             			buffer = buffer.substring(currentMessage.length());
             			
             			// Looks for the next whole message
             			currentMessage = getNextJSON(buffer);
             		}
         		}
         		else
         		{
         			log("Connection lost.  Trying to reconnect.");
         			connect(serverIP, port);
         		}
     		}
     		catch (Exception ex)
     		{
     			ex.printStackTrace();
     			connect(serverIP, port);
     		}
     	}
     	
     	close();
    }

    /**
     * Sends a Communications Message to the TCP Destination
     */
	public void send(CommMessage message)
    {
		if (message != null)
		{
		    send(gson.toJson(message));	
		}
	}

	public void sendOneTime(String ipAddress, int port, CommMessage message)
	{
		final String IP   	 = ipAddress;
		final int    PORT 	 = port;
		final String MESSAGE = gson.toJson(message);
		
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					Socket 		 sock 	   	= new Socket(IP, PORT);
		    	    OutputStream sendStream = sock.getOutputStream();
					
		    	    sendStream.write((MESSAGE + "\n").getBytes());
					sendStream.flush();
					
					sendStream.write(("KILL\n").getBytes());
					sendStream.flush();
					
					sendStream.close();
					sock.close();
				}
	    		catch (Exception ex)
	    		{
	    			ex.printStackTrace();
	    		}
			}
		};
		
		t.start();
	}
	
	/**
	 * Sends a String to the TCP Destination
	 * @param message
	 */
    private void send(String message) 
    {
    	message = message + "\n";
    	
	    try 
	    {
		   byte[] sendBuff = new byte[message.length()];
		   sendBuff = message.getBytes("US-ASCII");
		   sendStream.write(sendBuff);
		   sendStream.flush();
		   
		   lastPing = new Date();
	    } 
	    catch (Exception ex) 
	    {
		   System.err.println("Error while trying to send.  Attempting to reconnect to " + serverIP + ":" + port);
		   ex.printStackTrace();
		   
		   // Keeps Trying to Connect
		   connect(serverIP, port);
	    }
    }
    
    /**
     * Looks at a String and grabs the first complete JSON Message
     * @param s
     * @return
     */
    private String getNextJSON(String s)
    {
    	// Keeps Track of the Number of {'s (+1) and }'s (-1) 
    	int balance = 0;
    	
    	for (int i=0; i<s.length(); i++)
    	{
    		char c = s.charAt(i);
    		
    		if (c=='{')
    		{
    			balance++;
    		}
    		else if(c=='}')
    		{
    			balance--;
    		}
    		
    		if (balance == 0)
    		{
    			return s.substring(0, i+1);
    		}	
    	}
    	
    	return null;    	
    }
    
    /**
     * Grabs the Next TCP Response from the Socket (note, there could be nothing here as well)
     * @return
     */
    private String getResponse() 
    {
	    try 
	    {
		   int dataSize = 0;
		   
		   // Continually Checks to See if there is Data
		   while (connected && (dataSize = recvStream.available()) == 0) 
		   {
			   Thread.sleep(10);
			   
			   // TODO:  Check to make sure this is needed!
			   if (new Date().getTime() - lastPing.getTime() > PING_DURATION)
			   {
				   this.send("USER " + deviceID);
				   lastPing = new Date();
			   }
		   }
		   
		   if (connected)
		   {
			   log("Received " + dataSize + " bytes from TCP Stream");
			   if (dataSize > 0)
			   {
				   byte[] recvBuff = new byte[dataSize];
				   recvStream.read(recvBuff);
				   
				   // Creates and Returns the String
				   String str = this.processString(new String(recvBuff, "US-ASCII"));
				   return str;   
			   }   
		   }
	    } 
	    catch (Exception ex) 
	    {
		   System.err.println("Error in getResponse.  Reconnecting.");
		   ex.printStackTrace();

		   // Attempts to Reestablish Connection (Although Data will be Flushed)
		   connect(serverIP, port);
	    }
	    
		return "";
    }

    /**
     * Outputs a Message on the Queue
     * @param message
     */
    private void log(String message)
    {
    	if (DEBUG)
    	{
    		System.out.println(message);
    	}
    }
}
