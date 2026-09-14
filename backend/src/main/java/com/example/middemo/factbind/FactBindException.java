package com.example.middemo.factbind;

/** FactBind 的所有失败都从这里抛，一律 fail fast。 */
public class FactBindException extends RuntimeException {

    private FactBindException(String message) {
        super(message);
    }

    public static FactBindException contractLoad(String detail) {
        return new FactBindException("FactBind contract error: " + detail);
    }

    public static FactBindException unknownSymbol(String symbol) {
        return new FactBindException("FactBind: no operation with symbol '" + symbol + "' in the contract");
    }

    public static FactBindException missingParameter(String symbol, String name) {
        return new FactBindException("FactBind: operation '" + symbol + "' requires parameter '" + name + "'");
    }

    public static FactBindException undeclaredParameter(String symbol, String name) {
        return new FactBindException("FactBind: controller declares parameter '" + name
                + "' but operation '" + symbol + "' does not declare it"
                + "（若 @FactBindParam 未写名字，请确认编译时开启了 -parameters）");
    }

    public static FactBindException unboundPathParameter(String symbol, String name) {
        return new FactBindException("FactBind: operation '" + symbol
                + "' declares path parameter '" + name + "' but no controller parameter consumes it");
    }

    public static FactBindException unsupportedParameterLocation(String symbol, String name, String in) {
        return new FactBindException("FactBind: parameter '" + name + "' of '" + symbol
                + "' uses unsupported location '" + in + "'");
    }

    public static FactBindException unknownErrorCode(String code) {
        return new FactBindException("FactBind: error code '" + code
                + "' is not declared in 'x-factbind-errors'");
    }
}
