package com.github.holyhigh2.juth2server.config;

import com.github.holyhigh2.juth2server.Juth2Log;
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


/**
 * Juth2 配置入口，负责初始化配置文件及 FilterLevel1
 *
 * @author holyhigh https://github.com/holyhigh2
 */
@Configuration
@EnableConfigurationProperties({Juth2Properties.class})
public class Juth2Configuration {

    public Juth2Configuration(Juth2Properties juth2Properties, Environment environment){
        //banner
        String version = this.getClass().getPackage().getImplementationVersion();
        ResourceBanner banner = new ResourceBanner(new ClassPathResource("/juth2-logo.txt"));
        banner.printBanner(environment,Juth2Configuration.class,System.out);
        System.out.println(" ".repeat(61) +version);

        juth2Properties.log();
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
        if(Juth2Properties.SECURITY_COOKIE_ENABLED){
            config.setAllowCredentials(true);
        }

        List<String> origins = List.of("*");
        config.setAllowedOriginPatterns(origins);

        //log日志，显示配置项
        List<String> headers = Juth2Properties.ALLOWED_HEADERS;
        List<String> methods = Juth2Properties.ALLOWED_METHODS;
        config.setAllowedHeaders(headers);
        config.setAllowedMethods(methods);
        source.registerCorsConfiguration("/**", config);

        String initInfo = String.format("Initializing CORS with \nHeaders:%s,\nMethods:%s,\nUrlPatterns:/*\n",
                headers, methods
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
