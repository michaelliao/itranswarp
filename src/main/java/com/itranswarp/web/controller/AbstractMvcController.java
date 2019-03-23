package com.itranswarp.web.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.view.i18n.Translators;

public abstract class AbstractMvcController extends AbstractController {

	@Value("#{applicationConfiguration.name}")
	String name;

	@Value("#{applicationConfiguration.cdn}")
	String cdn;

	@Value("#{applicationConfiguration.profiles eq 'native'}")
	Boolean dev;

	@Autowired
	Translators translators;

	@Autowired
	LocaleResolver localeResolver;

	ModelAndView prepareModelAndView(String view) {
		ModelAndView mv = new ModelAndView(view);
		appendGlobalModel(mv);
		return mv;
	}

	ModelAndView prepareModelAndView(String view, Map<String, Object> model) {
		ModelAndView mv = new ModelAndView(view, model);
		appendGlobalModel(mv);
		return mv;
	}

	ModelAndView notFound() {
		ModelAndView mv = new ModelAndView("/404.html");
		appendGlobalModel(mv);
		return mv;
	}

	private void appendGlobalModel(ModelAndView mv) {
		@SuppressWarnings("resource")
		HttpContext ctx = HttpContext.getContext();
		mv.addObject("__name__", this.name);
		mv.addObject("__cdn__", this.cdn);
		mv.addObject("__dev__", this.dev);
		mv.addObject("__website__", settingService.getWebsiteFromCache());
		mv.addObject("__user__", ctx.user);
		mv.addObject("__navigations__", navigationService.getNavigationsFromCache());
		mv.addObject("__timestamp__", ctx.timestamp);
		mv.addObject("__translator__", translators.getTranslator(localeResolver.resolveLocale(ctx.request)));
		ctx.response.setHeader("X-Execution-Time", String.valueOf(System.currentTimeMillis() - ctx.timestamp));
	}

}
