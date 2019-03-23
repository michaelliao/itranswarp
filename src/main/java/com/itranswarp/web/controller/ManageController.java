package com.itranswarp.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/manage")
public class ManageController extends AbstractController {

	@GetMapping("/categories/list")
	public ModelAndView categoryList() {
		return null;
	}

	@GetMapping("/categories/create")
	public ModelAndView categoryCreate() {
		return null;
	}
}
