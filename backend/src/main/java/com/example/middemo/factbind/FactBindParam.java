package com.example.middemo.factbind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在控制器方法参数上：声明"这个参数对应契约里的哪个参数"。
 *
 * <p>名字留空时使用 Java 参数名（需要编译时开启 {@code -parameters}，Spring Boot 的 parent POM 默认开启）。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface FactBindParam {

    /** 契约里的参数名（HTTP 上看到的名字）；留空表示与 Java 参数名相同。 */
    String value() default "";
}
