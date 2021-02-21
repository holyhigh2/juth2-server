package com.github.holyhigh2.juth2server.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * yml解析器
 *
 * @author holyhigh https://github.com/holyhigh2
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());

        String propertyName = name != null ? name : resource.getResource().getFilename();
        Properties ymlProperties = null;
        try {
            factory.afterPropertiesSet();
            ymlProperties = factory.getObject();
        } catch (Exception e) {//文件丢失
        }
        return new PropertiesPropertySource(propertyName, ymlProperties);
    }
}
