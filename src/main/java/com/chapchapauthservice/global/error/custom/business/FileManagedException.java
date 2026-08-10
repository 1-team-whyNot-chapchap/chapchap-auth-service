package com.chapchapauthservice.global.error.custom.business;

import com.chapchapauthservice.global.error.custom.BusinessException;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;

public class FileManagedException extends BusinessException {
    public FileManagedException(String message) {
        super(CustomResponseCode.FILE_MANAGED_ERROR, message);
    }
}
