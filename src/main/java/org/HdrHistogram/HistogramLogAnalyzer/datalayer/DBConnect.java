/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

class DBConnect{
	private Connection connection = null;
	private ResultSet resultSet = null;
	Statement statement = null;
	private String filename=null;

	DBConnect(String db_filename) {
		try {
			this.filename = db_filename;
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite::memory:");
//	        connection = DriverManager.getConnection("jdbc:sqlite:"+filename);
			statement = connection.createStatement();
		} catch (Exception ignored) {
		}
	}
	void close_db() {
		try {
		    if(resultSet!=null) {
				if(!resultSet.isClosed()) {
					resultSet.close();
				}
				statement.close();
				connection.close();
		    }
		} catch (Exception e) {
		    System.out.println("DDD ERROR");
		}
	}
}
