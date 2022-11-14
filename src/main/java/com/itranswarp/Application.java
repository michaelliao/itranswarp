package com.itranswarp;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.itranswarp.warpdb.WarpDb;
import com.itranswarp.web.support.MvcConfiguration;

/**
 * Application entry.
 * 
 * @author liaoxuefeng
 */
@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration
@Import(MvcConfiguration.class)
public class Application {

    static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static final String VERSION = Application.class.getPackage().getImplementationVersion() == null ? String.valueOf(Instant.now().getEpochSecond())
            : Application.class.getPackage().getImplementationVersion();

    public static void main(String[] args) {
        logger.info("start application version {}...", VERSION);
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ZoneId createZoneId(@Value("${spring.application.timezone:}") String timezone) throws Exception {
        return timezone.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(timezone);
    }

    @Bean
    public WarpDb createWarpDb(@Autowired JdbcTemplate jdbcTemplate) throws Exception {
        WarpDb db = new WarpDb();
        db.setBasePackages(List.of(getClass().getPackageName() + ".model"));
        db.setJdbcTemplate(jdbcTemplate);
        db.init();
        return db;
    }
}
