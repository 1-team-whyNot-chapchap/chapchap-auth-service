package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthserivce.global.error.custom.BusinessException;
import com.chapchapauthserivce.global.response.constant.CustomResponseCode;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(CustomResponseCode.INVALID_TOKEN_ERROR, message);
    }
}
