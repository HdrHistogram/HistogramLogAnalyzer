/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnect{
	public Connection connection = null;
	public ResultSet resultSet = null;
	public Statement statement = null;
	public String filename=null;

	public DBConnect(String db_filename) {
		try {
			this.filename = db_filename;
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite::memory:");
//	        connection = DriverManager.getConnection("jdbc:sqlite:"+filename);
			statement = connection.createStatement();
		} catch (Exception e) {
		}
	}
	public void close_db() {
		try {
		    if(resultSet!=null)
		    {
			if(resultSet.isClosed()==false)
			resultSet.close();
			statement.close();
			connection.close();
		    }
		} catch (Exception e) {
		    System.out.println("DDD ERROR");
		}
	}

	public String getDBfilename()
	{
	    return filename;
	}
}
