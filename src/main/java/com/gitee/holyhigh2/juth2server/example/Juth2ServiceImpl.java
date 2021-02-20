package com.gitee.holyhigh2.juth2server.example;

import com.gitee.holyhigh2.juth2server.Juth2Client;
import com.gitee.holyhigh2.juth2server.Juth2DataAccessException;
import com.gitee.holyhigh2.juth2server.Juth2Service;
import com.gitee.holyhigh2.juth2server.principal.Juth2Principal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 【样例】
 * Juth2服务实现。为Juth2提供底层数据服务，包括
 * 管理授权客户
 * 对鉴权数据进行缓存
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
@Service
public class Juth2ServiceImpl implements Juth2Service {
    Map<String,Juth2Principal> principalMap = new HashMap<>();

    /**
     * 查询所有authclient
     *
     * @return
     */
    public Map<String, Juth2Client> getAllClients() {
        Map<String, Juth2Client> clientMap = new HashMap<>();

        Juth2Client client = new Juth2Client();
        client.setClientAddress("http://localhost:8080");
        client.setClientId("juth2-client");
        client.setGrantType("password client_credentials");
        client.setResponseType("");
        client.setClientScope("a b c");
        client.setClientSecret("    ");
        clientMap.put(client.getClientId(), client);

        return clientMap;
    }

    @Override
    public Map<String, Juth2Principal> getAllPrincipals() throws Juth2DataAccessException {
        return principalMap;
    }

    @Override
    public <T> T getUser(String userId) {
        return null;
    }

    @Override
    public void revoke(Juth2Principal principal) {
        principalMap.remove(principal.getToken());
    }

    @Override
    public void record(Juth2Principal principal) {
        principalMap.put(principal.getToken(), principal);
    }

    @Override
    public List<String> getScopeUrlList(String scope) {
        List<String> urlList = new ArrayList<>();
        urlList.add("/r/**");
        return urlList;
    }
}
