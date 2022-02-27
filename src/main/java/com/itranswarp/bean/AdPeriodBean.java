package com.itranswarp.bean;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;

public class AdPeriodBean extends AbstractRequestBean {

    public long userId;

    public long adSlotId;

    public String startAt;

    public String endAt;

    @Override
    public void validate(boolean createMode) {
        this.endAt = checkLocalDate("endAt", this.endAt);
        if (createMode) {
            checkId("userId", userId);
            checkId("adSlotId", adSlotId);
            this.startAt = checkLocalDate("startAt", this.startAt);
            if (this.endAt.compareTo(this.startAt) <= 0) {
                throw new ApiException(ApiError.PARAMETER_INVALID, "endAt", "Invalid endAt.");
            }
        }
    }
}
