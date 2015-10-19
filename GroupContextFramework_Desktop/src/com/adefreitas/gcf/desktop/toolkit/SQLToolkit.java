package com.adefreitas.gcf.desktop.toolkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLToolkit 
{
//	// A permanent link to the MySQL Driver JAR
//    static private final String DRIVER = "com.mysql.jdbc.Driver";   
	
    // The connection is static since there only needs to be one per application
    private Connection connection;
    
	// Specific Database Attributes
	private String url;
    private String dbServer;
    private String database;
    private String username;
    private String password;
        
    /**
     * Constructor
     * @param server   - The name of the server (e.g., 192.168.0.10, dfcs-raptor)
     * @param username - The MySQL username to use
     * @param password - The MySQL password to use
     * @param database - The name of the specific MYSQL database
     */
    public SQLToolkit(String server, String username, String password, String database)
    {
        this.dbServer = server;
        this.username = username;
        this.password = password;
        this.database = database;
        this.url      = "jdbc:mysql://" + this.dbServer + ":3306/" + this.database;
        
        try
        {
        	getConnection();
        }
        catch (Exception ex)
        {
        	System.err.println("Problem Getting SQL Connection: " + ex.getMessage());
        }
    }
    
    /**
     * Runs a SELECT Query
     * @param query
     * @return
     */
    public ResultSet runQuery(String query)
    {
        try
        {           
            ResultSet r = getConnection().createStatement().executeQuery(query);
            return r;
        }
        catch (Exception ex)
        {
        	System.out.println("Error Encountered Executing Statement: " + query + "\nError: " + ex.toString());
            return null;
        }
    }
    
    /**
     * Runs an INSERT, UPDATE or DELETE Query
     * @param query
     * @throws Exception
     */
    public void runUpdateQuery(String query)
    {        
        try
        {   
            getConnection().createStatement().executeUpdate(query);
        }
        catch (Exception ex)
        {
            System.out.println("Error Encountered Executing Statement: " + query + "\nError: " + ex.toString());
        }
    }

    /**
     * This retrieves a Connection to the Database (or creates a new one if the previous one timed out)
     * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException 
    {   
    	// Checks to See if the Database Connection is Valid First
        if (connection == null || !connection.isValid(1))
        {
            System.out.print("Creating new DB connection to " + url + ". . . ");
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("DONE!");
        }
        
        return connection;
    }
    
}
