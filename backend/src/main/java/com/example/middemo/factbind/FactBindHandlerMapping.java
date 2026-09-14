package com.example.middemo.factbind;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 让 Spring 认识 {@link FactBind}。
 *
 * <p>Spring 建路由表的那一步本来就是"读注解 → 构造 RequestMappingInfo"（{@code getMappingForMethod}），
 * 这里只是把"读 {@code @GetMapping}"换成"读 {@code @FactBind} + 查契约"。
 * 分发、参数绑定、序列化、异常处理全部还是 Spring 原来的代码。
 *
 * <p>解析只发生在启动建表时，请求路径上没有任何额外开销。
 */
public class FactBindHandlerMapping extends RequestMappingHandlerMapping {

    private final ObjectProvider<ContractRegistry> registryProvider;

    public FactBindHandlerMapping(ObjectProvider<ContractRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        FactBind factBind = AnnotatedElementUtils.findMergedAnnotation(method, FactBind.class);
        if (factBind == null) {
            // 不是 FactBind 方法（例如 Spring Boot 自带的 /error）——交还给默认实现
            return super.getMappingForMethod(method, handlerType);
        }

        ResolvedOperation operation = registryProvider.getObject().operation(factBind.value());
        validateParameters(method, operation);
        return RequestMappingInfo
                .paths(operation.path())
                .methods(RequestMethod.valueOf(operation.method()))
                .build();
    }

    /**
     * 启动期把"契约声明的参数"与"控制器声明的参数"对齐——这是唯一能在启动时发现参数名写错的地方
     * （Spring 自己不校验参数名与路径模板是否一致）。
     */
    private void validateParameters(Method method, ResolvedOperation operation) {
        Set<String> consumed = new LinkedHashSet<>();
        for (Parameter parameter : method.getParameters()) {
            FactBindParam annotation = parameter.getAnnotation(FactBindParam.class);
            if (annotation == null) {
                continue;
            }
            String name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
            operation.parameter(name);      // 契约里没有 → 抛 undeclaredParameter
            consumed.add(name);
        }
        for (ResolvedParameter declared : operation.parameters()) {
            if (declared.isPath() && !consumed.contains(declared.name())) {
                throw FactBindException.unboundPathParameter(operation.symbol(), declared.name());
            }
        }
    }
}
