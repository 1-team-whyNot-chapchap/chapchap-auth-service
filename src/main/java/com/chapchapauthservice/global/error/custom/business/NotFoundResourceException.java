package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthserivce.global.error.custom.BusinessException;
import com.chapchapauthserivce.global.response.constant.CustomResponseCode;

public class NotFoundResourceException extends BusinessException {
    public NotFoundResourceException(String message) {
        super(CustomResponseCode.NOT_FOUND_RESOURCE_ERROR, message);
    }
}
