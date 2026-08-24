package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthservice.global.error.custom.BusinessException;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;

public class CreateOAuth2Exception extends BusinessException {
    public CreateOAuth2Exception(String message) {
        super(CustomResponseCode.OAUTH2_ERROR,message);
    }
}
