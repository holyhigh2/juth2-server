package com.gitee.holyhigh2.juth2server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.servlet.ServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;


/**
 * 提供辅助函数
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
public abstract class Juth2Helper {
    //jwt不同请求类型的sub值
    public final static String JWT_SUB_AUTH = "authorize";
    final static String JWT_SUB_API = "Authorization";//用于访问码签名
    final static String JWT_ISSUER = "Juth2Server";
    static ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取json格式数据
     * @param servletRequest
     * @return
     */
    public static Map<String,Object> getPostData(ServletRequest servletRequest){
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = servletRequest.getReader()) {
            if (reader != null) {
                String str;
                while ((str = reader.readLine()) != null) {
                    stringBuilder.append(str);
                }
            }
        } catch (IOException e) {
            Juth2Log.error("getPostData",e);
        }
        Map<String,Object> rs = new HashMap<>();
        try {
            if(StringUtils.isNotEmpty(stringBuilder.toString())) {
                rs = mapper.readValue(stringBuilder.toString(), Map.class);
            }
        } catch (JsonProcessingException e) {
            Juth2Log.error("getPostData",e);
            rs = new HashMap<>();
        }

        return rs;
    }

    /**
     * 生成auth客户ID
     * 算法：
     * 与系统数据无关，纯随机
     *
     * @return
     */
    public static String genClientId() {
        String uuid = UUID.randomUUID().toString();
        String id = new DigestUtils(MessageDigestAlgorithms.SHA_256).digestAsHex(
                uuid + System.currentTimeMillis());
        id = Base64.encodeBase64String(id.getBytes());
        id = id.substring(5, 55);
        return id;
    }

    /**
     * 生成auth客户secret
     * 算法：
     * 1. 因为secret只是用于换取token，所以长度使用512
     * 2. 使用数据库特有（非用户可见）属性参与生成secret，这样可以对secret进行加密验证
     *
     * @return
     */
    public static String genClientSecret(String clientId, String createdTime, String createdUser) {
        String secret = new DigestUtils(MessageDigestAlgorithms.SHA_512).digestAsHex(
                clientId + createdTime + createdUser
        );
        secret = Base64.encodeBase64String(secret.getBytes());
        secret = secret.substring(10, 110);
        return secret;
    }

    /**
     * 生成访问码
     * 限制：
     * 1. 因为token要在前台展示，所以每次都需要进行单向加密验证
     * 2. 每次都不同，所以需要随机要素
     * 算法：
     * 1. SHA256生成加密串
     * 2. base64加密
     * 3.
     *
     * @param clientId
     * @return
     */
    public static String genAccessToken(String clientId) {
        String token = new DigestUtils(MessageDigestAlgorithms.SHA_256).digestAsHex(
                clientId + System.currentTimeMillis() + Math.random()
        );
        token = Base64.encodeBase64String(token.getBytes());
        token = token.substring(5, 25);
        return token;
    }

    /**
     * 生成访问码响应头字符串
     *
     * @param client
     * @param expireTimeInSec
     * @return
     * @throws JsonProcessingException
     */
    public static String[] genAccessTokenJWTString(Juth2Client client, int expireTimeInSec) {

        String refresh_token = "";
        if (!StringUtils.equals(client.getGrantType(), "client_credentials")) {
            refresh_token = genAccessToken(client.getClientId());
        }
        String accessToken = genAccessToken(client.getClientId());

        Algorithm algorithm = Algorithm.HMAC256(client.getClientSecret());
        String token = JWT.create()
                .withSubject(JWT_SUB_API)
                .withIssuer(JWT_ISSUER)
                .withAudience(client.getClientAddress())
                .withIssuedAt(new Date())
                .withExpiresAt(DateUtils.addSeconds(new Date(), expireTimeInSec))
                .withClaim("at_hash", accessToken)//访问码
                .withClaim("refresh_token", refresh_token)
                .withClaim("client_id", client.getClientId())
                .sign(algorithm);

        return StringUtils.stripAll(token, accessToken, refresh_token);
    }

    public static void main(String[] args) throws Exception {
        List<Object> x = new ArrayList<>();
    }
}
