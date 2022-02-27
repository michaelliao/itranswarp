package com.itranswarp.enums;

public enum Role {

    ADMIN(0),

    EDITOR(10),

    CONTRIBUTOR(100),

    SPONSOR(1_000),

    SUBSCRIBER(10_000);

    public final int value;

    Role(int value) {
        this.value = value;
    }

}
