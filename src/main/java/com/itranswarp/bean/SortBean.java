package com.itranswarp.bean;

import java.util.List;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;

public class SortBean extends AbstractRequestBean {

    public List<Long> ids;

    @Override
    public void validate(boolean createMode) {
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid id list.");
        }
        for (Long id : ids) {
            if (id == null || id.longValue() <= 0) {
                throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid id list.");
            }
        }
    }

}
