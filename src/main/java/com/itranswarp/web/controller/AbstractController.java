package com.itranswarp.web.controller;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.itranswarp.common.AbstractService;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.redis.RedisService;
import com.itranswarp.search.AbstractSearcher;
import com.itranswarp.service.AdService;
import com.itranswarp.service.AntiSpamService;
import com.itranswarp.service.ArticleService;
import com.itranswarp.service.AttachmentService;
import com.itranswarp.service.BoardService;
import com.itranswarp.service.EncryptService;
import com.itranswarp.service.HeadlineService;
import com.itranswarp.service.LinkService;
import com.itranswarp.service.NavigationService;
import com.itranswarp.service.SettingService;
import com.itranswarp.service.SinglePageService;
import com.itranswarp.service.TextService;
import com.itranswarp.service.UserService;
import com.itranswarp.service.WikiService;
import com.itranswarp.web.view.i18n.Translators;

public abstract class AbstractController extends AbstractService {

    protected static final String ID = "{id:[0-9]{1,17}}";
    protected static final String ID2 = "{id2:[0-9]{1,17}}";

    @Value("${spring.profiles.active:native}")
    String activeProfile;

    @Value("${spring.application.name:iTranswarp")
    protected String name;

    protected boolean dev;

    @PostConstruct
    public void initEnv() {
        this.dev = "native".equals(this.activeProfile);
        if (this.dev) {
            logger.warn("application is set to dev mode.");
        }
    }

    @Autowired
    protected EncryptService encryptService;

    @Autowired
    protected RedisService redisService;

    @Autowired
    protected AdService adService;

    @Autowired
    protected ArticleService articleService;

    @Autowired
    protected AttachmentService attachmentService;

    @Autowired
    protected BoardService boardService;

    @Autowired
    protected HeadlineService headlineService;

    @Autowired
    protected LinkService linkService;

    @Autowired
    protected NavigationService navigationService;

    @Autowired
    protected SettingService settingService;

    @Autowired
    protected SinglePageService singlePageService;

    @Autowired
    protected TextService textService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected WikiService wikiService;

    @Autowired
    protected AntiSpamService antiSpamService;

    @Autowired
    protected AbstractSearcher searcher;

    @Autowired
    protected Translators translators;

    @Autowired
    protected Markdown markdown;

}
