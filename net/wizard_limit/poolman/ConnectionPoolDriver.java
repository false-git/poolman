package net.wizard_limit.poolman;

import java.util.Properties;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;

/**
 * コネクションプールJDBCドライバ。
 * Class.forName("net.wizard_limit.poolman.ConnectionPoolDriver");<br>
 * で登録する。<br>
 * URL: jdbc:poolman:実際のDBのURL<br>
 * ユーザ名、パスワードなどは実際のDBに接続するものを指定する。<br>
 * コネクションプールの動作を変えるには、以下の項目をプロパティに設定する。<br>
 * <table border>
 * <tr><th>プロパティ</th><th>意味</th><th>デフォルト値/必須</th></tr>
 * <tr><td>poolman:debugLevel</td><td>デバッグレベル(0〜2)</td><td>0</td></tr>
 * <tr><td>poolman:debugLog</td><td>デバッグログファイル名</td><td>標準出力</td></tr>
 * <tr><td>poolman:errorLog</td><td>エラーログファイル名</td><td>標準エラー出力</td></tr>
 * <tr><td>poolman:driver</td><td>実際に接続するJDBCドライバクラス名</td><td>なし</td></tr>
 * <tr><td>poolman:closeCheckIntervalSec</td><td>解放された回線のチェック間隔(秒)</td><td>60</td></tr>
 * </table>
 * 
 * @author $Author: sugimoto $
 * @version $Revision: 1.2 $
 */
public class ConnectionPoolDriver implements Driver {
    /** 許容する URL */
    public static final String URL_HEAD = "jdbc:poolman:";
    /** デバッグレベル */
    public static final String DEBUG_LEVEL = "poolman:debugLevel";
    /** デバッグログ */
    public static final String DEBUG_LOG = "poolman:debugLog";
    /** エラーログ */
    public static final String ERROR_LOG = "poolman:errorLog";
    /** 使用するドライバクラス名 */
    public static final String DRIVER = "poolman:driver";
    /** 回線解放チェック間隔(秒) */
    public static final String CLOSE_CHECK_INTERVAL = "poolman:closeCheckIntervalSec";

    // クラスイニシャライザで、DriverManagerに登録する。
    static {
	try {
	    DriverManager.registerDriver(new ConnectionPoolDriver());
	} catch (SQLException e) {
	}
    }

    // 特に意味はないが、他からインスタンス化できないようにする。
    private ConnectionPoolDriver() {
    }

    // 以下、Driver で定義されるメソッド

    /**
     * Attempts to make a database connection to the given URL.
     * The driver should return "null" if it realizes it is the wrong kind
     * of driver to connect to the given URL.  This will be common, as when
     * the JDBC driver manager is asked to connect to a given URL it passes
     * the URL to each loaded driver in turn.
     *
     * <P>The driver should raise a SQLException if it is the right 
     * driver to connect to the given URL, but has trouble connecting to
     * the database.
     *
     * <P>The java.util.Properties argument can be used to passed arbitrary
     * string tag/value pairs as connection arguments.
     * Normally at least "user" and "password" properties should be
     * included in the Properties.
     *
     * @param url the URL of the database to which to connect
     * @param info a list of arbitrary string tag/value pairs as
     * connection arguments. Normally at least a "user" and
     * "password" property should be included.
     * @return a <code>Connection</code> object that represents a
     *         connection to the URL
     * @exception SQLException if a database access error occurs
     */
    public Connection connect(String url, Properties info)
	throws SQLException {
	if (!acceptsURL(url)) {
	    return null;
	}
	String validUrl = url.substring(URL_HEAD.length(), url.length());
	return ConnectionPool.getInstance(validUrl, info).getConnection();
    }

    /**
     * Returns true if the driver thinks that it can open a connection
     * to the given URL.  Typically drivers will return true if they
     * understand the subprotocol specified in the URL and false if
     * they don't.
     *
     * @param url the URL of the database
     * @return true if this driver can connect to the given URL  
     */
    public boolean acceptsURL(String url) {
	if (url.startsWith(URL_HEAD)) {
	    return true;
	}
	return false;
    }

    /**
     * Gets information about the possible properties for this driver.
     * <p>The getPropertyInfo method is intended to allow a generic GUI tool to 
     * discover what properties it should prompt a human for in order to get 
     * enough information to connect to a database.  Note that depending on
     * the values the human has supplied so far, additional values may become
     * necessary, so it may be necessary to iterate though several calls
     * to getPropertyInfo.
     *
     * @param url the URL of the database to which to connect
     * @param info a proposed list of tag/value pairs that will be sent on
     *          connect open
     * @return an array of DriverPropertyInfo objects describing possible
     *          properties.  This array may be an empty array if no properties
     *          are required.
     * @exception SQLException if a database access error occurs
     */
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
	throws SQLException {
	return new DriverPropertyInfo[0];
    }

    /**
     * Gets the driver's major version number. Initially this should be 1.
     * @return this driver's major version number
     */
    public int getMajorVersion() {
	return 1;
    }

    /**
     * Gets the driver's minor version number. Initially this should be 0.
     * @return this driver's minor version number
     */
    public int getMinorVersion() {
	return 0;
    }

    /**
     * Reports whether this driver is a genuine JDBC
     * COMPLIANT<sup><font size=-2>TM</font></sup> driver.
     * A driver may only report true here if it passes the JDBC compliance
     * tests; otherwise it is required to return false.
     *
     * JDBC compliance requires full support for the JDBC API and full support
     * for SQL 92 Entry Level.  It is expected that JDBC compliant drivers will
     * be available for all the major commercial databases.
     *
     * This method is not intended to encourage the development of non-JDBC
     * compliant drivers, but is a recognition of the fact that some vendors
     * are interested in using the JDBC API and framework for lightweight
     * databases that do not support full database functionality, or for
     * special databases such as document information retrieval where a SQL
     * implementation may not be feasible.
     */
    public boolean jdbcCompliant() {
	return false;
    }
}
