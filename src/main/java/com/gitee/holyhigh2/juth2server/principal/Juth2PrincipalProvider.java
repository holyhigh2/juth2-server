package com.gitee.holyhigh2.juth2server.principal;

import com.gitee.holyhigh2.juth2server.Juth2Client;
import com.gitee.holyhigh2.juth2server.Juth2DataAccessException;
import com.gitee.holyhigh2.juth2server.Juth2Helper;
import com.gitee.holyhigh2.juth2server.Juth2Service;
import com.gitee.holyhigh2.juth2server.config.Juth2Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.beans.Transient;
import java.util.Date;

/**
 * Juth2安全主体，标识了当前访问用户的
 * Juth2会维护一个主体列表，用于处理鉴权和在线用户管理
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
public class Juth2PrincipalProvider implements Juth2Principal {
    private static final long serialVersionUID = 7209776491450668945L;

    public Juth2PrincipalProvider(Juth2Authentication authentication,
                                  String clientId
    ) {
        this.createdTime = new Date();
        this.authentication = authentication;
        this.clientId = clientId;
    }
    public Juth2PrincipalProvider(){

    }

    protected String clientId;
    /**
     * 当前安全主体的业务实体id，通常是业务系统的userId
     */
    protected String businessEntityId;


    protected Date createdTime;

    protected Juth2Authentication authentication;

    /**
     * 鉴权通过后Juth2自动分配的访问码和刷新码
     */
    protected String accessToken;
    protected String refreshToken;

    @Transient
    @Override
    public Juth2Client getClient() throws Juth2DataAccessException {
        return Juth2Properties.getBean(Juth2Service.class)
                .getAllClients().get(clientId);
    }

    @Override
    public Juth2Authentication getAuthentication() {
        return authentication;
    }

    @Transient
    @Override
    public boolean isAuthenticated() {
        return accessToken != null;
    }

    @Transient
    @Override
    public String getToken() {
        return accessToken + " " + refreshToken;
    }

    @Override
    public Date getCreatedTime() {
        return createdTime;
    }

    @Override
    public void signIn(HttpServletResponse response, String businessEntityId) throws Juth2DataAccessException {
        this.businessEntityId = businessEntityId;
        refreshToken(response);
    }

    @Override
    public void refreshToken(HttpServletResponse response) throws Juth2DataAccessException {
        Juth2Properties.getBean(Juth2Service.class).revoke(this);

        //刷新访问码
        String[] tokens = Juth2Helper.genAccessTokenJWTString(getClient(), Juth2Properties.TOKEN_ACCESS_EXPIRE_TIME_IN_SEC);
        //设置token
        accessToken = tokens[1];
        refreshToken = tokens[2];
        createdTime = new Date();

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        response.setHeader("Authorization", "Bearer " + tokens[0]);

        if(Juth2Properties.SECURITY_COOKIE_ENABLED){
            Cookie juth2Auth = new Cookie("Juth2-Auth",tokens[0]);
            juth2Auth.setMaxAge(Juth2Properties.TOKEN_REFRESH_EXPIRE_TIME_IN_SEC);
            juth2Auth.setPath("/");
            juth2Auth.setHttpOnly(true);
            response.addCookie(juth2Auth);
        }

        Juth2Properties.getBean(Juth2Service.class).record(this);
    }

    @Transient
    @Override
    public <T> T getBusinessEntity() {
        return Juth2Properties.getBean(Juth2Service.class).getUser(businessEntityId);
    }

    @Transient
    @Override
    public String getBEId() {
        return businessEntityId;
    }

    public void logout() {//登出主体信息
        this.accessToken = this.refreshToken = null;
    }

    //for serializable
    public String getClientId(){return clientId;}
    public String getBusinessEntityId() {
        return businessEntityId;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }
}
