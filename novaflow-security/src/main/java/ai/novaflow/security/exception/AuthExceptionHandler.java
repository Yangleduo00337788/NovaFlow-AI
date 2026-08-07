package ai.novaflow.security.exception;

import ai.novaflow.common.domain.ApiResult;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleNotLogin(NotLoginException e) {
        log.warn("Unauthorized request: {}", e.getMessage());
        return ApiResult.fail(40101, "登录已过期，请重新登录");
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleForbidden(RuntimeException e) {
        log.warn("Forbidden request: {}", e.getMessage());
        return ApiResult.fail(40301, "无权限访问");
    }
}
