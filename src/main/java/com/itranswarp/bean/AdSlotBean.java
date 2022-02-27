package com.itranswarp.bean;

public class AdSlotBean extends AbstractRequestBean {

    public String alias;
    public String name;
    public String description;

    public long price;
    public long width;
    public long height;

    public long numSlots;
    public long numAutoFill;
    public String adAutoFill;

    @Override
    public void validate(boolean createMode) {
        if (createMode) {
            this.alias = checkAlias(this.alias);
            checkLong("width", this.width, v -> v > 100);
            checkLong("height", this.height, v -> v > 100);
        }
        this.name = checkName(this.name);
        this.description = checkDescription(this.description);
        this.adAutoFill = checkContent(this.adAutoFill);
        checkLong("price", this.price, v -> v > 0);
        checkLong("numSlots", this.numSlots, v -> v > 0 && v <= 10);
        checkLong("numAutoFill", this.numAutoFill, v -> v >= 0 && v <= this.numSlots);
    }

}
