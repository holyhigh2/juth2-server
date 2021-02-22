package com.github.holyhigh2.juth2server.example;

import com.github.holyhigh2.juth2server.filter.Juth2Request;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * 【样例】
 * 资源控制器
 *
 * @author holyhigh https://github.com/holyhigh2
 */
@RestController
@RequestMapping(path = "/r")
public class ResourceController {

    /**
     * 未获取token前的鉴权逻辑
     * 1. 验证client
     * 2. 生成token
     *
     * @param request
     * @param response
     * @return 返回授权码或者访问码json
     */
    @RequestMapping(path = "/show/me/the/money")
    public String showMeTheMonty(Juth2Request request, HttpServletResponse response) {
        return "999999999";
    }
}
