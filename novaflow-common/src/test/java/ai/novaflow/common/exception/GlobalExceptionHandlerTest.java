package ai.novaflow.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void mapsAuthAndQuotaCodesToHttpStatus() {
        assertEquals(HttpStatus.UNAUTHORIZED, GlobalExceptionHandler.httpStatusOf(40101));
        assertEquals(HttpStatus.FORBIDDEN, GlobalExceptionHandler.httpStatusOf(40303));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, GlobalExceptionHandler.httpStatusOf(42901));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, GlobalExceptionHandler.httpStatusOf(42902));
        assertEquals(HttpStatus.BAD_REQUEST, GlobalExceptionHandler.httpStatusOf(40001));
        assertEquals(HttpStatus.BAD_REQUEST, GlobalExceptionHandler.httpStatusOf(40000));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, GlobalExceptionHandler.httpStatusOf(50000));
    }

    @Test
    void businessRateLimitReturnsHttp429() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleBusiness(new BusinessException(42901, "请求过于频繁，请稍后再试"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(42901, response.getBody().getCode());
        assertEquals("请求过于频繁，请稍后再试", response.getBody().getMessage());
    }
}
