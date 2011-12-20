package net.wizard_limit.poolman;

import java.sql.Connection;

/**
 * コネクションが解放された通知を受け取るリスナ。
 * 
 * @author $Author: sugimoto $
 * @version $Revision: 1.2 $
 */
interface FreeConnectionListener {
    /**
     * コネクションが解放されたことを通知する。
     * @param proxy 解放された ConnectionProxy
     */
    public void freeConnection(ConnectionProxy proxy);
}
