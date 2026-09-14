package com.example.middemo.exception;

import com.example.middemo.factbind.ContractRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into the single error body format used by the whole API.
 *
 * <p><b>这里不再出现任何 HTTP 状态码字面量</b>：错误码 → 状态码的映射只写在契约
 * （{@code contracts/api.json} 的 {@code x-factbind-errors}）里，运行期从这个表查出来。
 * 所以"把 409 改成 422"只需要改契约一处。
 *
 * <p>同时强制"错误面"：某条接口抛了它没声明的业务错误码，立刻失败 ——
 * 契约里的错误面不是注释，是被执行的。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ContractRegistry contract;

    public GlobalExceptionHandler(ContractRegistry contract) {
        this.contract = contract;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.debug("Business error: {} - {}", ex.getCode(), ex.getMessage());
        return error(ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                fields.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        return error("VALIDATION_ERROR", "Validation failed", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));

        return error("VALIDATION_ERROR", "Validation failed", fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    fields.putIfAbsent(parameter == null ? "parameter" : parameter, String.valueOf(error.getDefaultMessage())));
        });

        return error("VALIDATION_ERROR", "Validation failed", fields);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() == null ? "the expected type" : ex.getRequiredType().getSimpleName();
        String message = "Parameter '" + ex.getName() + "' has an invalid value and cannot be converted to " + expected;
        return error("INVALID_PARAMETER", message, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error("MISSING_PARAMETER", "Required parameter '" + ex.getParameterName() + "' is missing", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error("MALFORMED_REQUEST_BODY", "Request body is missing or cannot be parsed", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return error("RESOURCE_NOT_FOUND", "No endpoint matches " + ex.getResourcePath(), null);
    }

    /**
     * 路径存在但 HTTP 方法不对（例如用 POST 打一个只支持 GET 的接口）。
     *
     * <p>必须显式处理：否则会被下面的 {@code Exception} 兜底捕获，把 405 变成 500，
     * 让"接口不存在/路径变了"这类问题变得难以诊断。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error("METHOD_NOT_ALLOWED",
                "HTTP method " + ex.getMethod() + " is not supported by this endpoint", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return error("UNSUPPORTED_MEDIA_TYPE", "Content-Type " + ex.getContentType() + " is not supported", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Database constraint violated", ex);
        return error("DATA_INTEGRITY_VIOLATION", "The request conflicts with existing data", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error("INTERNAL_ERROR", "Unexpected server error", null);
    }

    /** 状态码来自契约；契约里没有这个码就立刻失败，而不是猜一个状态码返回。 */
    private ResponseEntity<ErrorResponse> error(String code, String message, Map<String, String> fields) {
        HttpStatus status = HttpStatus.valueOf(contract.errorStatus(code));
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, fields));
    }

}
