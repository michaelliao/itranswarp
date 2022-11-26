package com.itranswarp.web.view;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class JoinFilter extends AbstractFilter {

    @Override
    public String getName() {
        return "join";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        if (input == null) {
            return null;
        }
        String sep = (String) args.get("0");
        if (sep == null) {
            sep = " ";
        }
        if (input instanceof String[]) {
            String[] ss = (String[]) input;
            return String.join(sep, ss);
        }
        return String.join(sep, (Iterable<? extends CharSequence>) input);
    }

}
