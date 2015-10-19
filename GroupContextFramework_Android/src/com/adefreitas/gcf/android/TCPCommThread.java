package com.adefreitas.gcf.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.messages.CommMessage;
import com.google.gson.Gson;

public class TCPCommThread extends CommThread
{
	private static final String LOG_NAME = "GCF-Socket [" + new Date().getTime() + "]";
	
	private Socket 	   	  sock;
	private String 		  server;
	private int 		  port;
    private OutputStream  sendStream;
    private InputStream   recvStream;
    private Gson 		  gson;
	private Handler 	  commHandler;
    private String        buffer;

    // Connection Status Variable
    private boolean 		   connected;
    private boolean 		   alive;
    private TransmissionThread transmissionThread;
    
    // TODO:  Verify that this KEEP ALIVE is actually needed
    private String 			 deviceID;
    private Date   		     lastPing 	   = new Date();
    private static final int PING_DURATION = 30000;
    
    /**
     * Constructor
     * @param port
     * @param server
     * @param commHandler
     * @throws IOException
     * @throws UnknownHostException
     */
	public TCPCommThread(CommManager commManager, String deviceID, int port, String server, Handler commHandler) throws IOException, UnknownHostException 
	{
		super(commManager);
		
		System.out.println("TCP COMM THREAD CREATED!");
		
		this.deviceID    = deviceID;
		this.server      = server;
		this.port 		 = port;
	    this.commHandler = commHandler;
	    this.gson 		 = new Gson();
		this.connected   = false;
	    
	    transmissionThread = new TransmissionThread();
	    transmissionThread.start();
    }
     
	public void connect(String ipAddress, int port)
	{
		super.connect(ipAddress, port);
		
		buffer    = "";
		connected = false;
		
		while (!connected)
		{
			try
			{
		    	if (!connected)
				{
		    		Log.d(LOG_NAME, "Connecting to " + server + ":" + port);
		    		sock 	    	   = new Socket(ipAddress, port);
		    	    sendStream  	   = sock.getOutputStream();
		    	    recvStream  	   = sock.getInputStream();
		    	    buffer    		   = "";
		    	    this.send("USER " + deviceID);
		    	    
		    	    connected   	   = true;
				}
			}
			catch (Exception ex)
			{
				Log.e(LOG_NAME, "Connection Attempt Failed");
	
				try
				{
					sleep(1000);
					
					if (sock != null)
					{
						sock.close();
					}
				}
				catch (Exception sleepEx)
				{
					Log.e(LOG_NAME, "A Problem Occurred While Sleeping?");
				}
			}
		}
	}
	
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
            				// Sends the Received Message to a Handler to be Processed
            				Message m = Message.obtain();
            				m.obj     = msg;
            				
