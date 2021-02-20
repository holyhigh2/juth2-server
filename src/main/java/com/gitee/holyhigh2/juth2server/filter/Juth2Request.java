package com.gitee.holyhigh2.juth2server.filter;


import com.gitee.holyhigh2.juth2server.Juth2Client;
import com.gitee.holyhigh2.juth2server.principal.Juth2Principal;

import javax.servlet.http.HttpServletRequest;

/**
 * 扩展自HttpServletRequest，增加获取client/principal/user的接口
 *
 * @author holyhigh https://github.com/holyhigh2
 */
public interface Juth2Request extends HttpServletRequest {
    Juth2Client getClient();

    Juth2Principal getPrincipal();

    <T> T getUser();

    /**
     * 是否鉴权请求
     *
     * @return
     */
    boolean isAuthRequest();
}
