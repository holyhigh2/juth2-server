package com.github.holyhigh2.juth2server.filter;

import com.github.holyhigh2.juth2server.Juth2Client;
import com.github.holyhigh2.juth2server.principal.Juth2Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 在level2鉴权通过后，增加获取client/principal的代理方法
 *
 * @author holyhigh https://github.com/holyhigh2
 */
class Juth2RequestWrapper extends HttpServletRequestWrapper implements Juth2Request {
    private Juth2Client client;
    private Juth2Principal principal;
    private boolean isAuthUrl = false;

    public Juth2RequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public Juth2RequestWrapper(HttpServletRequest request, Juth2Client client, Juth2Principal principal) {
        super(request);
        this.client = client;
        this.principal = principal;
        this.isAuthUrl = request.getRequestURI().startsWith(request.getContextPath() + "/juth2/");
    }

    public Juth2Client getClient() {
        return this.client;
    }

    public Juth2Principal getPrincipal() {
        return this.principal;
    }

    public <T> T getUser() {
        return this.principal.getBusinessEntity();
    }

    @Override
    public boolean isAuthRequest() {
        return isAuthUrl;
    }
}