            				if (commHandler != null)
            				{
            					commHandler.sendMessage(m);
            				}
             			}
             			     			
             			// Removes the Parsed string from the buffer
             			buffer = buffer.substring(currentMessage.length());
             			             			
             			// Looks for the next whole message
             			currentMessage = getNextJSON(buffer);
             		}	
    	    	}
    	    	else
    	    	{
         			Log.e(LOG_NAME, "Connection lost.  Trying to reconnect.");
         			connect(server, port);
    	    	}

        	}
        	catch (Exception ex)
        	{
        		ex.printStackTrace();
        		connect(server, port);
        	}
     	}
    	
    	if (sock != null)
    	{
    		close();
    	}
     }
    
    private String getNextJSON(String s)
    {
    	int balance = 0;
    	
    	{
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
    	}
    	
    	return null;    	
    }

	public void send(CommMessage message)
    {
		if (message != null)
		{
			send(gson.toJson(message));
		}
	}

	public void sendOneTime(String ipAddress, int port, CommMessage message)
	{
		final String IP   = ipAddress;
		final int    PORT = port;
		final String msg  = gson.toJson(message);
		
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					Socket 		 sock 	   	= new Socket(IP, PORT);
		    	    OutputStream sendStream = sock.getOutputStream();
					
		    	    sendStream.write((msg + "\n").getBytes());
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
	
    private void send(String message) 
    {			
    	if (message != null)
    	{
    		transmissionThread.send(message + "\n");
    	}
    }
    
    private String getResponse() {
	    try 
	    {
		   int dataSize = 0;
		   int count    = 0;
		   
		   while (connected && (dataSize = recvStream.available()) == 0) 
		   {
			   Thread.sleep(50);
			   
			   // TODO:  Check to make sure this is needed!
			   if (new Date().getTime() - lastPing.getTime() > PING_DURATION)
			   {
				   Log.d(LOG_NAME, "Sending Ping");
				   this.send("USER " + deviceID);
				   lastPing = new Date();
			   }
		   }
		   
		   if (connected)
		   {
			   if (dataSize > 0)
			   {
				   byte[] recvBuff = new byte[dataSize];
				   recvStream.read(recvBuff);
				   
				   String str = this.processString(new String(recvBuff, "US-ASCII"));
				   return str;   
			   }
		   }
		   else
		   {
			   Log.e(LOG_NAME, "Disconnected");
			   connect(server, port);
		   }
	    } 
	    catch (Exception ex) 
	    {
		   Log.e(LOG_NAME, "Error in getResponse");
		   ex.printStackTrace();
		   connected = false;
	    }
	    
		return "";
    }

    public void close() 
    {
	    try 
	    {
	       // Terminates the Connection Loops
	       connected = false;
		   alive	 = false;
		   
		   // Terminates the Transmission Thread
		   transmissionThread.kill();
		   
		   if (sendStream != null)
		   {
			   sendStream.close();
		   }
		   
		   if (recvStream != null)
		   {
			   recvStream.close();
		   }
		   
		   if (sock != null)
		   {
			   sock.close();
		   }
	    } 
	    catch (Exception ex) 
	    {
		   Log.e("GCM-Socket", "Error while closing the TCP Socket");
		   ex.printStackTrace();
	    }
    }
    
    class TransmissionThread extends Thread
    {
    	private ArrayList<String> sendBuffer;
    	private boolean 		  loop;
    	
    	public TransmissionThread()
    	{
    		sendBuffer = new ArrayList<String>();
    		loop 	   = true;
    	}
    	
    	public void send(String string)
    	{
    		sendBuffer.add(string);
    	}
    	
    	public void kill()
    	{
    		loop = false;
    	}
    	
    	public void run()
    	{
    		// Keeps Running Until Everything was Sent
    		while (true)
    		{
    			String nextMessage = (sendBuffer.size() > 0) ? sendBuffer.get(0) : null;
    			
    			if (connected && nextMessage != null)
    			{
    				// Grabs the Bytes and Removes the Entry from the Queue
    				final byte buffer[] = nextMessage.getBytes();
    				sendBuffer.remove(0);
    				
    				try
    				{   					
    					sendStream.write(buffer);
    					sendStream.flush();
    					lastPing = new Date();
    					
    					//Log.d(LOG_NAME, "Transmitting " + buffer.length + " bytes.");
    					
    					if (!loop && sendBuffer.size() == 0)
    					{
    						sendStream.write("KILL\n".getBytes());
        					sendStream.flush();
    						break;
    					}
    				}
    				catch (Exception ex)
    				{
    					Log.e(LOG_NAME, "Error while trying to send: " + ex.getMessage());
    					connected = false;
    				}
    			}
    			else
    			{
    				try
    				{
    					Thread.sleep(50);
    				}
    				catch (Exception ex)
    				{
    					Log.e(LOG_NAME, "Error while trying to sleep the sending thread.");
    				}
    			}
    		}
    		
    		Log.d(LOG_NAME, "TCPCommThread Terminated.");
    	}
    }
}
