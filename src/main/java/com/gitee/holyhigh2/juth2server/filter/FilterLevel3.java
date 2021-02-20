package com.gitee.holyhigh2.juth2server.filter;

import com.auth0.jwt.interfaces.Claim;
import com.gitee.holyhigh2.juth2server.*;
import com.gitee.holyhigh2.juth2server.config.Juth2Properties;
import com.gitee.holyhigh2.juth2server.principal.Juth2Authentication;
import com.gitee.holyhigh2.juth2server.principal.Juth2Principal;
import com.gitee.holyhigh2.juth2server.principal.Juth2PrincipalProvider;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * 进行oauth2 level3 的权限过滤，包括
 * 1. client检测
 * 2. principal检测(资源请求/刷新token)
 * 3. scope检测
 * 4. request封装
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
@Order(Juth2Properties.FILTER_START_NUMBER + 2)
@Component
public class FilterLevel3 implements Filter {
    Juth2Service juth2Service;
    final static AntPathMatcher urlMatcher = new AntPathMatcher();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        juth2Service = WebApplicationContextUtils.
                getRequiredWebApplicationContext(filterConfig.getServletContext()).
                getBean(Juth2Service.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        Juth2ResponseWrapper response = (Juth2ResponseWrapper) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        //noinspection unchecked
        Map<String, Claim> claimMap = (Map<String, Claim>) request.getAttribute("Juth2.jwt.claims");
        boolean isAuthRequest = (boolean) request.getAttribute("Juth2.isAuthRequest");
        Claim clientIdClaim = claimMap.get("client_id");
        Claim grantTypeClaim = claimMap.get("grant_type");
        Claim scopeClaim = claimMap.get("scope");

        Juth2Client client = null;
        Juth2Principal principal = null;

        if (isAuthRequest) {//鉴权请求
            //鉴权请求，但是url不匹配
            if (!isAuthRequestUrl(request, null)) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "Invalid url of authentication"));
                return;
            }
            try {
                client = juth2Service.getAllClients().get(clientIdClaim.asString());
            } catch (Juth2DataAccessException e) {
                Juth2Log.error("getAllClients in level3",e);
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Sys, "Data access error"));
                return;
            }

            //client为空有可能是实现层问题，如返回map错误等。也可能是请求端 的客户id错误
            if(client == null){
                response.sendError(HttpStatus.UNAUTHORIZED.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "No client found"));
                return;
            }

            //client match check
            if (!isClientMatch(client, claimMap)
            ) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "Invalid scope or grant-type"));
                return;
            }

            //如果是刷新token
            String grantType = grantTypeClaim.asString();
            if (Juth2Properties.REFRESH_TOKEN_GRANT_TYPE.equals(grantType)) {
                try {
                    principal = juth2Service.getPrincipal(request);
                    if (principal == null) {
                        //无法确定是服务器重启导致数据丢失还是已经被登出
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "You have been logged out"));
                        return;
                    }
                } catch (Exception e) {
                    Juth2Log.error("getPrincipal in level3",e);
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Sys, "Data access error"));
                    return;
                }
                //刷新码过期
                Date createdTime = principal.getCreatedTime();
                createdTime = DateUtils.addSeconds(createdTime, Juth2Properties.TOKEN_REFRESH_EXPIRE_TIME_IN_SEC);
                if (DateUtils.truncatedCompareTo(new Date(), createdTime, Calendar.MINUTE) > 0) {
                    response.sendError(HttpStatus.FORBIDDEN.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "The refresh token in the request is expired"));
                    return;
                }
            } else {
                Claim codeObj = claimMap.get("code");
                Claim usernameObj = claimMap.get("username");
                Claim passwordObj = claimMap.get("password");

                //创建principal
                principal = new Juth2PrincipalProvider(
                        new Juth2Authentication(
                                grantTypeClaim.asString(),
                                scopeClaim.asString(),
                                codeObj == null ? "" : codeObj.asString(),
                                usernameObj == null ? "" : usernameObj.asString(),
                                passwordObj == null ? "" : passwordObj.asString(),
                                Juth2Helper.getPostData(request)
                        ),
                        clientIdClaim.asString()
                );
            }
        } else {//资源请求
            //logged out check
            try {
                principal = juth2Service.getPrincipal(request);
                //验证是否已经登出
                if (principal == null) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "You have been logged out"));
                    return;
                }
                client = principal.getClient();
            } catch (Exception e) {
                Juth2Log.error("getPrincipal by logged out check in level3",e);
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Sys, "Data access error"));
                return;
            }

            //是否登出请求
            boolean isSignOut = false;
            if (isAuthRequestUrl(request, Juth2Properties.SIGN_OUT_SERVICE_NAME)) {
                isSignOut = true;
                //自定义主体清除
                juth2Service.revoke(principal);
                //request中主体清除
                ((Juth2PrincipalProvider) principal).logout();
            }

            //scope检测
            if (!isSignOut && Juth2Properties.SECURITY_SCOPE_ENABLED) {
                if (!isInScope(principal.getAuthentication().getScopes().trim(), request.getRequestURI().replace(request.getContextPath(), ""))) {
                    //超限
                    response.sendError(HttpStatus.FORBIDDEN.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "You don’t have permission to access this resource"));
                    return;
                }
            }

        }

        filterChain.doFilter(new Juth2RequestWrapper(request, client, principal), response);
    }

    /**
     * 检测鉴权请求中的client信息是否匹配服务器中保存的client
     *
     * @param client
     * @param claimMap
     * @return
     */
    private boolean isClientMatch(Juth2Client client, Map<String, Claim> claimMap) {
        String[] allScopesAry = client.getClientScope().trim().replaceAll("\\s+", " ").split("\\s");
        Set allScopes = new HashSet(Arrays.asList(allScopesAry));
        //filter2中已经做过scope非空检验，此处不再处理
        String[] requestScopesAry = claimMap.get("scope").asString().trim().replaceAll("\\s+", " ").split("\\s");
        Set requestScopes = new HashSet(Arrays.asList(requestScopesAry));
        if (!allScopes.containsAll(requestScopes)) return false;

        String[] grantTypes = client.getGrantType().trim().replaceAll("\\s+", " ").split("\\s");
        String grantType = claimMap.get("grant_type").asString();
        if (!"logout".equals(grantType) && !Arrays.asList(grantTypes).contains(grantType)) return false;

        return true;
    }

    /**
     * 是否鉴权请求地址
     *
     * @param request
     * @param type    鉴权类型 logout/refresh_token/authorize，null时不检测类型
     * @return
     */
    private boolean isAuthRequestUrl(HttpServletRequest request, String type) {
        if (type == null) {
            type = "";
        }
        return request.getRequestURI().startsWith(request.getContextPath() + "/juth2/" + type);
    }

    /**
     * 检测请求url是否在已授权scope中
     *
     * @param scopes
     * @param url
     * @return
     */
    private boolean isInScope(String scopes, String url) {
        String[] scopeAry = scopes.replaceAll("\\s+", " ").split("\\s");
        for (String scope : scopeAry) {
            List<String> urls = juth2Service.getScopeUrlList(scope);
            long matched = urls.stream().filter(scopeUrl -> urlMatcher.match(scopeUrl, url)).count();
            if (matched > 0) return true;
        }
        return false;
    }
}
