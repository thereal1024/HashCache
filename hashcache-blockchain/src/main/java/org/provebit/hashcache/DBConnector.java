package org.provebit.hashcache;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnector {

	private static String user = "root";
	private static String pw = "";
	private static String host = "localhost";
	private static int port = 3306;
	private static String db = "hashcache";
	
	public static Connection getConnection() throws SQLException {
		Properties connectionProps = new Properties();
		connectionProps.put("user", user);
	    connectionProps.put("password", pw);
	    String target = "jdbc:mysql://" + host + ":" + port + "/" + db;
		return DriverManager.getConnection(target, connectionProps);
	}
}
