package com.itranswarp.web.controller;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import com.itranswarp.Application;
import com.itranswarp.bean.setting.Website;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.RefType;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AdMaterial;
import com.itranswarp.model.AdPeriod;
import com.itranswarp.model.AdSlot;
import com.itranswarp.model.Article;
import com.itranswarp.model.Board;
import com.itranswarp.model.Category;
import com.itranswarp.model.EthAuth;
import com.itranswarp.model.Headline;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.OAuth;
import com.itranswarp.model.Reply;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.oauth.OAuthAuthentication;
import com.itranswarp.oauth.OAuthProviders;
import com.itranswarp.oauth.provider.AbstractOAuthProvider;
import com.itranswarp.search.Hits;
import com.itranswarp.service.ViewService;
import com.itranswarp.util.CookieUtil;
import com.itranswarp.util.HashUtil;
import com.itranswarp.util.HttpUtil;
import com.itranswarp.warpdb.PagedResults;
import com.itranswarp.web.filter.HttpContext;

/**
 * Mvc controller for all page views.
 * 
 * @author liaoxuefeng
 */
@Controller
public class MvcController extends AbstractController {

    @Value("${spring.signin.password.enabled}")
    boolean passauthEnabled;

    @Value("${spring.signin.eth.enabled}")
    boolean ethauthEnabled;

    @Autowired
    LocaleResolver localeResolver;

    @Autowired
    OAuthProviders oauthProviders;

    @Autowired
    ViewService viewService;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // index
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/")
    public ModelAndView index() {
        List<Article> recentArticles = this.articleService.getPublishedArticles(10);
        List<Topic> recentTopics = this.boardService.getRecentTopicsFromCache();
        return prepareModelAndView("index.html", Map.of("recentArticles", recentArticles, "recentTopics", recentTopics));
    }

    @GetMapping("/locale/{lo}")
    public String locale(@PathVariable("lo") String lo, HttpServletRequest request, HttpServletResponse response) {
        String language = lo;
        String country = "";
        int n = lo.indexOf('_');
        if (n > 0) {
            language = lo.substring(0, n);
            country = lo.substring(n + 1);
        }
        this.localeResolver.setLocale(request, response, new Locale(language, country));
        return "redirect:" + HttpUtil.getReferer(request);
    }

