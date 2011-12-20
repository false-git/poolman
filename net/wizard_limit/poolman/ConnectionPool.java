package net.wizard_limit.poolman;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * コネクションプール。
 * getInstance()メソッドでインスタンスを取得し、
 * getConnection()でコネクション取得。
 * @author $Author: sugimoto $
 * @version $Revision: 1.4 $
 */
public class ConnectionPool {
    private static final String URL_KEY = "poolman:url";
    private static Map poolMap = new HashMap();

    /**
     * コネクションプールインスタンスの取得。
     * @param url 実際に接続するDBのURL
     * @param info 接続情報
     * @return ConnectionPool コネクションプール
     */
    public static ConnectionPool getInstance(String url, Properties info) {
	Properties key = new Properties();
	key.putAll(info);
	key.put(URL_KEY, url);
	ConnectionPool pool = null;
	synchronized (poolMap) {
	    pool = (ConnectionPool) poolMap.get(key);
	    if (pool == null) {
		pool = new ConnectionPool(url, info);
		poolMap.put(key, pool);
	    }
	}
	return pool;
    }

    /**
     * コネクションプールインスタンスの取得。
     * @param url 実際に接続するDBのURL
     * @param user 実際にDBに接続するときに使用するユーザ
     * @param password 実際にDBに接続するときに使用するパスワード
     * @return ConnectionPool コネクションプール
     */
    public static ConnectionPool getInstance(String url, String user,
	String password) {
	Properties info = new Properties();
	info.put("user", user);
	info.put("password", password);
	return getInstance(url, info);
    }

    /**
     * コンストラクタ。
     * @param url 実際に接続するDBのURL
     * @param user 実際にDBに接続するときに使用するユーザ
     */
    private ConnectionPool(String url, Properties info) {
	this.url = url;
	this.info = new Properties();
	this.info.putAll(info);
	String errorLog = info.getProperty(ConnectionPoolDriver.ERROR_LOG);
	if (errorLog == null) {
	    errorLog = System.getProperty(ConnectionPoolDriver.ERROR_LOG);
	}
	if (errorLog != null) {
	    try {
		errorOut = new PrintStream(new FileOutputStream(errorLog));
	    } catch (IOException e) {
		log(errorOut, "can't open error log file [" + errorLog + "]",
		    e);
	    }
	}
	String debugLog = info.getProperty(ConnectionPoolDriver.DEBUG_LOG);
	if (debugLog == null) {
	    debugLog = System.getProperty(ConnectionPoolDriver.DEBUG_LOG);
	}
	if (debugLog != null) {
	    try {
		debugOut = new PrintStream(new FileOutputStream(debugLog));
	    } catch (IOException e) {
		log(errorOut, "can't open debug log file [" + debugLog + "]",
		    e);
	    }
	}
	String debugLevel = info.getProperty(ConnectionPoolDriver.DEBUG_LEVEL);
	if (debugLevel == null) {
	    debugLevel = System.getProperty(ConnectionPoolDriver.DEBUG_LEVEL);
	}
	if (debugLevel != null) {
	    try {
		this.debugLevel = Integer.parseInt(debugLevel);
	    } catch (NumberFormatException e) {
		log(errorOut, "Illegal debug level [" + debugLevel + "]", e);
	    }
	}
	String closeInterval = info.getProperty(ConnectionPoolDriver.CLOSE_CHECK_INTERVAL);
	if (closeInterval == null) {
	    closeInterval = System.getProperty(ConnectionPoolDriver.CLOSE_CHECK_INTERVAL);
	}
	if (closeInterval != null) {
	    try {
		this.closeCheckInterval = Long.parseLong(closeInterval) * 1000;
	    } catch (NumberFormatException e) {
		log(errorOut, "Illegal close check interval sec [" + closeInterval + "]", e);
	    }
	}
	String driverName
	    = info.getProperty(ConnectionPoolDriver.DRIVER);
	if (driverName == null) {
	    driverName = System.getProperty(ConnectionPoolDriver.DRIVER);
	}
	if (driverName != null) {
	    try {
		Class.forName(driverName);
	    } catch (ClassNotFoundException e) {
		log(errorOut, "can't load driver class [" + driverName + "]", e);
	    }
	}
	Runtime.getRuntime().addShutdownHook(new FinalizeThread());
	new ProxyWatchThread().start();
    }

