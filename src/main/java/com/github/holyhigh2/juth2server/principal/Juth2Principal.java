package com.github.holyhigh2.juth2server.principal;

import com.github.holyhigh2.juth2server.Juth2Client;
import com.github.holyhigh2.juth2server.Juth2DataAccessException;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Date;

/**
 * 表示一个Juth2的访问主体，会在授权通过后被保存在主体列表中
 * 访问主体包括请求头信息、鉴权请求信息、鉴权后的token信息、业务关联信息
 *
 * @author holyhigh https://github.com/holyhigh2
 */
public interface Juth2Principal extends Serializable {
    /**
     * 确认主体可以接入系统
     *
     * @param businessEntityId 业务实体id
     */
    void signIn(HttpServletResponse response, String businessEntityId) throws Juth2DataAccessException;

    /**
     * 更新principal的访问码
     *
     * @param response
     */
    void refreshToken(HttpServletResponse response) throws Juth2DataAccessException;

    /**
     * 获取业务系统中的User对象，该方法在grant之前
     *
     * @return
     */
    <T> T getBusinessEntity();

    /**
     * 获取业务实体id
     *
     * @return 如果授权时未赋值，为null
     */
    String getBEId();

    /**
     * 获取principal所属的client
     *
     * @return
     */
    Juth2Client getClient() throws Juth2DataAccessException;

    /**
     * 鉴权信息
     *
     * @return
     */
    Juth2Authentication getAuthentication();

    /**
     * 是否已通过鉴权
     *
     * @return
     */
    boolean isAuthenticated();

    /**
     * 返回已授权principal的token
     *
     * @return accessToken 1*space refreshToken
     */
    String getToken();


    /**
     * 获取principal的创建时间
     *
     * @return
     */
    Date getCreatedTime();
}
