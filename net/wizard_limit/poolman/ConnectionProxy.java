package net.wizard_limit.poolman;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * コネクションのProxy。
 * 
 * @author $Author: sugimoto $
 * @version $Revision: 1.2 $
 */
class ConnectionProxy implements InvocationHandler {
	/** 元になる Connection */
    private Connection con;
    /** ConnectionのtransactionIsolation(保存用) */
    private int transactionIsolation;
    /** 解放時に通知するリスナオブジェクト */
    private FreeConnectionListener listener;
    /** ドライバの利用者に貸し出したProxyオブジェクトへのリファレンス */
    private WeakReference reference;

    /**
     * コンストラクタ。
     * @param con 本物のConnection
     * @param listener コネクションの解放を通知するリスナ
     * @exception SQLException con.getTransactionIsolation()に失敗したとき
     */
    ConnectionProxy(Connection con, FreeConnectionListener listener)
	throws SQLException {
	this.con = con;
	this.listener = listener;
	transactionIsolation = con.getTransactionIsolation();
    }

    /**
     * proxyを生成して返す。
     * @return Connectionのproxyオブジェクト
     */
    Connection getProxy() {
	Connection proxy = (Connection)
	    Proxy.newProxyInstance(con.getClass().getClassLoader(),
		new Class[] {Connection.class}, this);
	reference = new WeakReference(proxy);
	return proxy;
    }

    /**
     * 内部に持っているConnectionをcloseする。
     * 最後の後始末用なので、FreeConnectionListenerには通知しない。
     * @exception SQLException close()に失敗したとき
     */
    void close() throws SQLException {
	if (con != null) {
	    con.close();
	}
    }

    /**
     * コネクションの利用者からのclose処理。
     * Connectionを再利用可能にする。
     */
    void pseudoClose() {
	if (con != null) {
	    try {
		if (!con.isReadOnly()) {
		    con.setReadOnly(false);
		}
	    } catch (SQLException e) {
	    }
	    try {
		if (!con.getAutoCommit()) {
		    con.rollback();
		}
	    } catch (SQLException e) {
	    }
	    try {
		if (transactionIsolation != con.getTransactionIsolation()) {
		    con.setTransactionIsolation(transactionIsolation);
		}
	    } catch (SQLException e) {
	    }
	    try {
		if (!con.getAutoCommit()) {
		    con.setAutoCommit(true);
		}
	    } catch (SQLException e) {
	    }
	    try {
		con.clearWarnings();
	    } catch (SQLException e) {
	    }
	    listener.freeConnection(this);
	    con = null;
	    reference = null;
	}
    }

    /**
     * proxyに対するメソッド呼び出し。
     * InvocationHandlerのメソッド
     * @param proxy proxyオブジェクト
     * @param m 呼び出されたメソッド
     * @param args メソッドの引数
     * @return メソッドの帰り値
     * @exception Throwable すべて
     */
    public Object invoke(Object proxy, Method m, Object[] args)
	throws Throwable {
        Object result;
	try {
	    if (m.getName().equals("close")) {
		result = null;
		pseudoClose();
	    } else {
		result = m.invoke(con, args);
	    }
        } catch (InvocationTargetException e) {
	    throw e.getTargetException();
        } catch (Exception e) {
	    throw new RuntimeException("unexpected invocation exception: " +
		e.getMessage());
	}
	return result;
    }

    /**
     * 本物のConnectionを返す。
     * @return Connection
     */
    Connection getConnection() {
	return con;
    }

    /**
     * 貸し出したproxyオブジェクトがgcされたかどうかを調べる。
     * @return proxyオブジェクトが使用済(解放済)の場合true)
     */
    boolean checkFree() {
	if (reference == null) {
	    return true;
	}
	if (reference.get() == null) {
	    // gcされた
	    return true;
	}
	return false;
    }
}
