package com.gitee.holyhigh2.juth2server.config;

import com.gitee.holyhigh2.juth2server.Juth2Client;
import com.gitee.holyhigh2.juth2server.Juth2DataAccessException;
import com.gitee.holyhigh2.juth2server.Juth2Log;
import com.gitee.holyhigh2.juth2server.Juth2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Juth2 配置入口，负责初始化配置文件及 FilterLevel1
 *
 * @author holyhigh https://gitee.com/holyhigh2
 */
@Configuration
public class Juth2Configuration {
    @Autowired
    private Juth2Service juth2Service;

    /**
     * FilterLevel1 - CORS过滤器
     * 1. 请求头key，默认 "Origin", "Content-Type", "Accept", "Authorization"
     * 2. 请求方法，默认 "GET", "POST", "PUT", "OPTIONS"
     * 3. 跨域源，来自client配置
     * @return
     */
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        List<String> origins = null;
        if (Juth2Properties.SECURITY_ORIGIN_ENABLED) {
            try {
                origins = juth2Service.getAllClients().values().stream().map(Juth2Client::getClientAddress).collect(Collectors.toList());
            } catch (Juth2DataAccessException e) {
                Juth2Log.error("getAllClients in level1",e);
            }
            config.setAllowedOrigins(origins);
        } else {
            origins = List.of("*");
            config.setAllowedOriginPatterns(origins);
        }
        //log日志，显示配置项
        List<String> headers = Juth2Properties.ALLOWED_HEADERS;
        List<String> methods = Juth2Properties.ALLOWED_METHODS;
        config.setAllowedHeaders(headers);
        config.setAllowedMethods(methods);
        source.registerCorsConfiguration("/**", config);

        String initInfo = String.format("Initializing CORS with \n\nOrigins:%s,\nHeaders:%s,\nMethods:%s,\nUrlPatterns:/*\n",
                origins, headers, methods
        );
        Juth2Log.info(initInfo);
        return new CorsFilter(source);
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> regCORSFilter(Juth2Properties juth2Properties) {//注入可以让属性读取在filter前执行
        Juth2Properties.log();
        final FilterRegistrationBean<CorsFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(corsFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Juth2Properties.FILTER_START_NUMBER);
        return reg;
    }

}
