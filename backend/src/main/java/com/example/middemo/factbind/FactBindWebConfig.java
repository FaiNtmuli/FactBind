package com.example.middemo.factbind;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 通过 Spring Boot 的官方扩展点把 {@link FactBindHandlerMapping} 与
 * {@link FactBindParamArgumentResolver} 挂进 MVC。
 *
 * <p>这只是"替换了 MVC 的一个组件"，Boot 照常给这个实例套用拦截器、内容协商、CORS 等配置。
 */
@Configuration
public class FactBindWebConfig implements WebMvcRegistrations, WebMvcConfigurer {

    private final ObjectProvider<ContractRegistry> registryProvider;

    public FactBindWebConfig(ObjectProvider<ContractRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    @Bean
    public ContractRegistry contractRegistry(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${factbind.contract:classpath:contracts/api.json}") String location
    ) {
        return new ContractRegistry(objectMapper, resourceLoader, location);
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new FactBindHandlerMapping(registryProvider);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new FactBindParamArgumentResolver(registryProvider));
    }
}
