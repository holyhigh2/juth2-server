package com.gitee.holyhigh2.juth2server.config;

import com.gitee.holyhigh2.juth2server.Juth2Log;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * Juth2 属性bean
 * 基于SpringBoot的默认顺序，如果application属性文件中出现了juth2配置项则会优先使用。
 * 其次使用外部属性文件 juth2.yml/yml
 *
 * @author holyhigh https://github.com/holyhigh2
 */
@Data
@Component
@ConfigurationProperties(prefix = "juth2",ignoreInvalidFields = true)
@PropertySources({
        @PropertySource(value = {"classpath:juth2.yml", "classpath:config/juth2.yml"}, factory = YamlPropertySourceFactory.class,ignoreResourceNotFound = true, encoding = "UTF-8"),
        @PropertySource(value = {"classpath:juth2.properties", "classpath:config/juth2.properties"}, ignoreResourceNotFound = true, encoding = "UTF-8")
})
public class Juth2Properties implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(Juth2Properties.applicationContext == null){
            Juth2Properties.applicationContext = applicationContext;
        }
    }
    public static <T> T getBean(Class<T> clazz){
        return Juth2Properties.applicationContext.getBean(clazz);
    }

    //////配置常量
    public static int TOKEN_ACCESS_EXPIRE_TIME_IN_SEC = 1800;//默认30分钟过期
    public static int TOKEN_REFRESH_EXPIRE_TIME_IN_SEC = 2000;
    public static final int FILTER_START_NUMBER = 1073741823;//过滤器启动序号，默认中位数
    public static boolean SECURITY_SCOPE_ENABLED = true;//进行scope检查
    public static boolean SECURITY_ORIGIN_ENABLED = true;//进行资源请求origin检查
    public static boolean SECURITY_COOKIE_ENABLED = false;//是否cookie，默认false
    public static List<String> ALLOWED_HEADERS = Arrays.asList("Origin", "Content-Type", "Accept", "Authorization");//允许发送给服务器的额外header信息
    public static List<String> ALLOWED_METHODS = Arrays.asList("GET", "POST", "PUT", "OPTIONS");//允许发送给服务器的额外methods信息
    public static String SIGN_OUT_SERVICE_NAME = "sign-out";
    public static String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

    static void log(){
        Map<String, Object> info = Map.of(
                "juth2.token.access-expire-time-in-sec", TOKEN_ACCESS_EXPIRE_TIME_IN_SEC,
                "juth2.token.refresh-expire-time-in-sec", TOKEN_REFRESH_EXPIRE_TIME_IN_SEC,
                "juth2.security.scopeEnabled", SECURITY_SCOPE_ENABLED,
                "juth2.security.originEnabled", SECURITY_ORIGIN_ENABLED,
                "juth2.security.cookieEnabled", SECURITY_COOKIE_ENABLED
        );
        StringBuffer initInfo = new StringBuffer("Config loaded as \n\n");
        info.forEach((k,v) -> {
            initInfo.append(k+":"+v+"\n");
        });
        Juth2Log.info(initInfo.toString());
    }

    private Token token = new Token();
    private Allowed allowed = new Allowed();
    private Security security = new Security();

    @Data
    class Token{//token过期时间
        private Integer accessExpireTimeInSec;
        private Integer refreshExpireTimeInSec;

        public void setAccessExpireTimeInSec(Integer accessExpireTimeInSec) {
            this.accessExpireTimeInSec = accessExpireTimeInSec;
            Juth2Properties.TOKEN_ACCESS_EXPIRE_TIME_IN_SEC = accessExpireTimeInSec;
        }

        public void setRefreshExpireTimeInSec(Integer refreshExpireTimeInSec) {
            this.refreshExpireTimeInSec = refreshExpireTimeInSec;
            Juth2Properties.TOKEN_REFRESH_EXPIRE_TIME_IN_SEC = refreshExpireTimeInSec;
        }
    }
    @Data
    class Allowed{//允许的额外头信息
        private List<String> headers;
        private List<String> methods;

        public void setHeaders(List<String> headers) {
            Set headerSet = new HashSet();
            headerSet.addAll(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
            headerSet.addAll(headers);
            this.headers = new ArrayList(headerSet);

            Juth2Properties.ALLOWED_HEADERS = this.headers;
        }

        public void setMethods(List<String> methods) {
            Set methodSet = new HashSet();
            methodSet.addAll(Arrays.asList("GET", "POST", "PUT", "OPTIONS"));
            methodSet.addAll(methods);
            this.methods = new ArrayList(methodSet);

            Juth2Properties.ALLOWED_METHODS = this.methods;
        }
    }
    @Data
    class Security{//安全检查配置
        private Boolean cookieEnabled = false;
        private Boolean scopeEnabled = true;
        private Boolean originEnabled = true;

        public void setCookieEnabled(Boolean cookieEnabled) {
            this.cookieEnabled = cookieEnabled;
            Juth2Properties.SECURITY_COOKIE_ENABLED = cookieEnabled;
        }

        public void setOriginEnabled(Boolean originEnabled) {
            this.originEnabled = originEnabled;
            Juth2Properties.SECURITY_ORIGIN_ENABLED = originEnabled;
        }

        public void setScopeEnabled(Boolean scopeEnabled) {
            this.scopeEnabled = scopeEnabled;
            Juth2Properties.SECURITY_SCOPE_ENABLED = scopeEnabled;
        }
    }
}
