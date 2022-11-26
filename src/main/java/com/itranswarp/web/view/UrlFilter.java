package com.itranswarp.web.view;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class UrlFilter extends AbstractFilter {

    @Override
    public String getName() {
        return "url";
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        if (input == null) {
            return "";
        }
        String s = input.toString();
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

}
