package com.itranswarp.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.itranswarp.util.IdUtil;

@Controller
public class LongIdRedirectController {

    private static final String STR_ID = "{id:[0-9a-f]{50}}";
    private static final String STR_ID2 = "{id2:[0-9a-f]{50}}";

    @GetMapping("/category/" + STR_ID)
    public String category(@PathVariable("id") String stringId) {
        return "redirect:/category/" + IdUtil.stringIdToLongId(stringId);
    }

    @GetMapping("/article/" + STR_ID)
    public String article(@PathVariable("id") String stringId) {
        return "redirect:/article/" + IdUtil.stringIdToLongId(stringId);
    }

    @GetMapping("/wiki/" + STR_ID)
    public String wiki(@PathVariable("id") String stringId) {
        return "redirect:/wiki/" + IdUtil.stringIdToLongId(stringId);
    }

    @GetMapping("/wiki/" + STR_ID + "/" + STR_ID2)
    public String category(@PathVariable("id") String stringId, @PathVariable("id2") String stringId2) {
        return "redirect:/wiki/" + IdUtil.stringIdToLongId(stringId) + "/" + IdUtil.stringIdToLongId(stringId2);
    }

    @GetMapping("/discuss/" + STR_ID + "/" + STR_ID2)
    public String discuss(@PathVariable("id") String stringId, @PathVariable("id2") String stringId2) {
        return "redirect:/discuss/" + IdUtil.stringIdToLongId(stringId) + "/" + IdUtil.stringIdToLongId(stringId2);
    }

    @GetMapping("/discuss/" + STR_ID)
    public String discuss(@PathVariable("id") String stringId) {
        return "redirect:/discuss/" + IdUtil.stringIdToLongId(stringId);
    }

    @GetMapping("/files/attachments/" + STR_ID)
    public String attachments(@PathVariable("id") String stringId) {
        return "redirect:/files/attachments/" + IdUtil.stringIdToLongId(stringId);
    }

    @GetMapping("/files/attachments/" + STR_ID + "/0")
    public String attachmentsOf0(@PathVariable("id") String stringId) {
        return "redirect:/files/attachments/" + IdUtil.stringIdToLongId(stringId) + "/0";
    }

    @GetMapping("/files/attachments/" + STR_ID + "/l")
    public String attachmentsOfL(@PathVariable("id") String stringId) {
        return "redirect:/files/attachments/" + IdUtil.stringIdToLongId(stringId) + "/l";
    }

    @GetMapping("/files/attachments/" + STR_ID + "/m")
    public String attachmentsOfM(@PathVariable("id") String stringId) {
        return "redirect:/files/attachments/" + IdUtil.stringIdToLongId(stringId) + "/m";
    }

    @GetMapping("/files/attachments/" + STR_ID + "/s")
    public String attachmentsOfS(@PathVariable("id") String stringId) {
        return "redirect:/files/attachments/" + IdUtil.stringIdToLongId(stringId) + "/s";
    }

}
