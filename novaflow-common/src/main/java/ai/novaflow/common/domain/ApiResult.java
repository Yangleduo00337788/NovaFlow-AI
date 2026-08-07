package ai.novaflow.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "success", data, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> fail(String message) {
        return fail(40000, message);
    }
}
