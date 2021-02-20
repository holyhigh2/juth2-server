package com.gitee.holyhigh2.juth2server.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitee.holyhigh2.juth2server.Juth2DataAccessException;
import com.gitee.holyhigh2.juth2server.filter.Juth2Request;
import com.gitee.holyhigh2.juth2server.principal.Juth2Authentication;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 【样例】
 * 鉴权逻辑控制器
 *
 * @author holyhigh https://github.com/holyhigh2
 */
@RestController
@RequestMapping(path = "/juth2")
public class Juth2Controller {
    ObjectMapper mapper = new ObjectMapper();

    /**
     * 未获取token前的鉴权逻辑
     * 1. 验证client
     * 2. 生成token
     *
     * @param request
     * @param response
     * @return 返回授权码或者访问码json
     */
    @SneakyThrows
    @RequestMapping(path = "/authorize")
    public String authorize(Juth2Request request, HttpServletResponse response) {
        Juth2Authentication authentication = request.getPrincipal().getAuthentication();
        String rs = "";//授权码 / 访问码
        Map<String, Object> json = new HashMap<>();
        String uid = "juth2-tester";

        switch (authentication.getGrantType()) {
            case "password":
                Map<String,Object> extra = request.getPrincipal().getAuthentication().getExtra();
                request.getPrincipal().signIn(response, uid);
                break;
            case "client_credentials":
                request.getPrincipal().signIn(response, uid);
                break;
        }

        json.put("uid",uid);
        rs = mapper.writeValueAsString(json);
        return rs;
    }

    /**
     * 进行token获取（授权码模式）或token刷新
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(path = "/token")
    public String token(HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        return token;
    }

    /**
     * 刷新访问token
     * 路由到该方法时，表示合法验证已经完成。
     * 是否允许刷新有业务自行决定
     *
     * @param request
     * @param response
     * @return
     */
    @SneakyThrows
    @RequestMapping(path = "/refresh-token")
    public String refreshToken(Juth2Request request, HttpServletResponse response) {
        System.out.println(request);
        try {
            request.getPrincipal().refreshToken(response);
        } catch (Juth2DataAccessException e) {
            e.printStackTrace();
        }
        Map<String, Object> rs = Map.of("code", 0);
        return mapper.writeValueAsString(rs);
    }

    @SneakyThrows
    @RequestMapping(path = "/sign-out")
    public String logout(Juth2Request request, HttpServletResponse response) {
        System.out.println(request);
        //删除其他业务数据。。。
        Map<String, Object> rs = Map.of("code", 0);
        return mapper.writeValueAsString(rs);
    }
}
