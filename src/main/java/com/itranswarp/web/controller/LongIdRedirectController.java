package com.itranswarp.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.util.IdUtil;

@RestController
public class LongIdRedirectController {

	static final String ID = "{id:[0-9a-f]{50}}";
	static final String ID2 = "{id2:[0-9a-f]{50}}";

	@GetMapping("/category/" + ID)
	public String category(@PathVariable("id") String longId) {
		return "redirect:/category/" + IdUtil.longIdToShortId(longId);
	}

	@GetMapping("/article/" + ID)
	public String article(@PathVariable("id") String longId) {
		return "redirect:/article/" + IdUtil.longIdToShortId(longId);
	}

	@GetMapping("/wiki/" + ID)
	public String wiki(@PathVariable("id") String longId) {
		return "redirect:/wiki/" + IdUtil.longIdToShortId(longId);
	}

	@GetMapping("/wiki/" + ID + "/" + ID2)
	public String category(@PathVariable("id") String longId, @PathVariable("id2") String longId2) {
		return "redirect:/wiki/" + IdUtil.longIdToShortId(longId) + "/" + IdUtil.longIdToShortId(longId2);
	}

	@GetMapping("/discuss/" + ID + "/" + ID2)
	public String discuss(@PathVariable("id") String longId, @PathVariable("id2") String longId2) {
		return "redirect:/discuss/" + IdUtil.longIdToShortId(longId) + "/" + IdUtil.longIdToShortId(longId2);
	}

	@GetMapping("/discuss/" + ID)
	public String discuss(@PathVariable("id") String longId) {
		return "redirect:/discuss/" + IdUtil.longIdToShortId(longId);
	}

}
