package com.gitee.holyhigh2.juth2server;

import lombok.Data;

import java.io.Serializable;

/**
 * 鉴权客户
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
@Data
public class Juth2Client implements Serializable {
    private String id;

    private String clientName;

    private String clientId;

    private String clientSecret;

    private String clientScope;

    private String grantType;

    private String redirectUri;

    private String responseType;

    private String clientAddress;

    private String rsaPubKey;
}
