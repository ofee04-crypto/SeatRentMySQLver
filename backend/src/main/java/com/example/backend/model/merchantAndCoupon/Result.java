package com.example.backend.model.merchantAndCoupon;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;    // 200:成功, 500:失敗
    private String message;  // 提示訊息
    private T data;          // 實際資料內容

    public static <T> Result<T> success(T data, String msg) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.data = data;
        r.message = msg;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.message = msg;
        return r;
    }
}