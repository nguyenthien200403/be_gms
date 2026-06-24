package com.application.gms.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.application.gms.config.exception.ResourceNotFoundException.class)
    public ResponseEntity<GeneralResponse<Object>> handleNotFound(com.application.gms.config.exception.ResourceNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GeneralResponse.error(
                    404,
                    ex.getMessage()
                )
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<GeneralResponse<Object>> handleBadRequest(BadRequestException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GeneralResponse.error(
                    400,
                    ex.getMessage()
                )
        );
    }


    @ExceptionHandler(DataDuplicateException.class)
    public ResponseEntity<GeneralResponse<Object>> handleDataDuplicate(DataDuplicateException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GeneralResponse.error(
                    409,
                    ex.getMessage()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {


        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMsg = (fieldError != null) ? fieldError.getDefaultMessage() : "Invalid input data";


        GeneralResponse<Object> response = GeneralResponse.error(
                HttpStatus.BAD_REQUEST.value(), // 400
                "Input data is not valid: " + errorMsg
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    public ResponseEntity<GeneralResponse<Object>> handleGlobalException(Exception ex){
        ex.printStackTrace();
        GeneralResponse<Object> response = GeneralResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "A serious system error has occurred, Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(response);
    }
}
