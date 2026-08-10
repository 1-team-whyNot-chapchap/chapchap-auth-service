package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthservice.global.error.custom.BusinessException;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;

public class AlreadyRegisteredException extends BusinessException {
    public AlreadyRegisteredException(String message) {
        super(CustomResponseCode.ALREADY_REGISTERED_ERROR, message);
    }
}
