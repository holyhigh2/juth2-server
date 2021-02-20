package com.gitee.holyhigh2.juth2server;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/**
 * 提供日志
 *
 * @author holyhigh https://github.com/holyhigh2
 */
public abstract class Juth2Log {

    static private Log logger = LogFactory.getLog("Juth2-Server");//Logger.getLogger("Juth2-Server");

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void warning(String msg) {
        logger.warn(msg);
    }

    public static void error(String msg,Exception e){
        logger.error(msg,e);
    }


    public static void main(String[] args) throws Exception {
        Juth2Log.info("sdfsdf");
        Juth2Log.warning("sdfsdf");
    }
}
