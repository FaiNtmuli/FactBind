package com.example.middemo.factbind;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * 让 {@link FactBindParam} 生效：参数的值从**契约声明的位置**读取。
 *
 * <p>这与 Spring 自己的 {@code PathVariableMethodArgumentResolver} / {@code RequestParamMethodArgumentResolver}
 * 做的是同一件事，区别只有一个："参数在 HTTP 上的名字和位置"不再来自注解，而来自契约。
 * 类型转换仍交给 Spring（{@code WebDataBinder}），所以枚举、数字的转换规则与原生注解完全一致。
 */
public class FactBindParamArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectProvider<ContractRegistry> registryProvider;

    public FactBindParamArgumentResolver(ObjectProvider<ContractRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(FactBindParam.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        FactBindParam annotation = parameter.getParameterAnnotation(FactBindParam.class);
        FactBind factBind = parameter.getMethodAnnotation(FactBind.class);
        if (factBind == null) {
            throw FactBindException.contractLoad(
                    "@FactBindParam requires @FactBind on the same method: " + parameter.getMethod());
        }

        String symbol = factBind.value();
        String name = annotation.value().isEmpty() ? parameter.getParameter().getName() : annotation.value();
        ResolvedParameter declared = registryProvider.getObject().operation(symbol).parameter(name);

        String rawValue = read(declared, symbol, webRequest);
        if (rawValue == null) {
            rawValue = declared.defaultValue();
        }
        if (rawValue == null) {
            if (declared.required()) {
                throw new MissingServletRequestParameterException(name, declared.in());
            }
            return null;
        }
        if (parameter.getParameterType() == String.class || binderFactory == null) {
            return rawValue;
        }

        WebDataBinder binder = binderFactory.createBinder(webRequest, null, name);
        try {
            return binder.convertIfNecessary(rawValue, parameter.getParameterType(), parameter);
        } catch (TypeMismatchException ex) {
            throw new MethodArgumentTypeMismatchException(rawValue, parameter.getParameterType(), name, parameter, ex);
        }
    }

    private String read(ResolvedParameter declared, String symbol, NativeWebRequest webRequest) {
        return switch (declared.in().toLowerCase()) {
            case "path" -> pathVariable(declared.name(), webRequest);
            case "query" -> webRequest.getParameter(declared.name());
            default -> throw FactBindException.unsupportedParameterLocation(symbol, declared.name(), declared.in());
        };
    }

    private String pathVariable(String name, NativeWebRequest webRequest) {
        Object attribute = webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (attribute instanceof Map<?, ?> variables && variables.get(name) instanceof String value) {
            return value;
        }
        return null;
    }
}
