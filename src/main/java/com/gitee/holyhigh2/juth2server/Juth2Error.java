package com.gitee.holyhigh2.juth2server;

/**
 * 读Juth2错误进行分类，并封装错误信息。
 * 返回友好的错误信息到网页/ajax/接口等
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
public class Juth2Error {
    public enum Types {
        Token("Token error"),//访问/刷新码错误，包括格式、内容、过期等
        Sys("System error"),//系统内部错误
        Auth("Auth error");//授权错误，包括没有url权限，被强制登出等

        private final String type;

        Types(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }

    /**
     * 返回包装后的juth2错误信息，用于网页/ajax/接口等错误展示
     *
     * @param type
     * @param desc
     * @return
     */
    public static String getErrorMessage(Types type, String desc) {
        if (desc == null) desc = "";
        return "Juth2-Server: " + type.getType() + "(" + desc + ")";
    }
}
