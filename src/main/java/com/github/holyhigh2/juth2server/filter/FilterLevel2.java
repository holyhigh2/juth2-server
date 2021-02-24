package com.github.holyhigh2.juth2server.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.github.holyhigh2.juth2server.*;
import com.github.holyhigh2.juth2server.config.Juth2Properties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 进行oauth2 level2 的权限过滤，包括
 * 0. 过滤origins
 * 1. jwt有效性
 * 2. 符合juth2规范
 * <p>
 * https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Authentication
 * https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Status/401
 * <p>
 * 过滤协议包括授权及资源请求；区分跨域/同域
 * 但都需要通过authorization请求头
 *
 * @author holyhigh https://github.com/holyhigh2
 */
public class FilterLevel2 implements Filter {
    Juth2Service juth2Service;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setHeader("Content-Type", "application/json;charset=UTF-8");//统一返回json格式
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if(juth2Service == null){
            juth2Service = Juth2Properties.getBean(Juth2Service.class);
        }

        //wrap response
        Juth2ResponseWrapper responseWrapper = new Juth2ResponseWrapper(response, request);

        //动态 origins 验证
        boolean isCorsRequest = CorsUtils.isCorsRequest(request);
        if (isCorsRequest && Juth2Properties.SECURITY_ORIGIN_ENABLED) {
            List<String> origins = null;
            try {
                origins = juth2Service.getAllClients().values().stream().map(Juth2Client::getClientAddress).collect(Collectors.toList());
            } catch (Juth2DataAccessException e) {
                Juth2Log.error("getAllClients in level2",e);
            }
            String requestOrigin = request.getHeader("Origin");
            boolean isMatch = false;
            if(requestOrigin != null && requestOrigin.length()>0){
                for (String allowedOrigin : origins) {
                    if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
                        isMatch = true;
                        break;
                    }
                }
            }
            if(!isMatch){
                Juth2Log.warning("Reject: '" + requestOrigin + "' origin is not allowed");
                responseWrapper.sendError(HttpStatus.FORBIDDEN.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Auth, "Invalid CORS request"));
                return;
            }

            response.setHeader("Access-Control-Allow-Origin",requestOrigin);
        }

        String authorization = request.getHeader("authorization");
        if (StringUtils.isEmpty(authorization) && Juth2Properties.SECURITY_COOKIE_ENABLED && request.getCookies() != null) {
            Optional<Cookie> v = Arrays.stream(request.getCookies()).filter(c -> "Juth2-Auth" .equals(c.getName())).findFirst();
            if (v.isPresent()) {
                authorization = "Bearer " + v.get().getValue();
            }
        }

        if (StringUtils.isEmpty(authorization)) {
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "No Authorization header was found"));
            return;
        }
        //校验协议格式
        String jwtToken = RegExUtils.removeFirst(authorization, "Bearer ");
        if (StringUtils.equals(jwtToken, authorization) || StringUtils.isEmpty(jwtToken)) {
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "Authorization header format error"));
            return;
        }
        DecodedJWT jwtObject;
        try {
            jwtObject = JWT.decode(jwtToken);
        } catch (JWTDecodeException e) {//解码失败
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "Decode error"));
            return;
        }

        //是否Juth2全信息
        String aud = jwtObject.getAudience().get(0);//不能为空
        String iss = jwtObject.getIssuer();//不能为空
        String sub = jwtObject.getSubject();//不能为空
        Claim iatClaim = jwtObject.getClaim("iat");//不能为空
        Claim expClaim = jwtObject.getClaim("exp");//不能为空
        Claim clientIdClaim = jwtObject.getClaim("client_id");//不能为空
        //at_hash / grant_type至少要出现一个
        Claim accessTokenClaim = jwtObject.getClaim("at_hash");
        Claim grantTypeClaim = jwtObject.getClaim("grant_type");
        if (aud == null
                || iss == null
                || sub == null
                || iatClaim == null
                || expClaim == null
                || clientIdClaim == null
                || (accessTokenClaim.isNull() && grantTypeClaim.isNull())
        ) {
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "Incomplete request"));
            return;
        }

        //请求类型
        Claim scopeObj = jwtObject.getClaim("scope");
        Claim stateObj = jwtObject.getClaim("state");
        /*
          是否鉴权请求，包括 鉴权 / 授权码换取访问码 / 刷新访问码
         */
        boolean isAuthRequest = Juth2Helper.JWT_SUB_AUTH.equals(sub)
                && grantTypeClaim != null
                && scopeObj != null
                && stateObj != null;

        //校验jwt签名
        String alg = jwtObject.getHeaderClaim("alg").asString();
        Juth2Client client = null;
        try {
            client = juth2Service.getAllClients().get(clientIdClaim.asString());
        } catch (Juth2DataAccessException e) {
            Juth2Log.error("getAllClients in level2", e);
            responseWrapper.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Sys, "Data access error"));
            return;
        }

        Algorithm algorithm = null;
        RSAPublicKey publicKey = null;

        try {
            if (StringUtils.equals("HS256", alg)) {
                String secret = client.getClientSecret();
                algorithm = Algorithm.HMAC256(secret);
            } else {
                publicKey = getRSAPublicKey(client.getRsaPubKey());
                algorithm = Algorithm.RSA256(publicKey, null);
            }
        } catch (Exception e) {
            Juth2Log.error("Algorithm check in level2", e);
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "Algorithm check error"));
            return;
        }

        try {
            Verification verification = JWT.require(algorithm);
            if (isAuthRequest) {//鉴权请求时可以忽略iat验证，防止请求时间晚于验证时间，比如 web
                verification = verification.ignoreIssuedAt();
            }
            JWTVerifier verifier = verification.build();
            jwtObject = verifier.verify(jwtToken);
        } catch (InvalidClaimException e) {
            //无效声明
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, e.getMessage()));
            return;
        } catch (TokenExpiredException e) {
            //访问码过期
            responseWrapper.sendError(HttpStatus.FORBIDDEN.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "The access token in the request is expired"));
            return;
        } catch (SignatureVerificationException e) {//签名验证错误
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "The signature is invalid"));
            return;
        } catch (JWTVerificationException e) {
            Juth2Log.error("verify JWT in level2", e);
            responseWrapper.sendError(HttpStatus.BAD_REQUEST.value(), Juth2Error.getErrorMessage(Juth2Error.Types.Token, "The token has been tampered"));
            return;
        }

        //msg to level3
        request.setAttribute("Juth2.jwt.claims", jwtObject.getClaims());
        request.setAttribute("Juth2.isAuthRequest", isAuthRequest);
        filterChain.doFilter(request, responseWrapper);
    }

    /**
     * 读取公钥
     *
     * @param publicKey
     * @return
     * @throws Exception
     */
    RSAPublicKey getRSAPublicKey(String publicKey)
            throws Exception {

        publicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll(System.getProperty("line.separator"), "")
                .replace("-----END PUBLIC KEY-----", "");

        byte[] keyBytes = Base64.decodeBase64(publicKey);
        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
