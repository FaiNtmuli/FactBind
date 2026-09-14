package com.example.middemo.factbind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在控制器方法上：声明"这个方法对应契约里的哪条 operation"。
 *
 * <p>代替 {@code @GetMapping("/{id}")} 这类注解——HTTP 方法与路径由契约提供。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FactBind {

    /** 契约里的 operationId，例如 {@code "User.Get"}。 */
    String value();
}
