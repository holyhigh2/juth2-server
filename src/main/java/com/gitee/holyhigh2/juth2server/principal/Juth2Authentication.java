package com.gitee.holyhigh2.juth2server.principal;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 鉴权信息
 *
 * @author holyhigh https://github.com/holyhigh2
 */
@Data
public class Juth2Authentication implements Serializable {
    private static final long serialVersionUID = 7219776491450668945L;

    public Juth2Authentication(){}
    public Juth2Authentication(String grantType, String scopes, String code,
                               String username, String password,
                               Map<String,Object> extra) {
        this.grantType = grantType;
        this.scopes = scopes;
        this.code = code;
        this.username = username;
        this.password = password;
        this.extra = extra;
    }

    /**
     * 申请授权方式
     */
    private String grantType;
    /**
     * 本次申请鉴权的scopes，一旦鉴权确认后无法更改
     */
    private String scopes;

    /**
     * 授权码方式会传递code
     */
    private String code;

    /**
     * 用户名/密码方式会传递 un/pwd
     */
    private String username;
    private String password;

    /**
     * 鉴权时传递的额外信息
     */
    private Map<String,Object> extra;
}
