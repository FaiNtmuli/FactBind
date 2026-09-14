package com.example.middemo.exception;

/**
 * Base class for every expected business failure.
 *
 * <p>只携带"哪个错误码 + 给人看的信息"。**HTTP 状态码不在这里**——它是契约里
 * {@code x-factbind-errors} 的事实，运行期由 {@link GlobalExceptionHandler} 从契约查出来。
 * 所以"把 409 改成 422"只需要改契约一处，不必碰 11 个异常类。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
