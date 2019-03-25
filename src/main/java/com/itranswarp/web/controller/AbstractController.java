package com.itranswarp.web.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.itranswarp.service.ArticleService;
import com.itranswarp.service.AttachmentService;
import com.itranswarp.service.BoardService;
import com.itranswarp.service.EncryptService;
import com.itranswarp.service.NavigationService;
import com.itranswarp.service.SettingService;
import com.itranswarp.service.SinglePageService;
import com.itranswarp.service.TextService;
import com.itranswarp.service.UserService;
import com.itranswarp.service.WikiService;

public abstract class AbstractController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected static final String ID = "{id:[0-9]{1,17}}";
	protected static final String ID2 = "{id2:[0-9]{1,17}}";

	public static final Map<String, Boolean> API_RESULT_TRUE = Map.of("result", Boolean.TRUE);

	@Autowired
	protected EncryptService encryptService;

	@Autowired
	protected UserService userService;

	@Autowired
	protected ArticleService articleService;

	@Autowired
	protected WikiService wikiService;

	@Autowired
	protected NavigationService navigationService;

	@Autowired
	protected BoardService boardService;

	@Autowired
	protected SinglePageService singlePageService;

	@Autowired
	protected TextService textService;

	@Autowired
	protected AttachmentService attachmentService;

	@Autowired
	protected SettingService settingService;

}
