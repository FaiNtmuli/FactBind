package com.example.middemo.factbind;

/** 契约里声明的一个参数。 */
public record ResolvedParameter(String name, String in, boolean required, String defaultValue) {

    public boolean isPath() {
        return "path".equalsIgnoreCase(in);
    }
}
