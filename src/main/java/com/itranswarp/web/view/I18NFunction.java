package com.itranswarp.web.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.itranswarp.web.support.AbstractFunction;
import com.itranswarp.web.view.i18n.Translator;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class I18NFunction extends AbstractFunction {

    @Override
    public String getName() {
        return "_";
    }

    @Override
    public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        String text = (String) args.get("0");
        // has args?
        boolean hasArgs = args.containsKey("1");
        if (hasArgs) {
            List<Object> params = new ArrayList<>();
            int i = 1;
            while (args.containsKey(String.valueOf(i))) {
                params.add(args.get(String.valueOf(i)));
                i++;
            }
            Translator t = (Translator) context.getVariable("__translator__");
            return t == null ? String.format(text, params.toArray()) : t.translate(text, params.toArray());
        } else {
            Translator t = (Translator) context.getVariable("__translator__");
            return t == null ? text : t.translate(text);
        }
    }

}
