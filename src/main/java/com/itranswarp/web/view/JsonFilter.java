package com.itranswarp.web.view;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.itranswarp.util.JsonUtil;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class JsonFilter extends AbstractFilter {

    @Override
    public String getName() {
        return "json";
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        return JsonUtil.writeJson(input);
    }

}