    private String url;
    private Properties info;
    private List connections = new LinkedList();
    private List proxies = new LinkedList();
    private boolean inFinalize;

    // プロパティ系
    private PrintStream debugOut = System.out;
    private PrintStream errorOut = System.err;
    private long closeCheckInterval = 60000;
    private int debugLevel = 0;

    /**
     * コネクションを取得する。
     * @return Connection コネクション
     */
    public Connection getConnection() throws SQLException {
	Connection con = null;
	synchronized (connections) {
	    if (connections.size() > 0) {
		con = (Connection) connections.get(0);
		connections.remove(0);
		if (debugLevel > 1) {
		    log(debugOut, "connection reused: " + con);
		}
	    }
	}
	if (con == null) {
	    con = DriverManager.getConnection(url, info);
	    if (debugLevel > 1) {
		log(debugOut, "connection created: " + con);
	    }
	}
	ConnectionProxy proxy = new ConnectionProxy(con,
	    new FreeConnectionListener() {
		public void freeConnection(ConnectionProxy proxy) {
		    if (debugLevel > 1) {
			log(debugOut, "free connection: " + proxy.getConnection());
		    }
		    synchronized (connections) {
			connections.add(proxy.getConnection());
		    }
		    synchronized (proxies) {
			proxies.remove(proxy);
		    }
		}
	    });
	con = proxy.getProxy();
	synchronized (proxies) {
	    proxies.add(proxy);
	}
	return con;
    }

    /**
     * システム終了時に実行するスレッド。
     * 使用中の Connection を close する。
     */
    class FinalizeThread extends Thread {
	public void run() {
	    inFinalize = true;
	    synchronized (connections) {
		for (Iterator i = connections.iterator(); i.hasNext(); ) {
		    Connection con = (Connection) i.next();
		    if (debugLevel > 1) {
			log(debugOut, "connection close: " + con);
		    }
		    try {
			con.close();
		    } catch (Exception e) {
			log(errorOut, "failed to close connection.", e);
		    }
		}
	    }
	    synchronized (proxies) {
		for (Iterator i = proxies.iterator(); i.hasNext(); ) {
		    ConnectionProxy proxy = (ConnectionProxy) i.next();
		    if (debugLevel > 0) {
			log(debugOut, "Warning: connection didn't closed. force close: " + proxy.getConnection());
		    }
		    try {
			proxy.close();
		    } catch (Exception e) {
			log(errorOut, "failed to close connection.", e);
		    }
		}
	    }
            if (errorOut != System.err) {
                try {
                    errorOut.close();
                } catch (Exception e) {
                }
            }
            if (debugOut != System.out) {
                try {
                    debugOut.close();
                } catch (Exception e) {
                }
            }
	}
    }

    /**
     * 未使用の ConnectionProxy を検知するスレッド。
     * gc済のものを見つけたら、再利用可能にする。
     */
    class ProxyWatchThread extends Thread {
	public void run() {
	    while (true) {
		if (inFinalize) {
		    break;
		}
		synchronized (proxies) {
		    List pc = new ArrayList(proxies);
		    try {
			for (Iterator i = pc.iterator(); i.hasNext(); ) {
			    ConnectionProxy proxy = (ConnectionProxy) i.next();
			    if (proxy.checkFree()) {
				// already gc.
				if (debugLevel > 0) {
				    log(debugOut, "Warning: connection didn't closed. gc: " + proxy.getConnection());
				}
				proxy.pseudoClose();
			    }
			}
		    } catch (Exception e) {
			log(errorOut, "exception occured.", e);
		    }
		}
		if (inFinalize) {
		    break;
		}
		try {
		    Thread.sleep(closeCheckInterval);
		} catch (InterruptedException e) {
		    break;
		}
	    }
	}
    }

    private void log(PrintStream out, String message) {
	log(out, message, null);
    }
    private void log(PrintStream out, String message, Throwable t) {
	out.println(getClass().getName() + ": " + message);
	if (t != null) {
	    t.printStackTrace(out);
	}
    }
}
