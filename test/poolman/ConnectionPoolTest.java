package test.poolman;

import junit.framework.*;
import java.util.Properties;
import java.util.HashMap;
import net.wizard_limit.poolman.ConnectionPool;

public class ConnectionPoolTest extends TestCase {
    public ConnectionPoolTest(String name) {
	super(name);
    }

    public void test0() throws Exception {
	Class.forName("oracle.jdbc.driver.OracleDriver");
	ConnectionPool pool = ConnectionPool.getInstance("jdbc:oracle:thin:@__DB_HOST__:1521:__DB_SID__", "__DB_USER__", "__DB_PASS__");
	System.out.println(pool.getConnection());
	java.sql.Connection con = pool.getConnection();
	System.out.println(con);
	con.close();
	System.out.println(pool.getConnection());
	System.out.println(con = pool.getConnection());
	System.gc();
    }
}
