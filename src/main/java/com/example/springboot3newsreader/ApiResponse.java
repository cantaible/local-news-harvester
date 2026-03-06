package com.example.springboot3newsreader;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private Integer total;

    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(int status, String message, T data, Integer total) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.total = total;
    }

    public int getStatus() {return status;}
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Integer getTotal() { return total; }
}
