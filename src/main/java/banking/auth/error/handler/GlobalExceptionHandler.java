package banking.auth.error.handler;

import banking.auth.error.exception.ConflictException;
import banking.auth.error.exception.ForbiddenException;
import banking.auth.error.exception.NotFoundException;
import banking.auth.error.exception.UnauthorizedException;
import banking.auth.service.publisher.SystemErrorPublisher;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final SystemErrorPublisher systemErrorPublisher;

    private Map<String, Object> buildBody(HttpStatus status, String message) {
        return Map.of("timestamp", LocalDateTime.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message);
    }

    private Map<String, Object> buildBody(HttpStatus status, String message, List<String> errors) {
        return Map.of("timestamp", LocalDateTime.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message, "errors", errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("timestamp", LocalDateTime.now().toString());
        errorBody.put("status", 400);
        errorBody.put("error", "Bad Request");
        errorBody.put("message", "Validation failed");
        errorBody.put("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of("field", fieldError.getField(), "message", fieldError.getDefaultMessage()))
                .toList());
        return ResponseEntity.badRequest().body(errorBody);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildBody(HttpStatus.UNAUTHORIZED, e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildBody(HttpStatus.FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildBody(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST,
                "Request validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleParamValidation(ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST,
                "Request validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(buildBody(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        systemErrorPublisher.publish(
                "bank-auth-service",
                "UNHANDLED",
                "Unhandled exception",
                e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildBody(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error"));
    }
}
