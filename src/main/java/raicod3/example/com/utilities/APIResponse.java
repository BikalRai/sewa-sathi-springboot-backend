package raicod3.example.com.utilities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.enums.ErrorCode;


@Getter
@Setter
public class APIResponse {

    private boolean success;
    private String message;
    private Object data;
    private Object error;
    private Long timestamp;
    private int statusCode;
    private ErrorCode errorCode;

    private APIResponse(boolean success, String message, Object error,  int statusCode) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
        this.statusCode = statusCode;
    }

    private APIResponse(boolean success, String message, Object data, Object error, int statusCode, ErrorCode errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public static APIResponse success( String message, int statusCode) {
        return new APIResponse(true, message, null, statusCode );
    }

    public static APIResponse success(Object data, String message, int statusCode) {
        return new APIResponse(true, message, data, null, statusCode , null);
    }

    public static APIResponse error(String message, int statusCode, Object error, ErrorCode errorCode) {
        return new APIResponse(false, message, null, error, statusCode, errorCode);
    }

    public static APIResponse paginate(String message, PaginationData data, int statusCode) {
        return new APIResponse(true, message, data, null, statusCode , null);
    }

}
