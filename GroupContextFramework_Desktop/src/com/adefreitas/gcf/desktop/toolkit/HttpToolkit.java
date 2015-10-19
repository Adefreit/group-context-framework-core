package com.adefreitas.gcf.desktop.toolkit;

import java.io.*;
import java.net.*;

public class HttpToolkit 
{
	/**
	 * Standard HTTP PUT
	 * @param urlString
	 * @param body
	 */
	public static void put(String urlString, String body)
	{		
		try
		{
			URL url 			  = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			
			con.setRequestMethod("PUT");
			con.setDoOutput(true);
			
			OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
			out.write(body);
			out.flush();
			out.close();
			con.getInputStream().close();
			con.disconnect();
		}
		catch (Exception ex)
		{
			System.out.println("Problem Occurred During HTTP PUT: [URL=" + urlString + ", BODY=" + body + "]");
			System.out.println("Error: " + ex.getMessage());
		}
	}
	
	/**
	 * Standard HTTP GET
	 * @param urlString
	 */
	public static String get(String urlString) 
	{
	      URL 				url;
	      HttpURLConnection conn;
	      BufferedReader 	rd;
	      String 			line;
	      String 			result = "";
	      
	      try 
	      {
	         url = new URL(urlString);
	         conn = (HttpURLConnection) url.openConnection();
	         conn.setRequestMethod("GET");
	         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	         
	         while ((line = rd.readLine()) != null) 
	         {
	            result += line + "\n";
	         }
	         
	         rd.close();
	         conn.disconnect();
	      } 
	      catch (Exception ex)
	      {
				System.out.println("Problem Occurred During HTTP GET: [URL=" + urlString + "]");
				System.out.println("Error: " + ex.getMessage());
	      }
	      
	      return result;
	   }

	/**
	 * Standard HTTP POST
	 */
	public static String post(String urlString, String body)
	{
		String result = "";
		
		try
		{
			URL obj = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			//add reuqest header
			con.setRequestMethod("POST");
			//con.setRequestProperty("User-Agent", "Mozilla/5.0");
			//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(body);
			wr.flush();
			wr.close();

			//int responseCode = con.getResponseCode();
			//System.out.println("\nSending 'POST' request to URL : " + urlString);
			//System.out.println("Post parameters : " + body);
			//System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) 
			{
				response.append(inputLine);
			}
			
			in.close();
			
			//print result
			result = response.toString();	
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * http://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
	 * @param filename
	 * @param urlString
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static void downloadFile(final String urlString, final String filename) 
	{    
	    BufferedInputStream in   = null;
	    FileOutputStream    fout = null;
	    File 				f    = new File(filename);
	    File				dir  = new File(filename.substring(0, filename.lastIndexOf("/") + 1));
	    
	    //System.out.println("Downloading . . .\nURL: " + urlString + "\nFile: " + filename);
	    
		try
	    {
	    	try 
		    {	
	    		if (!dir.exists())
	    		{
	    			dir.mkdirs();
	    		}
	    		
	    		if (!f.exists())
	    		{
	    			f.createNewFile();
	    		}
	    		
		        in   = new BufferedInputStream(new URL(urlString).openStream());
		        fout = new FileOutputStream(filename);

		        final byte data[] = new byte[1024];
		        int count;
		        while ((count = in.read(data, 0, 1024)) != -1) 
		        {
		            fout.write(data, 0, count);
		        }
		    } 
		    finally 
		    {
		        if (in != null)
		        {
		            in.close();
		        }
		        if (fout != null) 
		        {
		            fout.close();
		        }
		    }
	    }
	    catch (Exception ex)
	    {
		    System.out.println("Problem Occurred while Downloading . . .\nURL: " + urlString + "\nFile: " + filename);
		    System.out.println("Error: " + ex.getMessage());
	    }
	}
}
