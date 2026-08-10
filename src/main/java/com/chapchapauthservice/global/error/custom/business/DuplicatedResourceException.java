package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthservice.global.error.custom.BusinessException;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;

public class DuplicatedResourceException extends BusinessException {
    public DuplicatedResourceException(String message) {
        super(CustomResponseCode.DUPLICATED_RESOURCE_ERROR, message);
    }
}
