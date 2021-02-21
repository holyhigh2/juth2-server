package com.github.holyhigh2.juth2server.config;

import com.github.holyhigh2.juth2server.Juth2Client;
import com.github.holyhigh2.juth2server.Juth2DataAccessException;
import com.github.holyhigh2.juth2server.Juth2Log;
import com.github.holyhigh2.juth2server.Juth2Service;
import com.github.holyhigh2.juth2server.filter.FilterLevel2;
import com.github.holyhigh2.juth2server.filter.FilterLevel3;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Juth2 配置入口，负责初始化配置文件及 FilterLevel1
 *
 * @author holyhigh https://github.com/holyhigh2
 */
@Configuration
@EnableConfigurationProperties({Juth2Properties.class})
public class Juth2Configuration {
    private Juth2Service juth2Service;
    private Juth2Properties juth2Properties;

    Juth2Configuration(Juth2Service juth2Service, Juth2Properties juth2Properties, Environment environment){
        //banner
        String version = Juth2Service.class.getPackage().getImplementationVersion();
        ResourceBanner banner = new ResourceBanner(new ClassPathResource("/juth2-logo.txt"));
        banner.printBanner(environment,Juth2Configuration.class,System.out);

        this.juth2Service = juth2Service;
        this.juth2Properties = juth2Properties;

        Juth2Properties.log();
    }

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
    public FilterRegistrationBean<CorsFilter> loadFilterLevel1() {
        final FilterRegistrationBean<CorsFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(corsFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Juth2Properties.FILTER_START_NUMBER);

        Juth2Log.info("FilterLevel1 is activated");

        return reg;
    }

    @Bean
    public FilterRegistrationBean<FilterLevel2> loadFilterLevel2() {
        final FilterRegistrationBean<FilterLevel2> reg = new FilterRegistrationBean<>();

        reg.setFilter(new FilterLevel2());
        reg.addUrlPatterns("/*");
        reg.setOrder(Juth2Properties.FILTER_START_NUMBER + 1);

        Juth2Log.info("FilterLevel2 is activated");

        return reg;
    }

    @Bean
    public FilterRegistrationBean<FilterLevel3> loadFilterLevel3() {
        final FilterRegistrationBean<FilterLevel3> reg = new FilterRegistrationBean<>();

        reg.setFilter(new FilterLevel3());
        reg.addUrlPatterns("/*");
        reg.setOrder(Juth2Properties.FILTER_START_NUMBER + 2);

        Juth2Log.info("FilterLevel3 is activated");

        return reg;
    }

}
