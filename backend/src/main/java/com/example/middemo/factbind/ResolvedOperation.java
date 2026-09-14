package com.example.middemo.factbind;

import java.util.List;

/** 契约里的一条 operation：稳定符号 + 它当前在网络上的实现。 */
public record ResolvedOperation(
        String symbol,
        String method,
        String path,
        List<ResolvedParameter> parameters
) {

    /** 路径模板里的参数名（拼 URL 用）。 */
    public List<String> pathParams() {
        return parameters.stream().filter(ResolvedParameter::isPath).map(ResolvedParameter::name).toList();
    }

    public ResolvedParameter parameter(String name) {
        return parameters.stream()
                .filter(parameter -> parameter.name().equals(name))
                .findFirst()
                .orElseThrow(() -> FactBindException.undeclaredParameter(symbol, name));
    }
}
