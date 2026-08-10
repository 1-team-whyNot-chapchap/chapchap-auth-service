package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthserivce.global.error.custom.BusinessException;
import com.chapchapauthserivce.global.response.constant.CustomResponseCode;

public class NotRegisteredException extends BusinessException {
    public NotRegisteredException(String message) {
        super(CustomResponseCode.NOT_REGISTERED_ERROR, message);
    }
}
