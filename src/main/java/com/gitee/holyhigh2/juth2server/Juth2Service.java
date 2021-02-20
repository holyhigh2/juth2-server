package com.gitee.holyhigh2.juth2server;

import com.auth0.jwt.interfaces.Claim;
import com.gitee.holyhigh2.juth2server.principal.Juth2Principal;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


/**
 * 鉴权服务，提供用于整个鉴权过程的各种操作
 * 对于数据操作方法根据应用场景和部署方式实现
 *
 * @author holyhigh https://github.com/holyhigh2
 */
public interface Juth2Service {

    /**
     * 查询所有juth2Client
     * client的存储位置完全由实现决定，可以是非持久化数据缓存如redis，也可以是持久化数据库
     * 也因为如此，当实现存在任何可能导致无法读取数据时需要抛出异常
     *
     * @return 系统中注册的所有client的map<clientId, client>
     * @throws Juth2DataAccessException
     */
    Map<String, Juth2Client> getAllClients() throws Juth2DataAccessException;

    /**
     * 获取当前登录的client
     *
     * @param request http请求对象
     * @return Juth2Client对象或null
     * @throws Juth2DataAccessException
     */
    default Juth2Client getClient(HttpServletRequest request) throws Juth2DataAccessException {
        Map<String, Claim> claimMap = (Map<String, Claim>) request.getAttribute("Juth2.jwt.claims");
        Claim clientIdObj = claimMap.get("client_id");
        if (clientIdObj == null) return null;

        return getAllClients().get(clientIdObj.asString());
    }

    /**
     * 获取当前登录的连接对象
     *
     * @param request
     * @return Juth2Connection对象或null
     * @throws Juth2DataAccessException
     */
    default Juth2Principal getPrincipal(HttpServletRequest request) throws Juth2DataAccessException {
        @SuppressWarnings("unchecked") Map<String, Claim> claimMap = (Map<String, Claim>) request.getAttribute("Juth2.jwt.claims");
        Claim accessTokenObj = claimMap.get("at_hash");
        Claim refreshTokenObj = claimMap.get("refresh_token");
        if (accessTokenObj == null) return null;
        String token = accessTokenObj.asString();
        if (refreshTokenObj != null) {
            token += " " + refreshTokenObj.asString();
        }

        return getAllPrincipals().get(token);
    }

    /**
     * 查询当前所有接入系统的Principal
     * Principal的存储位置完全由实现决定，可以是非持久化数据缓存如redis，也可以是持久化数据库
     * 正因如此，当实现存在任何可能导致无法读取数据时需要抛出异常
     *
     * @return map<accessToken, principal>
     * @throws Juth2DataAccessException
     */
    Map<String, Juth2Principal> getAllPrincipals() throws Juth2DataAccessException;

    /**
     * 通过业务系统userId获取业务用户对象
     *
     * @param userId
     * @param <T>
     * @return
     */
    <T> T getUser(String userId);

    /**
     * 登出或者超时后，移除当前已授权主体
     * 对于系统退出或者强制下线操作，可以通过清除对应的principal来实现
     *
     * @param principal
     */
    void revoke(Juth2Principal principal);


    /**
     * 保存已授权的主体对象到存储中
     * 当你使用多验证实例时（比如负载多网关），请务必保存到共享存储中
     *
     * @param principal
     */
    void record(Juth2Principal principal);

    /**
     * 获取scope标识符对应的url列表。Juth2会根据scope中的url对请求访问进行过滤
     * url可以是具体的全路径也可以是包含通配符的pattern
     *
     * @param scope
     * @return
     */
    List<String> getScopeUrlList(String scope);
}
