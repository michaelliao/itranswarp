package com.itranswarp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Value("${spring.application.name:iTranswarp}")
    public String name;

    @Value("${spring.profiles.active:native}")
    public String profiles;

    @Value("${spring.application.cdn.master:}")
    public String cdnMaster;

    @Value("${spring.application.cdn.slave:}")
    public String cdnSlave;

}
