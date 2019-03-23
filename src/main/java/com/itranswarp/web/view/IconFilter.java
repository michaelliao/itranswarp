package com.itranswarp.web.view;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

@Component
public class IconFilter extends AbstractFilter {

	@Override
	public String getName() {
		return "icon";
	}

	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context,
			int lineNumber) throws PebbleException {
		if (input == null) {
			return null;
		}
		String icon = null;
		boolean lighten = false;
		if (input instanceof Boolean) {
			Boolean b = (Boolean) input;
			icon = b ? "check" : "times";
			lighten = !b;
		}
		if (icon == null) {
			return null;
		}
		return "<i class=\"uk-icon-" + icon + "\"" + (lighten ? " style=\"opacity: 0.2\" " : "") + "></i>";
	}

}
