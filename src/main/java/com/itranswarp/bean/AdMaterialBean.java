package com.itranswarp.bean;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;

public class AdMaterialBean extends AbstractRequestBean {

    public String startAt;

    public String endAt;

    public long weight;

    public String geo;

    public String tags;

    public String url;

    /**
     * Base64 encoded image data.
     */
    public String image;

    @Override
    public void validate(boolean createMode) {
        if (createMode) {
            this.image = checkImage(this.image);
        }
        if (this.startAt == null || this.startAt.isBlank()) {
            this.startAt = "1970-01-01";
        }
        if (this.endAt == null || this.endAt.isBlank()) {
            this.endAt = "2100-01-01";
        }
        this.startAt = checkLocalDate("startAt", this.startAt);
        this.endAt = checkLocalDate("endAt", this.endAt);
        if (this.endAt.compareTo(this.startAt) <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "endAt", "Invalid endAt.");
        }
        checkLong("weight", this.weight, v -> v >= 1L && v <= 100L);
        this.geo = this.geo == null ? "" : this.geo.trim();
        this.tags = checkTags(this.tags);
        this.url = checkUrl(this.url);
    }
}
