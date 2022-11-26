package com.itranswarp.web.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class DateTimeFilter extends AbstractFilter {

    final List<String> ARGUMENTS = Arrays.asList("format");
    final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV", Locale.US);
    final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US);
    final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);

    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();

    @Override
    public String getName() {
        return "datetime";
    }

    @Override
    public List<String> getArgumentNames() {
        return ARGUMENTS;
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        if (input == null) {
            return null;
        }
        final String format = (String) args.get("format");
        TemporalAccessor ta = null;
        DateTimeFormatter formatter = null;

        if (input instanceof Long) {
            Long n = (Long) input;
            if (n == 0) {
                return null;
            }
            ta = Instant.ofEpochMilli(n).atZone(zoneId);
            formatter = format == null ? DATETIME_FORMATTER : DateTimeFormatter.ofPattern(format, Locale.US);
        } else if (input instanceof ZonedDateTime) {
            ta = (ZonedDateTime) input;
            formatter = format == null ? DATETIME_FORMATTER : DateTimeFormatter.ofPattern(format, Locale.US);
        } else if (input instanceof LocalDateTime) {
            LocalDateTime ldt = (LocalDateTime) input;
            ta = ldt.atZone(zoneId);
            formatter = format == null ? DATETIME_FORMATTER : DateTimeFormatter.ofPattern(format, Locale.US);
        } else if (input instanceof LocalDate) {
            ta = (LocalDate) input;
            formatter = format == null ? DATE_FORMATTER : DateTimeFormatter.ofPattern(format, Locale.US);
        } else if (input instanceof LocalTime) {
            ta = (LocalTime) input;
            formatter = format == null ? TIME_FORMATTER : DateTimeFormatter.ofPattern(format, Locale.US);
        }
        if (ta == null) {
            return null;
        }
        return formatter.format(ta);
    }

}
