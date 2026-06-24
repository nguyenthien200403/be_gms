package com.application.gms.config.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneralResponse<T> {
    private int statusCode;
    private String msg;
    private T data;

    public static <T> GeneralResponse<T> success(T data, String msg){
        return new GeneralResponse<>(200, msg, data);
    }

    public static <T> GeneralResponse<T> error(int statusCode, String msg){
        return new GeneralResponse<>(statusCode, msg, null);
    }

}
