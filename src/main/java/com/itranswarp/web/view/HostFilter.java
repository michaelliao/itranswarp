package com.itranswarp.web.view;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class HostFilter extends AbstractFilter {

    @Override
    public String getName() {
        return "host";
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        if (input == null) {
            return "";
        }
        String s = input.toString();
        if (s.startsWith("https://")) {
            int n = s.indexOf('/', 9);
            if (n == -1) {
                return s.substring(8);
            }
            return s.substring(8, n);
        }
        if (s.startsWith("http://")) {
            int n = s.indexOf('/', 8);
            if (n == -1) {
                return s.substring(7);
            }
            return s.substring(7, n);
        }
        return "";
    }
}
