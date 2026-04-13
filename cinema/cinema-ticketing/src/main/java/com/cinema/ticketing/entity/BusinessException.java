package com.cinema.ticketing.entity;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    
   private final int code;
   private final String message;
    
    public BusinessException(int code, String message) {
       super(message);
       this.code = code;
       this.message = message;
    }
}
