package test.poolman;

import junit.framework.*;
import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionPoolDriverTest extends TestCase {
    public ConnectionPoolDriverTest(String name) {
	super(name);
    }

    public void test0() throws Exception {
	Class.forName("oracle.jdbc.driver.OracleDriver");
	Class.forName("net.wizard_limit.poolman.ConnectionPoolDriver");
	ConnectionPool pool = ConnectionPool.getInstance("jdbc:oracle:thin:@__DB_HOST__:1521:__DB_SID__", "__DB_USER__", "__DB_PASS__");
	System.out.println(con);
    }
}
