package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthservice.global.error.custom.BusinessException;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;

public class NotRegisteredException extends BusinessException {
    public NotRegisteredException(String message) {
        super(CustomResponseCode.NOT_REGISTERED_ERROR, message);
    }
}