    @GetMapping("/search")
    public ModelAndView search(@RequestParam(value = "q", defaultValue = "") String q, HttpServletResponse response) throws Exception {
        if (!this.searcher.ready()) {
            throw new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, "Search is not enabled.");
        }
        q = q.strip();
        if (q.isEmpty()) {
            return prepareModelAndView("search.html", Map.of("q", q, "qs", List.of(), "hits", Hits.empty(), "time", "0"));
        }
        long startTime = System.currentTimeMillis();
        Hits hits = Hits.empty();
        List<String> qs = this.searcher.parseQuery(q);
        if (qs == null) {
            qs = List.of();
        }
        if (!qs.isEmpty()) {
            logger.info("search keywords: {} -> {}", q, String.join(", ", qs));
            hits = this.searcher.search(qs, 25);
        }
        return prepareModelAndView("search.html",
                Map.of("q", q, "qs", qs, "hits", hits, "time", String.format("%.3f", (System.currentTimeMillis() - startTime) / 1000.0)));
    }

    @GetMapping("/ref/{refType}/" + ID)
    public ModelAndView refRedirect(@PathVariable("refType") RefType refType, @PathVariable("id") long id, HttpServletResponse response) {
        RedirectView rv = new RedirectView();
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        switch (refType) {
        case ARTICLE:
            rv.setUrl("/article/" + id);
            return new ModelAndView(rv);
        case WIKI:
            rv.setUrl("/wiki/" + id);
            return new ModelAndView(rv);
        case WIKIPAGE:
            WikiPage wikiPage = wikiService.getWikiPageById(id);
            rv.setUrl("/wiki/" + wikiPage.wikiId + "/" + id);
            return new ModelAndView(rv);
        case NONE:
        default:
        }
        return notFound();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // headline
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/headline")
    public ModelAndView headlines(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        PagedResults<Headline> pr = headlineService.getPublishedHeadlines(pageIndex);
        List<Headline> headlines = pr.getResults();
        return prepareModelAndView("headline.html", Map.of("page", pr.page, "headlines", headlines));
    }

    @GetMapping("/headline/create")
    public ModelAndView headlineCreate() {
        return prepareModelAndView("headline_form.html");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ad
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/sponsor/adperiod")
    public ModelAndView adperiods(@RequestParam(value = "id", defaultValue = "0") long id) {
        User user = HttpContext.getRequiredCurrentUser();
        if (user.role != Role.SPONSOR) {
            throw new ApiException(ApiError.PERMISSION_DENIED, null, "Permission denied.");
        }
        List<AdPeriod> adPeriods = this.adService.getAdPeriodsByUser(user);
        List<AdMaterial> adMaterials = List.of();
        final long adPeriodId = id == 0 && !adPeriods.isEmpty() ? adPeriods.get(0).id : id;
        if (adPeriodId != 0) {
            Optional<AdPeriod> opt = adPeriods.stream().filter(p -> p.id == adPeriodId).findFirst();
            if (opt.isEmpty()) {
                throw new ApiException(ApiError.ENTITY_NOT_FOUND, "AdPeriod", "AdPeriod not found.");
            }
            AdPeriod active = opt.get();
            if (active.userId != user.id) {
                throw new ApiException(ApiError.ENTITY_NOT_FOUND, "AdPeriod", "AdPeriod not found.");
            }
            adMaterials = this.adService.getAdMaterialsByAdPeriod(active);
        }
        List<AdSlot> adSlots = this.adService.getAdSlots();
        return prepareModelAndView("sponsor.html",
                Map.of("adSlots", adSlots, "adPeriods", adPeriods, "adMaterials", adMaterials, "today", LocalDate.now().toString(), "id", adPeriodId));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // category and article
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/category/" + ID)
    public ModelAndView category(@PathVariable("id") long id, @RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        Category category = articleService.getCategoryFromCache(id);
        PagedResults<Article> pr = articleService.getPublishedArticles(category, pageIndex);
        List<Article> articles = pr.getResults();
        if (!articles.isEmpty()) {
            long[] views = this.viewService.getViews(articles.stream().map(a -> a.id).toArray());
            int n = 0;
            for (Article article : articles) {
                article.views += views[n];
                n++;
            }
        }
        return prepareModelAndView("category.html", Map.of("category", category, "page", pr.page, "articles", articles));
    }

    @GetMapping("/article/" + ID)
    public ModelAndView article(@PathVariable("id") long id) {
        Article article = articleService.getPublishedById(id);
        article.views += viewService.increaseArticleViews(id);
        Category category = articleService.getCategoryFromCache(article.categoryId);
        User author = userService.getUserFromCache(article.userId);
        String content = textService.getHtmlFromCache(article.textId);
        return prepareModelAndView("article.html", Map.of("article", article, "author", author, "category", category, "content", content));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // link
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/link/" + ID)
    public ModelAndView link(@PathVariable("id") long id, HttpServletResponse response) throws Exception {
        String url = linkService.getLinkUrlFromCache(id);
        if (url == null) {
            return notFound();
        }
        response.sendRedirect(url);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // wiki
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/wiki/" + ID)
    public ModelAndView wiki(@PathVariable("id") long id) {
        Wiki wiki = wikiService.getWikiTreeFromCache(id);
        if (wiki.publishAt > System.currentTimeMillis()) {
            User user = HttpContext.getCurrentUser();
            if (user == null || user.role.value > Role.CONTRIBUTOR.value) {
                throw new ApiException(ApiError.ENTITY_NOT_FOUND, "wiki", "Wiki not found");
            }
        }
        wiki.views += viewService.increaseWikiViews(id);
        String content = textService.getHtmlFromCache(wiki.textId);
        return prepareModelAndView("wiki.html", Map.of("wiki", wiki, "current", wiki, "content", content));
    }

    @GetMapping("/wiki/" + ID + "/" + ID2)
    public ModelAndView wikiPage(@PathVariable("id") long id, @PathVariable("id2") long pid) {
        Wiki wiki = wikiService.getWikiTreeFromCache(id);
        if (wiki.publishAt > System.currentTimeMillis()) {
            User user = HttpContext.getCurrentUser();
            if (user == null || user.role.value > Role.CONTRIBUTOR.value) {
                throw new ApiException(ApiError.ENTITY_NOT_FOUND, "wiki", "Wiki not found");
            }
        }
        WikiPage wikiPage = wikiService.getWikiPageById(pid);
        if (wikiPage.wikiId != wiki.id) {
            return notFound();
        }
        if (wikiPage.publishAt > System.currentTimeMillis()) {
            User user = HttpContext.getCurrentUser();
            if (user == null || user.role.value > Role.CONTRIBUTOR.value) {
                return notFound();
            }
        }
        wikiPage.views += viewService.increaseWikiPageViews(pid);
        String content = textService.getHtmlFromCache(wikiPage.textId);
        return prepareModelAndView("wiki.html", Map.of("wiki", wiki, "current", wikiPage, "content", content));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // discuss
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/discuss")
    public ModelAndView discuss() {
        List<Board> boards = boardService.getBoards();
        List<Topic> recentTopics = this.boardService.getRecentTopicsFromCache();
        return prepareModelAndView("discuss.html", Map.of("boards", boards, "recentTopics", recentTopics));
    }

    @GetMapping("/discuss/" + ID)
    public ModelAndView board(@PathVariable("id") long id, @RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        Board board = boardService.getBoardFromCache(id);
        PagedResults<Topic> pr = boardService.getTopics(board, pageIndex);
        return prepareModelAndView("board.html", Map.of("board", board, "page", pr.page, "topics", pr.results));
    }

    @GetMapping("/discuss/" + ID + "/" + ID2)
    public ModelAndView topic(@PathVariable("id") long id, @PathVariable("id2") long tid, @RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        Board board = boardService.getBoardFromCache(id);
        Topic topic = boardService.getTopicById(tid);
        if (topic.boardId != board.id) {
            return notFound();
        }
        PagedResults<Reply> pr = boardService.getReplies(topic, pageIndex);
        return prepareModelAndView("topic.html", Map.of("board", board, "topic", topic, "page", pr.page, "replies", pr.results));
    }

    @GetMapping("/discuss/" + ID + "/topics/create")
    public ModelAndView discussNewTopic(@PathVariable("id") long id) {
        Board board = boardService.getBoardFromCache(id);
        return prepareModelAndView("topic_form.html", Map.of("board", board));
    }

    @GetMapping("/discuss/topic/" + ID + "/find/" + ID2)
    public ModelAndView discussFindReplyInTopic(@PathVariable("id") long topicId, @PathVariable("id2") long replyId) {
        Reply reply = boardService.getReplyById(replyId);
        if (reply.topicId != topicId) {
            return notFound();
        }
        Topic topic = boardService.getTopicById(topicId);
        int pageIndex = boardService.getReplyPageIndex(topicId, replyId);
        return new ModelAndView("redirect:/discuss/" + topic.boardId + "/" + topicId + "?page=" + pageIndex + "#" + replyId);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // single page
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/single/" + ID)
    public ModelAndView singlePage(@PathVariable("id") long id) {
        SinglePage singlePage = singlePageService.getPublishedById(id);
        String content = textService.getHtmlFromCache(singlePage.textId);
        return prepareModelAndView("single.html", Map.of("singlePage", singlePage, "content", content));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // user and profile
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/user/" + ID)
    public ModelAndView user(@PathVariable("id") long id) {
        User user = userService.getById(id);
        List<Topic> topics = boardService.getTopicsByUser(user.id);
        return prepareModelAndView("profile.html", Map.of("user", user, "topics", topics));
    }

    @GetMapping("/profile")
    public ModelAndView profile() {
        User user = HttpContext.getRequiredCurrentUser();
        List<Topic> topics = boardService.getTopicsByUser(user.id);
        return prepareModelAndView("profile.html", Map.of("user", user, "topics", topics));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // sign in
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/auth/signin")
    public ModelAndView signin(@RequestParam(value = "type", defaultValue = "") String type, HttpServletRequest request) {
        boolean oauthEnabled = !this.oauthProviders.getOAuthProviders().isEmpty();
        boolean passauthEnabled = this.passauthEnabled;
        if (!oauthEnabled && !passauthEnabled) {
            throw new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, "Invalid signin configuration.");
        }
        if (!oauthEnabled && type.equals("oauth")) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "type", "Do not support OAuth signin.");
        }
        if (!passauthEnabled && type.equals("passauth")) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "type", "Do not support password signin.");
        }
        if (type.isEmpty() && oauthEnabled) {
            type = "oauth";
        }
        if (type.isEmpty() && passauthEnabled) {
            type = "passauth";
        }
        if (type.isEmpty() && ethauthEnabled) {
            type = "ethauth";
        }
        return prepareModelAndView("signin.html", Map.of("type", type, "oauthEnabled", oauthEnabled, "passauthEnabled", passauthEnabled, "ethauthEnabled",
                ethauthEnabled, "oauthConfigurations", this.oauthProviders.getOAuthConfigurations()));
    }

    @PostMapping("/auth/signin/local")
    public ModelAndView signinLocal(@RequestParam("email") String email, @RequestParam("passwd") String password, HttpServletRequest request,
            HttpServletResponse response) {
        if (!this.passauthEnabled) {
            throw new ApiException(ApiError.OPERATION_FAILED, null, "Password auth is disabled.");
        }
        if (password.length() != 64) {
            return passwordAuthFailed();
        }
        // try find user by email:
        email = email.strip().toLowerCase();
        User user = userService.fetchUserByEmail(email);
        if (user == null || user.lockedUntil > System.currentTimeMillis()) {
            return passwordAuthFailed();
        }
        // try find local auth by userId:
        LocalAuth auth = userService.fetchLocalAuthByUserId(user.id);
        if (auth == null) {
            return passwordAuthFailed();
        }
        // validate password:
        String expectedPassword = HashUtil.hmacSha256(password, auth.salt);
        if (!expectedPassword.equals(auth.passwd)) {
            return passwordAuthFailed();
        }
        // set cookie:
        String cookieStr = CookieUtil.encodeSessionCookie(auth, System.currentTimeMillis() + LOCAL_EXPIRES_IN_MILLIS, encryptService.getSessionHmacKey());
        CookieUtil.setSessionCookie(request, response, cookieStr, LOCAL_EXPIRES_IN_SECONDS);
        return new ModelAndView("redirect:" + HttpUtil.getReferer(request));
    }

    private ModelAndView passwordAuthFailed() {
        boolean oauthEnabled = !this.oauthProviders.getOAuthProviders().isEmpty();
        return prepareModelAndView("signin.html", Map.of("type", "passauth", "oauthEnabled", oauthEnabled, "passauthEnabled", passauthEnabled, "ethauthEnabled",
                ethauthEnabled, "oauthConfigurations", this.oauthProviders.getOAuthConfigurations(), "error", Boolean.TRUE));
    }

    @PostMapping("/auth/signin/eth")
    public ModelAndView signinByEth(@RequestParam("message") String message, @RequestParam("signature") String signature, HttpServletRequest request,
            HttpServletResponse response) {
        if (signature.length() != 132) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin by Web3 failed.");
        }
        int n = message.indexOf("\r\n");
        if (n == -1) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin by Web3 failed.");
        }
        String line1 = message.substring(0, n);
        if (!line1.equals("Signin: " + request.getServerName())) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin by Web3 failed.");
        }
        String expiresStr = message.substring(n + 2);
        if (!expiresStr.startsWith("Expires: ")) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin by Web3 failed.");
        }
        long expires = ZonedDateTime.parse(expiresStr.substring(9)).toEpochSecond() * 1000;
        if (expires - 3600000 < System.currentTimeMillis()) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin by Web3 failed.");
        }
        // check signature:
        byte[] sign = Numeric.hexStringToByteArray(signature);
        if (sign.length != 65) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin by Web3 failed.");
        }
        byte[] r = Arrays.copyOfRange(sign, 0, 32);
        byte[] s = Arrays.copyOfRange(sign, 32, 64);
        byte[] v = Arrays.copyOfRange(sign, 64, 65);
        SignatureData signatureData = new SignatureData(v, r, s);
        BigInteger pubKey = null;
        try {
            pubKey = Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8), signatureData);
        } catch (SignatureException e) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Invalid signature.");
        }
        String address = Keys.getAddress(pubKey);
        if (!address.startsWith("0x")) {
            address = "0x" + address;
        }
        logger.info("recover address: {}", address);
        EthAuth ethAuth = this.userService.getEthAuth(address, expires);
        // set cookie:
        String cookieStr = CookieUtil.encodeSessionCookie(ethAuth, expires, encryptService.getSessionHmacKey());
        CookieUtil.setSessionCookie(request, response, cookieStr, (int) ((expires - System.currentTimeMillis()) / 1000));
        return new ModelAndView("redirect:" + HttpUtil.getReferer(request));
    }

    @GetMapping("/auth/from/{authProviderId}")
    public String oauthFrom(@PathVariable("authProviderId") String authProviderId, HttpServletRequest request) {
        AbstractOAuthProvider provider = this.oauthProviders.getOAuthProvider(authProviderId);
        String url = HttpUtil.getScheme(request) + "://" + request.getServerName() + "/auth/callback/" + authProviderId;
        return "redirect:" + provider.getAuthenticateUrl(url);
    }

    @GetMapping("/auth/callback/{authProviderId}")
    public String oauthCallback(@PathVariable("authProviderId") String authProviderId, @RequestParam(value = "state", defaultValue = "") String state,
            @RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AbstractOAuthProvider provider = this.oauthProviders.getOAuthProvider(authProviderId);
        String url = HttpUtil.getScheme(request) + "://" + request.getServerName() + "/auth/callback/" + authProviderId;
        OAuthAuthentication authentication = null;
        try {
            authentication = provider.getAuthentication(code, state, url);
        } catch (Exception e) {
            logger.error("OAuth failed.", e);
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin from OAuth failed.");
        }
        logger.info("oauth ok from {}: {}", authProviderId, authentication.getAuthenticationId());
        OAuth auth = this.userService.getOAuth(authProviderId, provider.getOAuthConfiguration().isIgnoreImage(), authentication);
        User user = this.userService.getEnabledUserById(auth.userId);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is locked.");
            return null;
        }
        if (!auth.isNew) {
            // update user name and image:
            user.name = authentication.getName();
            user.imageUrl = this.userService.getOAuthImageUrl(provider.getOAuthConfiguration().isIgnoreImage(), authentication);
            user.updatedAt = System.currentTimeMillis();
            user.version++;
            userService.updateUserProfile(user);
        }
        String cookieStr = CookieUtil.encodeSessionCookie(auth, encryptService.getSessionHmacKey());
        CookieUtil.setSessionCookie(request, response, cookieStr, (int) authentication.getExpires().toSeconds());
        return "redirect:/";
    }

    @GetMapping("/auth/signout")
    public String signOut(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteSessionCookie(request, response);
        return "redirect:" + HttpUtil.getReferer(request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // exception handler
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @ExceptionHandler(ApiException.class)
    public ModelAndView handleApiException(ApiException e) {
        if (e.error == ApiError.AUTH_SIGNIN_REQUIRED) {
            return new ModelAndView("redirect:/auth/signin");
        }
        if (e.error == ApiError.ENTITY_NOT_FOUND) {
            return notFound();
        }
        return prepareModelAndView("500.html", Map.of("error", e.getMessage()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // utility method
    ///////////////////////////////////////////////////////////////////////////////////////////////

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
        ModelAndView mv = new ModelAndView("404.html");
        appendGlobalModel(mv);
        return mv;
    }

    private void appendGlobalModel(ModelAndView mv) {
        final HttpContext ctx = HttpContext.getContext();
        final Website website = settingService.getWebsiteFromCache();
        // development mode?
        mv.addObject("__dev__", this.dev);
        // application name:
        mv.addObject("__name__", this.name);
        // application version:
        mv.addObject("__version__", Application.VERSION);
        // current user or null:
        mv.addObject("__user__", ctx.user);
        // url:
        mv.addObject("__scheme__", ctx.scheme);
        mv.addObject("__host__", ctx.host);
        mv.addObject("__url__", ctx.url);
        // timestamp as millis:
        mv.addObject("__timestamp__", ctx.timestamp);
        // settings:
        mv.addObject("__website__", website);
        mv.addObject("__snippet__", settingService.getSnippetFromCache());
        mv.addObject("__follows__", settingService.getFollowFromCache().getFollows());
        // CDN:
        // navigation menus:
        mv.addObject("__navigations__", navigationService.getNavigationsFromCache());
        // can search?
        mv.addObject("__searchable__", this.searcher.ready());
        // ads:
        mv.addObject("__ads__", adService.getAdInfoFromCache());
        // supported languages:
        mv.addObject("__languages__", translators.getLanguages());
        // translator:
        mv.addObject("__translator__", translators.getTranslator(localeResolver.resolveLocale(ctx.request)));
        ctx.response.setHeader("X-Execution-Time", String.valueOf(System.currentTimeMillis() - ctx.timestamp));
    }

    private static final int LOCAL_EXPIRES_IN_SECONDS = 3600 * 24 * 7;
    private static final long LOCAL_EXPIRES_IN_MILLIS = LOCAL_EXPIRES_IN_SECONDS * 1000L;

}
