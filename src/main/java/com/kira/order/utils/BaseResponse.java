package com.kira.order.utils;

import lombok.Data;

@Data
public class BaseResponse {
	
    private int status = 200;
    private String message;

    public BaseResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public BaseResponse() {
    }

}
