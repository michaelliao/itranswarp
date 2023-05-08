package com.itranswarp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.itranswarp.bean.setting.Website;
import com.itranswarp.enums.RefType;
import com.itranswarp.enums.ResourceEncoding;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AbstractEntity;
import com.itranswarp.model.Article;
import com.itranswarp.model.Attachment;
import com.itranswarp.model.Board;
import com.itranswarp.model.Category;
import com.itranswarp.model.Headline;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.Navigation;
import com.itranswarp.model.OAuth;
import com.itranswarp.model.Reply;
import com.itranswarp.model.Resource;
import com.itranswarp.model.Setting;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.Text;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.util.HashUtil;
import com.itranswarp.warpdb.WarpDb;

/**
 * Generate database schema for API server.
 */
public class SchemaBuilder {

    static final Random random = new Random(1234567890L);
    static ZonedDateTime base = LocalDateTime.of(2022, 2, 22, 22, 22, 22).atZone(ZoneId.of("Z"));

    static long currentTimeMillis() {
        base = base.plusDays(1).plusHours(9).plusMinutes(18);
        return base.toEpochSecond() * 1000L;
    }

    public static void main(String[] args) throws Exception {
        SchemaBuilder builder = new SchemaBuilder();
        String ddl = builder.generateDDL();
        String inserts = builder.generateEntities();
        File ddlFile = new File("dev/sql/ddl.sql").getAbsoluteFile();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ddlFile), "UTF-8"))) {
            writer.write(ddl);
        }
        File initFile = new File("dev/sql/init.sql").getAbsoluteFile();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(initFile), "UTF-8"))) {
            writer.write(inserts);
        }
        System.out.println("\nGenerated SQL:\n" + ddl);
        System.out.println("\nRun generated SQL:\n\nmysql -u root --password=password < " + ddlFile);
        System.out.println("\nmysql -u root --password=password < " + initFile);
    }

    String dbName = "it";
    String dbUser = "root";
    String dbPassword = "password";
    String loginPassword = "password";
    WarpDb db;
    User admin;
    User editor;
    User subscriber;
    long[] imageIds;

    AtomicLong nextId = new AtomicLong();

    SchemaBuilder() {
        db = new WarpDb();
        db.setBasePackages(List.of("com.itranswarp.model"));
        db.init();
    }

    String generateTable(Class<?> clazz) {
        Set<Class<?>> classesWithUtf8 = Set.of(OAuth.class, LocalAuth.class, Resource.class);
        String charset = classesWithUtf8.contains(clazz) ? "UTF8" : "UTF8MB4";
        return db.getDDL(clazz).replace(" BIT ", " BOOL ").replace(");", ") Engine=INNODB DEFAULT CHARSET=" + charset + ";\n");
    }

    String generateDDL() {
        String[] tables = db.getEntities().stream().map(this::generateTable).sorted().toArray(String[]::new);
        String schema = String.join("\n", tables);
        StringBuilder sb = new StringBuilder(4096);
        sb.append("\n\n-- BEGIN generate DDL --\n\n");
        sb.append(String.format("DROP DATABASE IF EXISTS %s;\n\n", dbName));
        sb.append(String.format("CREATE DATABASE %s;\n\n", dbName));
        if (!"root".equals(dbUser)) {
            sb.append(String.format("CREATE USER IF NOT EXISTS %s@'%%' IDENTIFIED BY '%s';\n\n", dbUser, dbPassword));
            sb.append(String.format("GRANT SELECT,INSERT,DELETE,UPDATE ON %s.* TO %s@'%%' WITH GRANT OPTION;\n\n", dbName, dbUser));
            sb.append(String.format("FLUSH PRIVILEGES;\n\n"));
        }
        sb.append(String.format("USE %s;\n\n", dbName));
        sb.append(schema);
        sb.append("\n-- END generate DDL --\n");
        return sb.toString();
    }

    String generateEntities() {
        List<AbstractEntity> entities = new ArrayList<>();
        initUsers(entities);
        initSinglePages(entities);
        initHeadlines(entities);
        initArticles(entities);
        initDiscuss(entities);
        initWiki(entities);

        Navigation nav = new Navigation();
        nav.name = "Discuss";
        nav.icon = "commenting-o";
        nav.url = "/discuss";
        nav.displayOrder = 5;
        entities.add(nav);

        nav = new Navigation();
        nav.name = "External";
        nav.icon = "external-link";
        nav.url = "https://weibo.com/";
        nav.displayOrder = 6;
        nav.blank = true;
        entities.add(nav);

        Setting s = new Setting();
        s.settingGroup = Website.class.getSimpleName();
        s.settingKey = "name";
        s.settingValue = "iTranswarp";
        entities.add(s);

        StringBuilder sb = new StringBuilder(102400);
        sb.append("-- generated initial data\n\n");
        sb.append("USE it;\n\n");
        entities.forEach(entity -> {
            if (entity.id == 0) {
                entity.id = nextId.incrementAndGet();
            }
            if (entity.createdAt == 0) {
                entity.createdAt = entity.updatedAt = currentTimeMillis();
            }
            sb.append(generateInsert(db.getTable(entity.getClass()), db.getInsertableFields(entity.getClass()), db.getInsertableValues(entity)));
        });
        return sb.toString();
    }

    void initSinglePages(List<AbstractEntity> entities) {
        Text text = initText(entities);
        SinglePage page = new SinglePage();
        page.id = nextId.incrementAndGet();
        page.name = "Help";
        page.tags = "help,doc";
        page.textId = text.id;
        page.publishAt = currentTimeMillis();
        entities.add(page);

        Navigation nav = new Navigation();
        nav.name = page.name;
        nav.icon = "plane";
        nav.url = "/single/" + page.id;
        nav.displayOrder = 3;
        entities.add(nav);
    }

    void initUsers(List<AbstractEntity> entities) {
        List<String> emails = Arrays.stream(Role.values()).map(r -> r.name().toLowerCase() + "@itranswarp.com").collect(Collectors.toList());
        // insert users with password:
        emails.forEach(email -> {
            User user = new User();
            user.id = nextId.incrementAndGet();
            user.email = email;
            user.name = email.substring(0, email.indexOf('@'));
            user.role = Role.valueOf(user.name.toUpperCase());
            user.lockedUntil = user.role == Role.ADMIN ? 0 : 10000000000000L;
            user.imageUrl = "/avatar/" + HashUtil.sha1(user.name);
            entities.add(user);
            if (user.role == Role.ADMIN) {
                this.admin = user;
            }
            if (user.role == Role.EDITOR) {
                this.editor = user;
            }
            if (user.role == Role.SUBSCRIBER) {
                this.subscriber = user;
            }
            final String hashedPasswd = HashUtil.hmacSha256(loginPassword, user.email);
            LocalAuth auth = new LocalAuth();
            auth.id = nextId.incrementAndGet();
            auth.userId = user.id;
            auth.salt = HashUtil.sha256(user.email);
            auth.passwd = HashUtil.hmacSha256(hashedPasswd, auth.salt);
            entities.add(auth);
        });
    }

    void initHeadlines(List<AbstractEntity> entities) {
        Navigation nav = new Navigation();
        nav.name = "Headlines";
        nav.icon = "calendar";
        nav.url = "/headline";
        nav.displayOrder = 0;
        entities.add(nav);

        for (int n = 0; n < 25; n++) {
            Headline h = new Headline();
            h.userId = admin.id;
            h.name = randomLine(1) + " " + n;
            h.published = n < 15;
            h.publishAt = currentTimeMillis();
            h.description = randomLine(30);
            h.url = "https://www.google.com/search?q=headline+" + n;
            entities.add(h);
        }
    }

    void initArticles(List<AbstractEntity> entities) {
        Category category = new Category();
        category.id = nextId.incrementAndGet();
        category.name = "Sample";
        category.tag = "sample";
        category.description = "Java Series";
        entities.add(category);

        Navigation nav = new Navigation();
        nav.name = category.name;
        nav.icon = "coffee";
        nav.url = "/category/" + category.id;
        nav.displayOrder = 1;
        entities.add(nav);

        initAttachments(entities);

        for (int n = 0; n < imageIds.length; n++) {
            Text t = initText(entities);
            Article a = new Article();
            a.userId = admin.id;
            a.categoryId = category.id;
            a.publishAt = currentTimeMillis();
            a.name = randomLine(1) + " " + n;
            a.description = randomLine(10);
            a.tags = "abc,xyz,hello";
            a.textId = t.id;
            a.imageId = imageIds[n];
            entities.add(a);
        }
    }

    Text initText(List<AbstractEntity> entities) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("# ").append(randomLine(10)).append("\n\n");
        int lines = (int) (random.nextDouble() * 10) + 10;
        for (int i = 0; i < lines; i++) {
            sb.append(randomLine(20)).append("\n\n");
        }
        String content = sb.toString();
        Text t = new Text();
        t.id = nextId.incrementAndGet();
        t.content = content;
        t.hash = HashUtil.sha256(content);
        entities.add(t);
        return t;
    }

    void initWiki(List<AbstractEntity> entities) {
        Wiki wiki = new Wiki();
        wiki.id = nextId.incrementAndGet();
        wiki.description = "A Sample Wiki";
        wiki.imageId = imageIds[0];
        wiki.name = "Sample Tutorial";
        wiki.tag = "sample";
        wiki.textId = initText(entities).id;
        wiki.userId = admin.id;
        entities.add(wiki);

        for (int i = 0; i < 3; i++) {
            WikiPage p = new WikiPage();
            p.id = nextId.incrementAndGet();
            p.wikiId = wiki.id;
            p.displayOrder = i;
            p.name = "page " + (i + 1);
            p.parentId = wiki.id;
            p.textId = initText(entities).id;
            p.publishAt = currentTimeMillis();
            entities.add(p);

            for (int j = 0; j < 4; j++) {
                WikiPage sub = new WikiPage();
                sub.id = nextId.incrementAndGet();
                sub.wikiId = wiki.id;
                sub.displayOrder = j;
                sub.name = "sub " + (j + 1);
                sub.parentId = p.id;
                sub.textId = initText(entities).id;
                sub.publishAt = currentTimeMillis();
                entities.add(sub);

                if (j == 2) {
                    for (int n = 0; n < 3; n++) {
                        WikiPage leaf = new WikiPage();
                        leaf.id = nextId.incrementAndGet();
                        leaf.wikiId = wiki.id;
                        leaf.displayOrder = n;
                        leaf.name = "leaf " + (n + 1);
                        leaf.parentId = sub.id;
                        leaf.textId = initText(entities).id;
                        leaf.publishAt = currentTimeMillis();
                        entities.add(leaf);
                    }
                }
            }
        }

        Navigation nav = new Navigation();
        nav.name = wiki.name;
        nav.icon = "magic";
        nav.url = "/wiki/" + wiki.id;
        nav.displayOrder = 3;
        entities.add(nav);
    }

    void initDiscuss(List<AbstractEntity> entities) {
        Board board = new Board();
        board.id = nextId.incrementAndGet();
        board.name = "Discuss Sample";
        board.description = "Discuss Sample.";
        board.tag = "sample";
        board.topicNumber = 20;
        entities.add(board);

        for (int i = 0; i < board.topicNumber; i++) {
            Topic topic = new Topic();
            topic.id = nextId.incrementAndGet();
            topic.boardId = board.id;
            topic.content = randomLine(50);
            topic.userId = subscriber.id;
            topic.userName = subscriber.name;
            topic.userImageUrl = subscriber.imageUrl;
            topic.createdAt = topic.updatedAt = currentTimeMillis();
            topic.name = randomLine(3) + (i + 1);
            topic.refId = 0;
            topic.refType = RefType.NONE;
            topic.replyNumber = i;
            entities.add(topic);

            for (int j = 0; j < topic.replyNumber; j++) {
                Reply reply = new Reply();
                reply.topicId = topic.id;
                reply.userId = subscriber.id;
                reply.userName = subscriber.name;
                reply.userImageUrl = subscriber.imageUrl;
                reply.createdAt = reply.updatedAt = currentTimeMillis();
                reply.content = randomLine(50);
                entities.add(reply);
            }
        }
    }

    void initAttachments(List<AbstractEntity> entities) {
        imageIds = new long[12];
        for (int i = 0; i < imageIds.length; i++) {
            Resource r = new Resource();
            r.id = nextId.incrementAndGet();
            r.encoding = ResourceEncoding.BASE64;
            r.content = IMGS[i];
            r.hash = HashUtil.sha256(r.content);
            entities.add(r);

            Attachment a = new Attachment();
            a.id = nextId.incrementAndGet();
            a.width = 640;
            a.height = 360;
            a.mime = "image/jpeg";
            a.name = "img-" + i;
            a.resourceId = r.id;
            a.size = r.content.length() / 4 * 3;
            a.userId = admin.id;
            entities.add(a);

            imageIds[i] = a.id;
        }
    }

    String generateInsert(String table, String[] insertableFields, Object[] insertableValues) {
        String fields = String.join(", ", insertableFields);
        String values = String.join(", ", Arrays.stream(insertableValues).map(v -> {
            if (v instanceof String) {
                return "'" + escape((String) v) + "'";
            }
            if (v instanceof Enum) {
                return "'" + ((Enum<?>) v).name() + "'";
            }
            return v.toString();
        }).toArray(String[]::new));
        return String.format("INSERT INTO %s (%s) VALUES (%s);\n", table, fields, values);
    }

    String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 100);
        for (char ch : value.toCharArray()) {
            switch (ch) {
            case '\u0000':
                sb.append("\\0");
                break;
            case '\'':
                sb.append("\\\'");
                break;
            case '\"':
                sb.append("\\\"");
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            default:
                sb.append(ch);
                break;
            }
        }
        return sb.toString();
    }

    String randomLine(int n) {
        int w = (int) (random.nextDouble() * n) + 1;
        String[] words = new String[w];
        for (int j = 0; j < w; j++) {
            words[j] = WORDS[(int) (random.nextDouble() * WORDS.length)];
        }
        return String.join(" ", words) + ".";
    }

    static final String[] WORDS = { "a", "ac", "accommodare", "accumsan", "accusata", "ad", "adhuc", "adipisci", "adipiscing", "adolescens", "adversarium",
            "aenean", "aeque", "affert", "agam", "alia", "alienum", "aliquam", "aliquet", "aliquid", "aliquip", "altera", "alterum", "amet", "an", "ancillae",
            "animal", "ante", "antiopam", "aperiri", "appareat", "appetere", "aptent", "arcu", "assueverit", "at", "atomorum", "atqui", "auctor", "audire",
            "augue", "autem", "bibendum", "blandit", "brute", "causae", "cetero", "ceteros", "civibus", "class", "commodo", "commune", "comprehensam",
            "conceptam", "conclusionemque", "condimentum", "congue", "consectetuer", "consectetur", "consequat", "consetetur", "constituam", "constituto",
            "consul", "contentiones", "conubia", "convallis", "convenire", "corrumpit", "cras", "cu", "cubilia", "cum", "curabitur", "curae", "cursus",
            "dapibus", "debet", "decore", "definiebas", "definitionem", "definitiones", "delectus", "delenit", "delicata", "deseruisse", "deserunt",
            "deterruisset", "detracto", "detraxit", "diam", "dicam", "dicant", "dicat", "dicit", "dico", "dicta", "dictas", "dictum", "dictumst", "dicunt",
            "dignissim", "dis", "discere", "disputationi", "dissentiunt", "docendi", "doctus", "dolor", "dolore", "dolorem", "dolores", "dolorum", "doming",
            "donec", "dui", "duis", "duo", "ea", "eam", "efficiantur", "efficitur", "egestas", "eget", "ei", "eirmod", "eius", "elaboraret", "electram",
            "eleifend", "elementum", "elit", "elitr", "eloquentiam", "enim", "eos", "epicurei", "epicuri", "equidem", "erat", "eripuit", "eros", "errem",
            "error", "erroribus", "eruditi", "esse", "est", "et", "etiam", "eu", "euismod", "eum", "euripidis", "evertitur", "ex", "expetenda", "expetendis",
            "explicari", "fabellas", "fabulas", "facilis", "facilisi", "facilisis", "falli", "fames", "fastidii", "faucibus", "felis", "fermentum", "ferri",
            "feugait", "feugiat", "finibus", "fringilla", "fugit", "fuisset", "fusce", "gloriatur", "graece", "graeci", "graecis", "graeco", "gravida",
            "gubergren", "habemus", "habeo", "habitant", "habitasse", "hac", "harum", "has", "hendrerit", "himenaeos", "hinc", "his", "homero", "honestatis",
            "iaculis", "id", "idque", "ignota", "iisque", "imperdiet", "impetus", "in", "inani", "inceptos", "inciderint", "indoctum", "inimicus",
            "instructior", "integer", "intellegat", "intellegebat", "interdum", "interesset", "interpretaris", "invenire", "invidunt", "ipsum", "iriure",
            "iudicabit", "ius", "iusto", "iuvaret", "justo", "labores", "lacinia", "lacus", "laoreet", "latine", "laudem", "lectus", "legere", "legimus", "leo",
            "liber", "libero", "libris", "ligula", "litora", "lobortis", "lorem", "luctus", "ludus", "luptatum", "maecenas", "magna", "magnis", "maiestatis",
            "maiorum", "malesuada", "malorum", "maluisset", "mandamus", "massa", "mattis", "mauris", "maximus", "mazim", "mea", "mediocrem", "mediocritatem",
            "mei", "mel", "meliore", "melius", "menandri", "mentitum", "metus", "mi", "minim", "mnesarchum", "moderatius", "molestiae", "molestie", "mollis",
            "montes", "morbi", "movet", "mucius", "mus", "mutat", "nam", "nascetur", "natoque", "natum", "ne", "nec", "necessitatibus", "neglegentur", "neque",
            "netus", "nibh", "nihil", "nisi", "nisl", "no", "nobis", "noluisse", "nominavi", "non", "nonumes", "nonumy", "noster", "nostra", "nostrum", "novum",
            "nulla", "nullam", "numquam", "nunc", "ocurreret", "odio", "offendit", "omittam", "omittantur", "omnesque", "oporteat", "option", "oratio", "orci",
            "ornare", "ornatus", "partiendo", "parturient", "patrioque", "pellentesque", "penatibus", "per", "percipit", "pericula", "periculis", "perpetua",
            "persecuti", "persequeris", "persius", "pertinacia", "pertinax", "petentium", "pharetra", "phasellus", "placerat", "platea", "platonem", "ponderum",
            "populo", "porro", "porta", "porttitor", "posidonium", "posse", "possim", "possit", "postea", "postulant", "posuere", "potenti", "praesent",
            "pretium", "pri", "primis", "principes", "pro", "prodesset", "proin", "prompta", "propriae", "pulvinar", "purus", "putent", "quaeque", "quaerendum",
            "quaestio", "qualisque", "quam", "quas", "quem", "qui", "quidam", "quis", "quisque", "quo", "quod", "quot", "recteque", "referrentur",
            "reformidans", "regione", "reprehendunt", "reprimique", "repudiandae", "repudiare", "reque", "rhoncus", "ridens", "ridiculus", "risus", "rutrum",
            "sadipscing", "saepe", "sagittis", "sale", "salutatus", "sanctus", "saperet", "sapien", "sapientem", "scelerisque", "scripserit", "scripta", "sea",
            "sed", "sem", "semper", "senectus", "senserit", "sententiae", "signiferumque", "similique", "simul", "singulis", "sit", "sociis", "sociosqu",
            "sodales", "solet", "sollicitudin", "solum", "sonet", "splendide", "suas", "suavitate", "sumo", "suscipiantur", "suscipit", "suspendisse",
            "tacimates", "taciti", "tale", "tamquam", "tantas", "tation", "te", "tellus", "tempor", "tempus", "theophrastus", "tibique", "tincidunt",
            "torquent", "tortor", "tota", "tractatos", "tristique", "tritani", "turpis", "ubique", "ullamcorper", "ultrices", "ultricies", "unum", "urbanitas",
            "urna", "usu", "ut", "utamur", "utinam", "utroque", "varius", "vehicula", "vel", "velit", "venenatis", "veniam", "verear", "veri", "veritus",
            "vero", "verterem", "vestibulum", "viderer", "vidisse", "vim", "viris", "vis", "vitae", "vituperata", "vituperatoribus", "vivamus", "vivendo",
            "viverra", "vix", "vocent", "vocibus", "volumus", "voluptaria", "voluptatibus", "voluptatum", "volutpat", "vulputate", "wisi" };

    static final String IMG_0 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpCODQ5NDA1RTQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDpCODQ5NDA1RjQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjEzOTcxQUQ1NDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjEzOTcxQUQ2NDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAI4AAQADAQEBAAAAAAAAAAAAAAACAwQFAQYBAQEAAwAAAAAAAAAAAAAAAAABAgMEEAEAAgADAwgDDQ"
            + "gDAAAAAAAAAQIRAwRREhMhMUFhUuJTk4HRkpGhscHhIkJicqLSFAXwcTIjM0NjJIKyoxEBAAEDAwQDAQAAAAAAAAAAAAERUQIxEiJhobED8EEycf/aAAwDAQACEQMRAD8A+jAdDlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAUX1Na24dYm9+xSMZ9PRHpmErQiK6LxXGXq78sZdKfbvjP3Y+N7+W1e3K9y3rTdDPZKYhwNZHhT6bR8U"
            + "ozXV158qs/ZzPXEG6DZktFE596/wBTKzK/8d7/AKzKP57K6d7HZw7+pd0JtmzSM/5qd6sbl4red2LWjd5cJnmnl6NjQRNUmJjUAVAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFefmxk5dsyeXCObbs"
            + "gELb+ozOBlTu4cuZePoxsj60+9HLsdLI0+Xp67mXEVj9ufbLFp6W0mTM4b+ZhN7RH0r/tyR1L9PrJtlZeZn1nKvmTu7k7Z6GiZrLpjGkNYy8bP/M8Phxwd3Hib30tmDUigAAAMH6rH8mL9NL0tHtRHwS8S/VZ/kbvTa+XX70ItmDV7PoAbGoAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAZdbaIjLrPTmZfJtws1MuoreM3KvlWit5mcvljGMJjGdnZ2sctGWH6h0VOdpsvUTE5sTO7/DhaYwnbHX1qorq6fTpf7VZr8EynWNXPLu5WH2rfhaXQ20mIiK444"
            + "bedPFycjUZ+5FbZV7Z3LvYxu1iftTyYR1Yr4rrOzle3b8IroDn7us7OV7dvwqtR+epl2tWKYxHNXetPo5ubnEdV5NojpcyuuysIiJveY2ZdsZ95VqM3UTu4RwaXtuYzhN+WJ6OaObrnqBbqsyNRn1y68tcr595+th82PfmfcWK8rKrk13KRhHw9c"
            + "9axuxikOfKayAMmIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAz5v9fJj61p+5LQz5vJn5E/WtHu0ljloyw/UNdf47fuhPT5k4UrtxRis70z0TEGXXCK489Wl0NghW8T+9MEL23cOucClpmbRPRL21d7"
            + "DqnErXCZnaCHEmK2tsnBl/U/4cqf8ALT42uleeJ6Zlk/U+bKj/AC1+CZISdHgDocwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA5+qtmZudGXl2ivD3czHDGcZxw9GHO6DDn6fN4s5uVuzForExaZj"
            + "mx6pP7oRX61e7+pn+77mXU3tR40+xVXw9T2K+Z3Xm5qexXzPkKYWK+y/hbvanxvuVexfUx/en2K+pTuanw48z5Dh6nsV8zulMLFfZfwu4mp8a3sU9RxNT41vZp6lPC1PYr5ndOHqexXzO6UwsV9l13E1PjW9inqV5tc/N3d7Omd2d6PmV58MNnWj"
            + "wtT2K+Z3ThansV8zulMLFfZfwl/seL/wCcH+z4seXHrR4Wp7FfM7pwtT2K+Z3V4/Kpy6dksdT4tfL+Ux1PiV8vvI8PU9ivmd04ep7FfM7px+VOXTsnvarxKeX3je1Xbp5c/iQ4ep7FfM7puanw48yPUcTl07OkAigAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP//Z";

    static final String IMG_1 = "/9j/4AAQSkZJRgABAgAAZABkAAD/7AARRHVja3kAAQAEAAAACgAA/+EDLGh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8APD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz"
            + "4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS41LWMwMTQgNzkuMTUxNDgxLCAyMDEzLzAzLzEzLTEyOjA5OjE1ICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly"
            + "93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy"
            + "5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDowNUJDMkU1QTQyMDIxMUU5OTk4RkJCNjMyNTY5RTk2QS"
            + "IgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDowNUJDMkU1OTQyMDIxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ0MgKE1hY2ludG9zaCkiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW"
            + "5zdGFuY2VJRD0ieG1wLmlpZDo3MEZERjI3OTJCRDgxMUU2OUIyREM0OUM4MTBDNDI3MyIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDo3MEZERjI3QTJCRDgxMUU2OUIyREM0OUM4MTBDNDI3MyIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZG"
            + "Y6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Pv/uAA5BZG9iZQBkwAAAAAH/2wCEABQQEBkSGScXFycyJh8mMi4mJiYmLj41NTU1NT5EQUFBQUFBREREREREREREREREREREREREREREREREREREREQBFRkZIBwgJhgYJjYmIC"
            + "Y2RDYrKzZERERCNUJERERERERERERERERERERERERERERERERERERERERERERERERERP/AABEIAWgCgAMBIgACEQEDEQH/xACAAAEAAwEBAQAAAAAAAAAAAAAAAQMEAgUGAQEBAQEAAAAAAAAAAAAAAAAAAQIDEAEAAgIABAMEBwYFBAMAAAAAAQ"
            + "IRAyExEgRBUWFxgSITkaGxwTJyBdFCUoIjFPDhkqIz8bLCU6OzFREBAQEBAQEAAAAAAAAAAAAAAAERMSEC/9oADAMBAAIRAxEAPwD64BzbAFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAUAAAAAEAAAAABQAAAAAAAAAAAQAFAAAAAAAAAAADmAAAAAByAA5AByEABQAAAAAAAQAFABAAUAAAAAAAAAAAAA5gAAAAACAAoAAAAASAAAAg"
            + "AAAAAAAAAAAAAAcgAAAAAOYABzAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA5AAAAAAcwAAUOYCAAoAIACgAAAAAAAgAKACAAoAIAAACgAgAKACAAoAIAAAAAAACgAAAgAAAAAKAAACAAoAIACgAgAKAAACAAAAAAoAAAIACgAgAKAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAoAAAAAIACgAgAKAAACABkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABQAAAQAAAAAAAFABAAUAEABQAQAFAyAAAAAAAAAAAACAAoAAAAj"
            + "ORT3dbW1W6JmLdMzXHnhRo/UNdtfVe2LViOvh58p96DcKY7jXFIvNois8OPDitraLRmOMeaiQABR3Hd6u36Y2WiJtOI/x9/Jf7EAAABQAAAAAAAAAAAQAFABAAUAAAAAEABQAAAQAAAFABAAAAAAUAJQAFABAAAAAAUAAAEABQAQAAAFAGXdstW1"
            + "aa8RNotOZ48sevqIv2bK64zeYiPWXzunVbdWuY6a2+HqrjjEZmOrOZz8MY98cnvae0pFvmTM2v/FM/sxDJ/Z7pts1xiuuJ6tVufGefj6zzLPDVOzt+nha02paLzETj8WJnMcPze9u7LZFtUVieNY6Z8+Dyuz32vsmmyOnptN8eMRMXmZn6Y+lr01"
            + "jXsrujlaba/TE2tMT9kJFbtvc69M1recTbhVcw91+n07i8bJmYnlb1hX3P6lXVnXqxa/DEZ4fT9Pj4Kizv+wjuppbxrMZ/L+36Pa2xGOEPjtXf7J7uNl/gmZ5Y8Orly+t9fTZGysXrxiYiYB2Kb76UvWkzEWtn6oyuhFAFABAAUAEABQAAAQAFAB"
            + "AAAAAAAAUAEABQAAAAAAAQAAAFABAAAAAAUAEABQAAAQAAAFABAAAAAU9z29O4rNNnL0XCjLq7a+mvTrviI84jH7Svc7pvOvorMxHPqn7On72ll7aZt3W2P4Yr9cESuN/a111veIxs3TWl5iZ5cvsROiOiNVZ5RGJ9a8l/6jovv0TSnC3CY+lm7G"
            + "JpqilpzaJtE+2MtSesfV8aO32/0p6541zW38v+XF4+f7ms7Lxib8PZEf4y095snTXbbGa3r0z6Ty+vMK448Y+ln6mVZdjFr7H5VpmLZ4cOHKfDx/x6vS/RZnGykzmK3wqwu/SY6LbYnHGY5e9Gl+79M17bzaZmIv8A8kefl7OLdERBnPLxSoDnrj"
            + "OM8XSKAAAAAAAAAAAAAAAAAKAAAAACAAAAAAAAAAoSAAAgAAAAAAAAAAAKACAAAAAAAAoAIACgAgAKDP2dc32X/inH+nMNCrs5+CfPq2f99liVomHjWrsjuLbY4aovj3zEV+2XtMvfxadNopE2tOOmI88tMsUz8/bOivGIms7M+63vy1z+m9vac9"
            + "ER+Xh9h2Pbzq15vGNluN5+yPXDYdOPOt+lauHRNq+fx25eX4mbs/0y2vudk7Yi2uYnp6vi/e4c+L2hMGPb2VZjOnGu/wDFWPu8WDf3t72/tKZjd+9MTw+n15+mfPg9t5f6j2fzb0vWei1p6JtjPDFp5e1LFlUf/mWnXPVaPmTx6sZ/6+2fat/T98"
            + "zNtWzMTExiJnqxmOWfHz/mx4M99NuzmJnE+PXWOnOOM1mI9InzWbo6e6jb/D0f7pms/UxytPWEcktAAAAgAKAAACABKgAAAAAAAgAAAKGAEAAAAABQAQAAAAAAAAAAAFAAAAAAABAAUAAAAAEABQQlAE2iOc4YNXc11btlZtHROJrPhE+OZ9c8HX"
            + "b0+fbZG6ZzW0xWMzHw+HJ1ns4iaz04mcT1Rzmvt8lZb44kvP1b4rurr13m9Ji0TmcxExyxPj4+MtHdXvXo+XOJm2OPH923sXUaEsVtvc1/ci3s+H75V/3fdf8Ao/8AkqauPRc7NldcZtOIedfvu4pNYtqivVPTGbZ4+5H6h86NM3tNcVmJxFZ9n8"
            + "U+ao9Nm7mc2118erP1WaYnLFidnc2vn4aR0RHrzz9eEqsndzbbvinOkdNf5pnj/smTuJi+y2PxTOukR78z9EWynPGLz/7bZ+usfcjRpm/c26o/48W9vVXH3MfXcWPUj1SIlVSAgAAAAAAAKACAAoAAAIACgAAAgAAAKACAAoAIACgAASAACAAAAo"
            + "AQgAAAAAKAAAAACAAoAAAAy77TpvXbEZicUt7Oefdx+lxfbPax18LaZmZzHh1cfDOeM+Tazbey1bPDE+PTEQIndMYpeOUWrx/N8P3uu5421fn/APGznuYiuqccq9M/6ZhF7dUabedon/ZYhWhICq92qNtZrPjDjVsr3NJpfhaMddfX9mV7NovG3b"
            + "e8cIr/AE/fnj9yxK77TdE1+VMx10jFo8fT6UdvOJvWecXt9fH72bt6RbbTd+9s6rz+Xw+2FsVm3d2vH4a06J9vC32HUZ+6xptNbf8AFsmc+mY4z9TmItWZ22nNtcxWZ/irP7Mzyd992eqbT3G29ojpxjMdM+nHz8mPse320mmq9omM9V+eYx+GPZ"
            + "w8fXCUe9AiEjSEggAAAKACAAAHIAAkAAAAABQAQDmCgAAHIQAAAFAAA5ggAKHqAgAAAAHIAAAOYCgAgAKACAAoAAAIACgAgAKKO8j+hs/Lb7FNJ/pdt/L/APXZZ3tojVMTwiZrE++0KtM/M3R0/wDHrr0x+b2eyRG4EZwKq7nfGiuedp+Gses8lG"
            + "3RHRXtYnGYza3jiuOPtmcO9lqW2zN/w6q9U+2Z5+7plRN9lNdts8b7Jjojyrzx5cOIjrXeNU7O6vPwR8NY8sT0z9MxEtPb0mtM2/FaZtPtnjj3cvcoitbWr2/ONcZt5csR9ufc2g5tWt4xaMx5SU1V1x00iKx6Rh0CoSSIAcwABQAAAAAQAADkCg"
            + "AgHIAAAAACQUAAAAAAAAAAAEAAABQAQAFAAAAAAAAABAAUAAAEABQAABAIveuuOq8xER4zwRXZW8dVZiYnxhh/VqdeqI48/CJnwnniJ+HzZJv3Ea6bKUmOqnT0UiYxOa/u+EcJwg9m8V2R0WiJjylzX5WqsxXFaxzxwiPN4Oie4tstWk3nbThabT"
            + "8EfDMfTM9Ph6utXbdzauyL9fXMTExn4Z+KJ+zy4eCo93XtptjOu0WjOM1nLL/c7tm6aaoiddZiLzPPlnhx8pjwWdp29dWmKRHTMxGceeOLFTtNmj51tc3m/PXM28enHsmf8kV6GztdW23XaMzjznj7vFO7TG6IjM1ms5iazj0eZ2+zfF7xEbei1Z"
            + "6J2Rx64+yJzHOPBN9fda61+XNptanxdU5xbNfo8Qelo1a9cT0cc855zPtWvDveadvM9t835kRXq6uvPrjq4Zzzx6tnZ9zPcbdk5noicViYx+7Xjjn4yD0REJUAAAEABQAQAFABAAUAAAAAAAAAEAAABQAQAFAAABAAUAAAAAAAEABQAQAAAADmCg"
            + "AgAABIoAIAAACgACina012vsp+K8x1e5Z8WMOxBx8RGXeDmDj4nUZ8UmAROZjEcJUdr23yYmbW6r2t1Wnlx4R90NGRQAAAQAFABADkKAAACAAAAoAAAhBICgAgAKAAAAAAAAACAAAAoAAAAAIAAAAACgAgAKACAAAAAAoAIACgAAAAAAAAAAAAAg"
            + "AAAAAKACAAoAIAAAAAAACgAAAAAgAAAAAKACAAoAAAIACgAgAKACAAAAoAIAAAAACgAgAAAAAKACAAoAAAAAAAAAIAAACgAAAAAgAAAAAKACAAAAoAIAAAAAAACgAAAAAgAKACAAoAAAIAAAAACgAgAAAKAAACAAoAAAAAIACgAgAKAAACAAAAAA"
            + "oAIAAAAACgAAAgAAAKACAAAAAAoAAAIACgAgAKAAACAAAAAAoBzAAAIAQAgAAUAEADmAAoAIBgAAFAAABAAUAAAEDmAoAAAIACgAgAcgAAAFAAABAAUAEABQAQAFABAAUAAAAAEABQAAAAAQAFAAAAAAABAAUAAAAAEABQAAAQAFABAAUAAAAAEA"
            + "AABQAQAAAFAAAAAAAAABAAUAAAAAAAAAEABQAAAAAQAFABAAAAUAEABQAAAAOQAAIAAACgAAAgAAAAAAAKAAACAAoAAAAAIACgAAAAAAAAAgAAAAAKACAAAAAAAAAAoAAAIGAAAAAAAFABAAUEAgkBQAQAAAFAAAAAADAAACAAAAAAoAIAAAAACg"
            + "AgAKAAACAAAAoAA//Z";

    static final String IMG_2 = "/9j/4AAQSkZJRgABAgAAZABkAAD/7AARRHVja3kAAQAEAAAACgAA/+EDLGh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8APD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz"
            + "4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS41LWMwMTQgNzkuMTUxNDgxLCAyMDEzLzAzLzEzLTEyOjA5OjE1ICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly"
            + "93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy"
            + "5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDowNUJDMkU1RTQyMDIxMUU5OTk4RkJCNjMyNTY5RTk2QS"
            + "IgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDowNUJDMkU1RDQyMDIxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ0MgKE1hY2ludG9zaCkiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW"
            + "5zdGFuY2VJRD0ieG1wLmlpZDo3MEZERjI3OTJCRDgxMUU2OUIyREM0OUM4MTBDNDI3MyIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDo3MEZERjI3QTJCRDgxMUU2OUIyREM0OUM4MTBDNDI3MyIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZG"
            + "Y6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Pv/uAA5BZG9iZQBkwAAAAAH/2wCEABQQEBkSGScXFycyJh8mMi4mJiYmLj41NTU1NT5EQUFBQUFBREREREREREREREREREREREREREREREREREREREQBFRkZIBwgJhgYJjYmIC"
            + "Y2RDYrKzZERERCNUJERERERERERERERERERERERERERERERERERERERERERERERERERP/AABEIAWgCgAMBIgACEQEDEQH/xACOAAEAAgMBAQAAAAAAAAAAAAAABAUBAwYCBwEBAQEBAQEAAAAAAAAAAAAAAAEDAgQFEAEBAAECAwUFBQUHAwUBAA"
            + "AAAQIRAyExBEFRcRIF8GGBwSKRobEyE9HhQnIGUmKCoiNDFDNTc/GSstIkNBEBAQACAgEFAQEAAAAAAAAAAAERAiExA0FRYRIyE1L/2gAMAwEAAhEDEQA/AOhAfPekAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAClAClAAAAAAAAAAAAAClAClAClAClAAAAgAQgAQgAQgAQ7gA7juADuAAAAAAAAIAEIAEAAAA"
            + "AAAApQApQAAAAAAAhAAhAAAAAAAAAApQApQAAAAAAAAA7zvADvO8AKUAKUAKUAKUAAACABCABCABCABAAAAAAAAAAAACABCABAAAAAAAAAAAAAAAAACEACEAAAAAAAAAClAClAAAAAAAAAAAAAClAClAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAQgAQAAAAAAAAAAAAgAQgAQAAAAAAAClAClAAAAAAAAIQAIQAAAAAAAAAKUAKUAAAAAAAAAO87wA7zvAClAClAClAClAAAAgAQgAQgAQgAQAAAAAAAAAAAAgAQgA"
            + "QAAAAAAAAAAAAAAAAAAAAAAAAAAAAAClAClAAAAAAAAAAAAAClAClAClAClAAAAgAQgAQgAQgAQ7gA7juADuAAAAAAAAIAEIAEAAAAAAAApQApQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAPE8QA8TxAD29vbwPb29vAAPb29vA8AA8DwADwPAAPA8AA8DwAAAAAAAAAAAAADwPAAPA8AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA8TxADxPEAAAAAAAAAAAAAAAApQApQAAAAAAAAAAAAApQApQApQApQAAAIA"
            + "EIAeJCAeJ4kIB4hDuADuO4AO4AAAAAAAPEIB4niQgHieJADxPEAPE9vb28QA9vb28T29vbxADwPApQPA8ClAAAAAAACEACEAAAAAAAAAClAClAAAAAAAAADvO8AO87wApQApQApQApQAAAIAEIAEIAEIAEAAAAAAAAAAAAIAEIAEAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA5nMAOZzAAAAAAAAAAAAADmcwA5nMAAAAAAAAAAAgAQgAQAAAAAAAAAAAADmAR4y3cJfLxuXbMZrZr+HxPNuc/w"
            + "BLP/L/APZ1NbUzHsjXN/DXy5a4ZXszmn2dn2Vs5JZZ2uQ5AgcgAAecsvL2a23SQnI9HGsTHHbtu9uzXhLjNMZLfv8AvbP+L090zy4y6aebLKy68uFung2nivrXH3jwc+TdOh6e/wC3j/7Y8zoely444Y8O3Hs+xf4/Kff4a6N2PRbeP5PNL/Nlfu"
            + "tsRc8c9iee5Tc2u3L+Ke/hw0nby041zfFZ8rN5WwBk7AAAhAAhAAAAAAAAAApQApQAAAAAAAAA7zvADvO8AKUAKUAKUAKUAAACABCABCABCABAAAAAAAAAAAB43crjjbrpymvdr+x70YsmUsvGXhYTi8jZ1Ny6fbn6Okty7bx48eGtmt92vLkiY+"
            + "qZXDK2ayeaTKTTjjh5vy34+3Fst3MZcNMdzbv8GfOfHjr7pp8Xr/lY4Y657NxxnP8AJZpy7+7uevXfW9VjZUnC49TjcdyS8csfLePCWxF28bjrhzmOVxxvu/dy1+L3ubuWedx2tMZ5ZbnONsy7uz8WMcZjNJ7e338XHls/Pq60l7ZPE0QOq9V2un"
            + "tww+vOcLJynjf2MJLemluO0/mKPDHr+uvmxtwxvKz6Z+2pm50d6Xbuee9uZZ8seNv1Xl9Pb8exp/P55cfZYaPGcylm5t6XLHXhe2ds93Z+DMx3s+OO3p/Plp+Gqs6j1TPYz8uU287/AHMrw8eBrptLmLdosMsem63c45aZWaXDLhdey8e7Wzhw4t"
            + "2PQTG53zX6pZ4a/jp2dyjx6jq+vsmG3j5LeFuOsnxy4fZFlt/rbV/T3rLPL5r+n5sbLPC9vZppyvBt9sds8Z6S8OgmGeOcyumMnDw17dffre9ien+XLzTL+PLc0s/tfOacL2d1R9vHcyxn6mWWM5yTO68ePHLn7tNbJ72ybdxusyz4d+5lfml8uq"
            + "/Sk9O8s1y3LNLjlbppxmvHu1uv3PG3lMcLtbGXmxty+rT6Z5rrz7dOXDn2tmeGOf55L48XrTg4vl/zFmnuxjjMZMZyk0jIMGgAAAAAAAAAAAAAAHM5gBzOYAAAAAAAAAfM+YAfM+YAczmAHM5gBzOYAczmAAAByADkcgA5HIAORyADkfIAPkfIAP"
            + "kAAAAAAAHMIAACPNnLb3P1McpNvTTLHTSSe6/t76r+p9a8tuOxjrp/Fly+E/el+qzXp7PNMe3jefu9v3ofoW3sZ+bzyXclnl83d3xvpJZ9tuXFuLiM7HQ9Z1f1b+dxwy5468/8PKe2sWnT+ndPsSTHCWz+LLjdUrKZWaY3S96L0HUXdmeOX+3ncJ"
            + "e+Tlr73aM9TuYbWWP6m7cJlwxnDTh79Pmreu9awymmxLbLMvNeE4fej+uXLLenO4SaTu17fj3rXofTtnZwxysmed0vm5/YvE5c98K7Z6fr+r+rczywxvfdP8s0+9ZbHpPTbE/L5r358f3fcmbmVxxtxmt7J7djz0+eO7t45bd82NnC95m1cSPWW5"
            + "jhpLdNeEiFvdJeo387hnlhnMcLjljf5uc7Wd/Z3d3qNrc2L9OOsz46Sz5+3JM/4OOW7N/PXzyaTS6TT5/EkS1Wzd6npfp6rDXH/u4cv8X7UzGzKTLG6zss5LCzXheMQOr6PHb2rudPPJlhrn5ceEy91nK6/a528WeYs39wa+m35v7eO7jNJlOTY8"
            + "1mK1AAAAApQApQAAAIAEIAEIAEIAEIAAAAAAAAAAAAAEIAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAADGsk1vCTtapN7qeGxpht3/AHMuOv8ALPnfhrwrZsdP+tnnnuaZbc+nHHs4c7e+69/LRY6dz0ePx+tZ7bekQZ0fTdNhM97TK4/7m7xv235Oe9"
            + "NzmXXTKcdbn+Fddnt47k8ucmU7rNUXD0zpsN2b2GExyndwn2cm2PZnl682vCcb7jY6PHa1vblbldOWt/H4pGX0TXGa6dkcp6j6h1uWdmXm2sZdJJrP83b+CSSdrblYeu9VszZ/4+Nnn1x+nHs09uR6Fncunst10y0n2RT9H6bv9bfNjNMbzzy9uL"
            + "qej9O2ulw8mOt15+bj93JbMkuGeOfDGazvvJnpuhw6bbm1jrcZ3pQTWRLcsaaMg6QYvGIHqfW7fT7Vnm0z4aYy/VzVuz/UG5u7+OHkkwysx07eN011Bv8ATtcdvLDswz3MZ8KlonRc973buaW8O/6r0a9AMuVYAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAJzOwBTdfv5Y9Nj5LZ/q7nK6crWvpvXuo2ZMdzTcnv4X7Wvrbr0+3f7+9/wDJXPdr0897df03rnTb/DK+TLuz/asplLNY+fPW3u57N823lcb3yukfQDSOY6X+odzDTHfx80/tY8L+/wC50HTdVt9Vh5"
            + "9q6z8EG/QBQAAAByPqPSbm71m55MbZrjreyayLXofQ9vYym7uXz5zjj2Sfv+Kx62T9HL4fjEhyqk6Lnv8A/mzS0Xovzb//AJc0p5N/1W+vQGgzUAUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPEAPE8QA8QAAAAAAAAAADsCMzmDn"
            + "Oqn/5drScJnuzv7e9XrHqpJ0uEknDd3J7e18Vc92vTz0AdIOj/AKa/JufzT8HOOk/pr8m5/NPwQXwCgADR1PUTp8ZnZbNZLp2a9t907Vdh61s7+/ht43KTzc9OGWvCTv58mz1jek6bcwxv1SY6z3W6OX6THXf25354/ig7TrP+jl8Pxje0buzrtf"
            + "p7c05SeEbsspjLlldJONtBUdJNLu3v3c0lq2LLjcpyyyzynhcrp9vNteLf9V6J0UByoAAAAAAAAeAAeB4AB4HgAAAAAAAAAAAAAAAAAHgeBzOYHgeBzOYAAAAAAAAAAAAAAAAAAAAAAAAAAB2gCl6/buPTXXs3sv8ANNVOv+svn/U6fPTGZ3HPbz"
            + "v5dZJwt+Cj3NrPb/NNNeV7L4Xt+D2aXiMNpy8ANHI6X+mv+lufzfJzTp/6bn+jnf7/AMoguwFAAGvd2sd3DLDKcMppUDpfSen6K3e1tsn5s7OHt3vXUeq7OEuO1/q7nZjt/V9tQLsdT1t16vLy4f8Aaw+ftfg422mvbqS3pP3/AFXbxlx2J+rn3Y"
            + "cZx5a5ckPLpuo6nKZdTufTLrdrCfT4e/4xL29vHbxmGE0xnZHvm8+3kt64jSaSdsEBk7CAAAAAAAAAAAAAAABSgBSgAAAAAAAAAAAABSgBSgBSgBSgAAAQAIQAIQIBCEIBDuIAdx3AB3AAAAAAAADLAMxG3ulm5LJw83PG8cb8OGnjLOPPVIFzZ0"
            + "Yc71Hp9w82WM007MrwnLlneGU+y9+qBlhljp5pZrNZq7G8EPe6DHKWbfCc/JnPNhr+OP8Ahsba+X02Z3T2czydT/Ts06fL+e/hFXv+lzHG563b07Mp5sfhcdbp/NNe9eej7U2enmPmmWtuWuN1nFvNpemeMLEQ8/UtjHK7eN82c/hwmv7p8ar9yd"
            + "V1nDdy/S2/7GF+q+OSXeTsktT9/wBU2Nrhjl+pn2Ybf1X7lbubXV9b/wD0Zfpbf/bw5/G/+s9yXtbOGzj5duTGe5sYbeW3rhrNJ6te1s4bOPk25MZ7mzQGLsAAoABPkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQgAQgAAAAAAAFAClACgAA"
            + "AAAAAAAAAAAAAACv3/AEna38/1JbjbePl9vbmsOQstnM4LMtXT9Nh02H6e3NJ2++tpBLzyAAFDsAAEAKKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAIACgAAAD//Z";

    static final String IMG_3 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpDRDQ2NTJFMjQyMDQxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDpDRDQ2NTJFMzQyMDQxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkNENDY1MkUwNDIwNDExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOkNENDY1MkUxNDIwNDExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAHQAAQADAQEAAAAAAAAAAAAAAAABAwQCBQEBAQEBAQEAAAAAAAAAAAAAAAECBAMFEAEAAgIAAwgDAQ"
            + "EAAAAAAAAAAQIRA1GRBCExQXESUjMU0TITYUIRAQEAAgICAgMAAAAAAAAAAAABEQIhEjEDQVFhMhP/2gAMAwEAAhEDEQA/APYAafSAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABMW9M55u7a5zivbHgjPbF5Vi6OmvPgi3T3jwVO+v2qFlY9GbT4d3mr7xqXIAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA71U9dogS3Ey"
            + "t6fp/X227m+tYrGI7iIxGIZ9Oi2u82m0zlHFtt25y0gIw4vrrsjEvP3aZ1T/jVXReNs7JtOOGVu2nrrMK9NN+t/DywmMTgV2gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACEu9VPXaIEtxy4W9Nspr"
            + "tm8xEY8W/+FMYwzatVY2zWYiYiEeF9k21sXfc0e+vM+5o99ebv+NPbHI/jr9schz8K/uaPfXmfc0e+vNZ/HX7Y5H8dftjkhwr+5o99eZ9zR7683f8AHXH/ADHJzOqntjkpww7Ji1pms5iXDTXVFtk8I8F9tVLRjA6v6TXEeeExjsFewAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAu6b5IUrum+SBnf9a9Jmp89vL8NLLT57eX4Rxa/LUgRM4RlKJthzNuDkEzOUQhMAp1/JZdCjX8ll8De3mPOt3z5oTbvnzQ07Z4ABQAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABd03yQpMzHbE4niJtMzD2GWnz28vwo+9sxj0xnjnsT0kzN5mZzOEck0sltbpthxM5Jco8koAEJhBAKtfyWXQxbL217JtXlxTbq72j01r6Z45V7XS2yq7d8+aAV1zwAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOte2dU+qIzxhyCWZmGyvV6tkxWJ7Z8FrFp/ePNtZce+s1qvZvpqxF5xlNNldkeqk5juU9T3x5O+m/SfMLpOvZZPYpnrNUfrOZ4RErp7pYBfXpNibTacz3yA07JMcAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACEgO9U4vGeLdMdrzp4Oo37qxiLRj/Y7Uw8PZpdvC7qpj1RHCHXTfrMeOWSI4zmeMpi1qz6qTiRbpevX5b7dkTM8GAtfZs/eezhEYBfXpdfIAr1AAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf/Z";

    static final String IMG_4 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDowRjdGRjEzMzQyMDYxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDowRjdGRjEzNDQyMDYxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjBGN0ZGMTMxNDIwNjExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjBGN0ZGMTMyNDIwNjExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAIoAAQACAwEAAAAAAAAAAAAAAAAEBQEDBgIBAQADAQEBAAAAAAAAAAAAAAABAgMEBQYQAQACAQIEBA"
            + "MGBgIDAAAAAAABAgMRBCExEgVBUSITYXEy8IGRobFC0VJyIzMUwZIVBhYRAQACAgICAQMEAwAAAAAAAAABAhEDIRIxBEFRIhPwYcFC0TIj/9oADAMBAAIRAxEAPwC4AVYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMXvFIm1p0iBOM8Mivnu2OJ0iszHmm4stcteuk6xKZiY8tb6dmuO164iXsBDEAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABB3ncIwz0UjW3j8ExEz4aa9dtlutIzKcK7ab7Jmv0TXq/p5wsSYmPKdmq2qet/IAhkAAAATOjRXeYbW6ItGrbTd7fHlnHuNNJrrx5OczTSb2nH9Gs9PyXrXPl6Hrep+WJ75rxmsulbdjuc"
            + "Nr5MdpiLV/m8ldhy5ce39zLWdYj0zMfV5KO0zadZ4zPFNa+VtHqd5vFp/14iYb957fv39r6Oqel72Oe2HJpGs1n6oj9UV0v/rnt+1fT/J1cfl4NLcQ9P2JimmYtHaPD2Nm6mlM3RE8Zjq0a3O+bmJjyACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAHnJ7nTPtVm19OEQ5vJS9LTXJExbx15uv7Xlx5q3tSdZiemVD3zNTNuf7fHpiKzPxa044ev6MzS065r58z9GOz7zFtslve+m0aa+Sdi3tdxltFPp/b8fNz6Vsa5LZI9qNbRxWtXPLp9r1q2i23+2F8Hzb9vr"
            + "rM1jW0RwYPAaBti2XLGu4p0erSJ84es05otFIxx7fHqn+UThoZrWbTERzlhjFvsO3zdOWdPTrEkRlNKTecVjKl7ptr7fPMZJi029UTHkj222WtPcmlop/NMcErfb6u63MZtPRWYiInxiJ/5X+77ngnbWvjtW0zGkV+fwdGZiIe9O3Zrrqr0zM8Sl"
            + "bLLO4xVzTHTrHJyPcYrG5yRTl1TyWv/wBBFcEUpWfdiOnX9sfFQTMzOs8ZlFYxKvq6bUte9o6x4iHvFivltFMcTa0+EPdb5tnk9OtLxzS+z7ym0zTbJ9No6dfJju+7x7rP14vpiIrr5rfOPh0za07PxTT/AJ48o9cufcZovEzbLMxo6K1LY56b8/"
            + "HRzODLbDkrkp9VZ1hdbfuV97knriI0jhopeHD72qZiLViOtISgGLxQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB5z7XJuMN/anSY/P4PSJXcbrNnvtMFois85mOUacVqxy6NFJtbtGI6fdOVJXJan0zMa89J0WP"
            + "ZNlTdZp9yNa0jXTzmUffduy7G0RfSaz9Noe+27rJsrzmrWbY/pv5fi3nmOHv7J76rW0zzbxK67z2/D7E5aVitqeXDgruwZKxntjt++ukfcdy7zO8p7WOs1rztrzlVUm1bRNNeqOMaKxE4xLHVpvOi2vbxM+P2dZuMfRLVFprOscJU+bu2fcVit9I"
            + "6eOsRpM6Lenr008dPzZWjDx92m2qY7fKXk1vniJ5cGnJlveZiZnTXkmzj/AL9beEQrrc5+arGWELLsYz7mlr/454W+7+KaJicLa9ltU9qecYVve9rhwXrXBXSdPVEcvgq4pPHhP4O6vaa9No0ikcbzPlp4FZ9ydY0nHMcNOerSLvRp7k1pFJjMx8"
            + "5UPZO24c9Jy5o6p16YrPgr+7bfHt9zamL6dInTy+DrK44pk0rw1jih5+2bLriMkaXvM6eqdZki3OTX7WNk7L56z/VyJETadIjWZ8IdZfsmypHVaNI85s34O2bfaWjJjiYty1181u8Oqfepj7YnKn7Jmwbabxn9OXWIjq/Rp3N7bDd3maxpf1aRPh"
            + "K17vtYtivkxVjriNb3nnpHk5e2Sb21vM2n4z4Ec8q6qxvm2yfFoxav+HSUvGSsWrxiXppnfbW1qY9vGkTHLTTSfJuYzGHjbKTScTEx9MgCGQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA0UmdruP9msTato6bxHP5"
            + "w3sxE2nSOMpicNKXmk8fPEwjdy3f8A5KsYNrWbTX12mY0/Vo2vcse32l9tkrPX6o0056+fydLjw1rWImI10YnbYpnWaVmf6YXz8O2u+vWNc0+2Jz55y4KIlc9jyVxXtHDrtEdM/q6WNvijlSv/AFh7ila8oiEzbLXb7c7KzTriJc9v9pbcZazSOM"
            + "/XOn24rTa7WaR1TwmI9MSnMqOK1ptERPivhBjPn4zetYrHjE6oSXvM3VPRXlHP5oissLDzkiZjSvN6BV6xb+2Ont5adURwbKdzx446a0mI+5pDK3aW/Dv/AHc8cNInhzT8lIn+5MazWJmvDkocmK2vVVIp3HPjjS0RPzhOVot9UzbWjfYZjPWJjV"
            + "M6OGk8dFVXus1jSKR+LzbuuWfpiI/MyntCHuN5utzTNhx01rEz1W+Hl9vB6wZdj/o9N+nq6Z1j93V8EradWT3YtEz1xrMR6dZ5c2Y7HtJtpEW0059f5NImHdTdrmvWYmkROY6/yrdjtLYNvO/i0dVYtpWY1jy4/FPpMzWJnnMQ3bjYYcNa9NeXD7"
            + "fH4tSlpy5vY2/knP6x9ABVygAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANWXLNJ0hO2m+w6RW3pt+U/eizWLcJa5wVnlwExOF/ExMaxxhlz9KZMf0WmG339zH7/t+Ccr9l2KSNzuo/dH5E7ncz+6I+RlPaF1a0VjWZ"
            + "0hWbnuUR6cXHzt/BCvTJk43tr8yNvHjJlWbNtbRaNYZYrEVjSGUKAAAAAAGgAMRN6W66Tx5aTye6bjJjnWlKRM+UPIJyxlvlzzE5JiIjlEMgAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf/9k=";

    static final String IMG_5 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpCODQ5NDA2MjQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDpCODQ5NDA2MzQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkI4NDk0MDYwNDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOkI4NDk0MDYxNDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAHkAAQACAwEAAAAAAAAAAAAAAAADBAECBQYBAQEBAQAAAAAAAAAAAAAAAAACAQMQAQACAQIEAwYFBA"
            + "EFAAAAAAABAgMRMSFBEgRRYdFxgZEiMlLwoUITBbHB4RTSYnIjM0MRAQEBAQEBAAAAAAAAAAAAAAABEQJRMf/aAAwDAQACEQMRAD8A7ADq4gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA3x"
            + "47ZJ0ruzjwze0VmJ6ZmeMbcPNmtxGl/wBfJx4cYjWPP3+qzHZ14RbWdJjTlPv8U2TNSs9MfNb7a8Z/x703pU59R/6tZpFZ4eOnP3+jl2yaW/brW1sn2RHH38ojzdfoyZPrnoj7a7/H0+KSlK446axoyVVkcz/U7itYvMRM86Ry9k8/yRRbX2xvE8"
            + "nb0mVHvsP/ANq8vq84/wAf0bKm8+KYC0AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEVte3RTTXTqmZ5R+OTAnhxlZwYK5MfXvM/Tx0hJh7HFpF5n9yd4mfp90bf1T/v1i0468bR+nx9nJNq5z607Xt"
            + "/2tdbazPlpo3vmrS3TGtrfbX8cPex+3fJxvPTE/pp/y9NElKVxx00jSPCErR9GTL9c9MfbXf329PikpSuOOmkaR5N+MkRoBxk00ZY1BliZNNdwHI7jBPbTMxGuLxj9PlPl4S0iYmNY2dqY1c/P/H6TN8HCedJ+mfSVSovPiqNYtrM1mJraN6zu2W"
            + "gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAX+xxdNOufqvx93L8ebk581YpaI4zpL0FLdVYtHONUdL5Q2xTSevFwmd68p9J8/iY8HXj0yRxtM2nyn2+SwJWr47Wrb9q868628Y9YTxGiPNj668OFo41nz/"
            + "G7OLJGSvVt4x4SCRiZ0NzQDdjbZsA1teKRrM6HUq0pW2S3XxvG2v28tPxulzWmtJtExExGoJxrSeqsTtrDYEHcdrTuI+beNrRvDl5sd+2/9nGvK8f38P6O2xMRMaS2XGWa4osZ+wnH83b7c8c7e7w/oq1vFtY2mN4neFy652Y2AawAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABFly9HCN1e1ptvKXLinXqjigYExrweh7C/X2+Of+mHnlnte+ydrHRp1UjltMeyfX4ssVzcehFbt+9xdx9E/NzrPC3wWUOggvitWZyY9/1Vna3pPn8U4CPHmrkjhvG8TvCRDlw9c9VZ"
            + "6bxtb+0+MGLNrPReOm/h4+cfjgCYAGmTFXJ9Ua6bI/9am86zp91plOAAAAAKH8jirpXJ+qLRGvlPJfc/v81bTGKJ1tE9VvLhzbGX4qAOjkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIcmHXjXdMAoTw3F6axbe"
            + "NUVu3ifpnRgqzETx5xtPNdwfyeXFwyf+Svwt6T+StbFau8NDGy49H2/d4u4jXHbWY3jnHthO8ppxi0cJjaY4TC/wBv/KZcXDNHXX7o+r0n8k2LnTuI8mKuSNLe6ecT5NcHc4+4jqx2ifHxj2wmSpXjJbFMUy7T9N/H2+E/lP5LDFqxaNLRrE8lf5"
            + "u38bY/jNfWPzjzBZGK2i0axOsSyAAAxro0y5a4qze86RDlZ89+54T8uP7ec/8Ad6fFsmstxN3HfTk+TBOleeT/AI+u3hqrVrFY0hkXJjnboA1gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1tjrbeGwCvbt"
            + "5/TKK1ZrvC6MHP2t1VmYtG1o4S6GD+WyY/lzx1R99d/fHp8EdsNbeSG2C0bcSxsuPQYc+PPXqx2i0eSR5aItS3VWZrfxjhP+fe6OD+XtT5e4jWPvp/ePRFi506M47Ypm2Lad6ePs8J/KfzS48lcka19/l7WMWWmavXjmLRPOGnCM/DnXj7pYpOrd"
            + "z3de3jTe8/TWN5Qdz3+kzjw8bc7cq/jwUq10mZmdbTvad5VIm9M2m2W37mWdbco5V9nqyC3MAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABiaxbhPFFbt4nbgmAVaYcmO3Vjnot91f7xzWb5c+W"
            + "Yi0xXh0zavP0ZGY3WK1isaVjSGQawAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAB//Z";

    static final String IMG_6 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpDRDQ2NTJFNjQyMDQxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDpDRDQ2NTJFNzQyMDQxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkNENDY1MkU0NDIwNDExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOkNENDY1MkU1NDIwNDExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAIgAAQACAwEAAAAAAAAAAAAAAAAEBQECAwYBAQADAQEAAAAAAAAAAAAAAAACAwQBBRABAAIBAwIEBQ"
            + "EHBQAAAAAAAAECAxESBCExQVETBWFxkSIyUoGhscFCYhXR4TMUBhEBAAICAQIGAgEFAAAAAAAAAAECEQMSITFBUWEiMhORBHGx0eFScv/aAAwDAQACEQMRAD8A9CAwtgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAj5+XTDO2dZt5Q25Ob0qax+U9KotqVpWIjreetrI2nC2lYnrbszPuUV70tCRh5mLN0rPXylXRlreZqiZ8Xp21"
            + "jsri8tP00t0+MvRikwe45MXS33V/es8XOw5e1tJ8pWRaJZ76b08Mx5wkBSYvMViYnV3vx9I1iU4rMxmGeZiJxLgA4kAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAyDBMjG2bzpCF7TWuYjM+ECr5eXJ"
            + "lzenhrN5r5N49r5l41vemOPqn0tnvE0xaY6xPXJPWZ+Ufzlv8A4vDm65ptln+60/wjos164vWLzGZt1WTv44rGK49Mz/ZV/wCDyd6ZqTPy/wB0bmcXlYK7s1daR/VWdY/avZ9l4desY+vwmf8AVFnBM78PGy6TEaWx3tviNfOJ6wsnTHl+Ha/tzm"
            + "M2z/1GP6Kbj4LZ9Zp4NL45rbb4wsPaONfj5L4MsaTrCzrirS0zpG7XuwRFvsmk9mq37HG0xHWPBV8Hh5q3rl/HTr1X1s+saRHdxYaq5rGI8WLZb7LcreHYAEQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAGL22Vm0+Cp5HLyY5id07vLwW1qxaJie0qLmxMXiLR90d/irvlp/XiJtiVhHuMTFZ2zpPSfmk3yWrbSI7wqa0mcdZ0/Cd1o8dPNdxbdEWjtKOPsjjlHbWtfjHmZ75cfDtfDGuWI+2J81f/wCY9x5HMw5LcqddltIv2+f0WODnUm3pX6ddIme0ps"
            + "UrWNsRER5Q9DXNZrEU8OjzrZz1Q8XueHPkjDXWJtE7Z81X7Z7Ry8XOvy+VbdP4xaJj7q6aR00S+Pw+Nxs3qY4vN46VrMW0r8un8VnfLFK7rTER8Uq5x7k9kVzH1xOPVF5NJnSY8JccMWiPv7k82mS8UrrMT2t4OjHeK22Tsr18FtMxSIly5Gb0KT"
            + "eY10Rb+5RSmtq/f5NudFssen2rGkzaeyr5Vt2RVa3XENunXW0Ry791vxuVOWYi0R1jpolK/hYtNunWK9Zn4rBOucdVGyIi2KgCSsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcORx4zRE/1R1iXccmMux"
            + "MxOYRMfHtuiZ6RHb4a94+SdSms6T4NdNejrWtotp4Oa6cbZxM1nuhtvOPV5/NMUnZljxnX4O9c2TTZW9tvwlb8n2/HyY++Pu/VHdUZsMcfJNNdZrEfRXt1W1daz7fR2NkTSta8omuZtiM9/L8sY+TyMesVyTMT+rro0yX3xM5bTf5ouSta1mYt38"
            + "Fnwfa4y4t1rRtt+lCsX2RxiZldPGNf2TPe0dePGcZhy4URkvXZHSJ/dC3vTrpDbFx6ceNtI0hmcdZnWW6NFq040xNpnLFbbHObRmK9UPPi9TTbPbxlFjgxktuydo+Osz85WNo2zo1Zo1xE9e7VXZaI9ssViKxpWNIZZYWIgAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAN6fklY48UWnSyXj7NWr4z/LNt+UN3nefes5smvaNKz9F7n5GPj1nJlttrDyuTnYs02m2sRa0+DP+18YiPNdopac3itrRH+vdvhpjmN0R9W/F92/62SaxE2x/2umbgVngzmprEx"
            + "rb5w7+3YsN8FaxEbvNRr12raLz49vRqrFONtmybbIzxxbvX/K043Lxcym7FPbvHjDZT8KPT50Vp2mJ3Lme709dsx1YP2Nca7e3tMZhxy93N0yz1c2S/wApW0+MACCYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAADKTjv4oraltsrdd+M9e0q9leUdO8K73WYyc3HTJ/x1jdp56t+bw8XpzasRpKTzeFHLitqzpev4z/KUX/H8jN9uW9a08dvVK1J5TMRnLRr21409/Dh0mE72ufU4lYtGsaTX9kdES3s+XHMxx8kRSfC0dvlK1w1phpGOnStY0htOTyW8M"
            + "xiWX75re1qdrTnCHw+DXia2md2S3eyRM+JM+bje+vSHbTGuPVV7ttuVurWZ1nVgGLu1gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMxaY7OkZfNyEq3tXtKE0ie7t6sMTl8nIT+2yP1VbWtNu7"
            + "DArmc91kRjsAOOgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP/2Q==";

    static final String IMG_7 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpCODQ5NDA2NjQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDpCODQ5NDA2NzQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkI4NDk0MDY0NDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOkI4NDk0MDY1NDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAHkAAQACAwEAAAAAAAAAAAAAAAABBAMFBgIBAQEBAQAAAAAAAAAAAAAAAAABBAUQAQACAQIEAQoFBQ"
            + "AAAAAAAAABAgMRBCExEgVBUWFxgZEiMnITBsHRQjM0seFSgjURAQEBAAIDAQAAAAAAAAAAAAABESESMQIEA//aAAwDAQACEQMRAD8A6UAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEWtFI6rTpEeMgkY8WambjjtFtP8AGdWQARExPI10FypEa6pEAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFbfUjJgvW09MTGnVKyp9z/i5PlBX7NgpgpeKZIyazHwsm8yzNuiOUKf25+3k+aFve45rbq8JGj8JO/KvW01nWJ0l7y5rZZ1ljEdDrPOMmHLOK0T4eMLm932PZRFskTMW4e6pYsc5LR"
            + "Vn7xg+ttbac6+/Hq/srF9GbMZsG+x58M5669Ea668+DFtO64t5f6eOLa6a8Yj83P7XdfT2mbF4zNdPXzXOz1nDt8258dJivq4jI2O771g29ppxvaOfTyh62fd8G6t0RrW3kt4+hz/adrTd5+nLxrETafO8dwwxs91auLhETW1QdJu+64dpf6eSLT"
            + "OmvCI/Nly77HiwRuLRPROnp4ud73frz1t5aVn26r+//wCZj9GMGy2W/wAe9i044mOnTXq8620P238OT01/FvgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFPuf8XJ8q483pXJWa2jWs84Bpftz9vJ80f0"
            + "bq9IvHTbk8YdvjwRMY6xWJ56MosuNVmwWxTx4x5XimO2SdK823mImNJ5IpStI0rGkDVPovXLOWPDgjFHn8rLasXiazxieHtSqdyplvt7Rh16+GmnPmMt9r7Xa47PinDktjn9MzX2S67ZbWK7SuG36q+9/tzaTadm3GXLFs9dK662m083UiONpObt"
            + "WfqmvGOHHlMJrjzd03E20+KfenwiHYTEW4TGsERERpEaQDm+/7a1b0yVj3Onp4eWFG+6z59vGKf28fjEfi7KYieEoisRGkRGgNH9t/Dl9Nfxb5GiQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf/Z";

    static final String IMG_8 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDowRjdGRjEyRjQyMDYxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDowRjdGRjEzMDQyMDYxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkI4NDk0MDY4NDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjBGN0ZGMTJFNDIwNjExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAJMAAQADAQEBAQAAAAAAAAAAAAABBAUDAgYHAQEAAwEBAAAAAAAAAAAAAAAAAQIDBAUQAQACAQIDBA"
            + "YHBQYHAAAAAAABAgMRBCESBTFBUWFxgZGxMlKhwSJCEzMU0WKSFQZygrIjQ1Pw4cJjc4OTEQEAAgEDAgUFAAAAAAAAAAAAAQIRITEDEgRBkSIyE1FhcVIU/9oADAMBAAIRAxEAPwD7EBmkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAEicCBKAAAEohIIAVABICRIjiJDAAJxAgSIwIEgIEgIEhiBAlBiAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOeXPjw158lorXxtOgOg81vF41rOsT3w9AAAATOgAzc/XNrhy1w83NabcluXspP73g626rt6567Wbf5loiY07OPZx"
            + "8+5IumsMuvX9lfLGCl9b2nljSJ019LP3W1vuOo1pk4zFvxKzFuFcdNO75rX7ZMD6QIEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAzOtbH9Zt5isa3pPPSPGY7vXDQyZK4qze3ZWJmfRD5nJ/Uua082"
            + "DFXk7ue3GfZ2JVtMRHqZnTepW6bki0TM7e86Xp8s+MfW+7raLRE14xMaxL86327i2Sc34U05/zKxOtdfGJ8+9p9N3O43O2jBXJNcVJmvNHx2jujXu07OHFF7RWOq2kI44mfTu+pz9Q2+3nTNkrWfCZ4+xyjrOynszU9rHw7HBh+Cka/NPGfbLvbH"
            + "W3CYifU5J7qudKzLrjgnxlu0y1yRzUmLR4xOqtuN/TBkphnW2TJP2a18PGfKGFGz/Bt+JtbThv+78M+mvY1um7+NzaaZqxTcUj7UeMeNZ8Jb05a8mzK9JruoYugxmtuIz8K3zRkjT71Y46eji97foV8dsVrXj/AC7azER21rMzSPVr7G/2jXMqPn"
            + "7dBjHSJxcv41c34sWn5ebXl9jWyYpi8ZccRzzpS02+Tt9vgtMXrW6z4r48OKZpS8Wm+StOe0cvhBuNibxWNbTER5q9+pbWnxZqR/fhh48fSLaWz5fxb9857W906R9C/iz9Jx/BOCP4TAsX6xs6RrOamnlOvueP57sf92PZb9j3/NNjXsy4/VaD+b"
            + "7S3CuSLeVIm3ugHvD1TaZ5iuPLSbT3a8fYuQzM+Om+pNIwaxP3steWI/6vd6XH+nqZMeDJjyTM8uS9a2mddYidAbICAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAmNWFm/pvBa02x3vjiePLXTl+mG68ZLxjp"
            + "a9uysTafUImInd8puOlbeuaMFZteaaXyTa3j8NdI0jj2+j0r1axWIrWIiI7IhT6ba2altzf4s1rX9Xd9C88zuLza818Id3FSK1AHM2FbcxbHy7nF+Zi+1HnX71fXH0rIvS01tFoVtETGJbWDLXNSuSnGtoi0et0ZfQraYLYZ/0r2pHo7Y97UezE5"
            + "jLz5jGgy+p25NztLf9y1f4qy1GT12eSmHL8mbHPvhKGpNKz2xHseZwY57a1n+7ClbqFu6IeP1+SfBzT3FGscVpXo22KOylf4YdYrFezgy/1uXxj2Eb7L5exH9NE/DZqKHS+EZq+GbJ9M6/W5/r8nk89HyWvfca/wC5r7aw0py1vpVS1JrrLWAaqA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACvvcU5tvkx17bUtWPXCwiQfNbG0X2+OY4Ryxw9Cw5ZMP6LcWxT+XkmcmOfP71frjye70rkrNbcYnhLyeWvTec7bvQpOa6JS8zpWvlEKkY8+5+1e04qd1K/F657vRC"
            + "sVidc4haZ8114tSLTEzM/ZnXt97xi22PDxpHHxnjLxuJvmtG1wfmX7Z+Svfafq800r1XitPNFpxGbNDoP26Zc3dfJPL6KxFfqbDhtNvTbYq4cfw1jSHZ68RiMPPmczkZfX667S0/LalvZaGqz+tV5tlmj9yZ9nERG6gIidY1S8Sd3pwAIB16Jxyb"
            + "n+3X/BDksdErGme3jln6Ih2dr7rfhhz+2GsA9BxgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK+72mPd4/w8nZ2xMdsT4wwctsuynl3UTNY7M1Y+zP9r5Z+h9NJp3KXpW8YtC9bzXZ89jy0yxrS0WjynVN71"
            + "pGtpiI85aOXo+zzTzXxV18Y+z7tHrB0ra4J5qYq6+M/an6dXN/LH7aNvn+zMxUy7nhhjSs/wCpaOHqj73u82ts9jj2lZinG1uN7z8Vp/47u5aHRTjrSPT5sbXm25EaANFBV6jXm22WP3L+5actxhjPjtimZiLRNZmPMGFgtzY6T41ifodHP+X7za"
            + "V5KRXPSOFZ15b6eevCXOc2Wn5mDLX0V5vdLzL8F4mcRmHdXkrjdYFb9ZXvpkj/ANVv2H62ny5P/lb9jP4r/rK3yV+qysdBnXHm/wDNf6lKmTJk+DDkn005f8WjQ6Jgvgw2/FjltbJktMev/k7O347VmeqMMOa0TEYlpiUOtzAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABoAGhEaAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD/2Q==";

    static final String IMG_9 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVG"
            + "N6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj"
            + "0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly"
            + "9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIgeG"
            + "1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpDRDQ2NTJFQTQyMDQxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDoxMzk3MUFDQzQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvbS"
            + "BzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkNENDY1MkU4NDIwNDExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOkNENDY1MkU5NDIwNDExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0aW"
            + "9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgHC"
            + "AmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAHQAAQADAQEAAAAAAAAAAAAAAAACAwQBBQEBAQEBAQAAAAAAAAAAAAAAAAEDAgUQAQACAgAEBQMCBw"
            + "AAAAAAAAABAhEDITESBEFRcSIyYaGxExSBkdFCYnIFEQEBAQEAAwEBAAAAAAAAAAAAARECITESQTL/2gAMAwEAAhEDEQA/APMAec6AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAW9vqjbfE/GOMiyalp7adnutOKtfRqp8K5nzsTOZGkmNZMSi0eNazHojbtdW74ey32DOOMKrBs1W1W6bxxQett1x3OvH90fF5LOzGXXOegBHIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA3djX22t5zj+UMLf/z5ia2rPn+YwvPt1x7SicuoVjEzE84lNo1HHXAW6JxZ5/dVim28Ryy9DRGbPO7m/XttaOUynXpz3/MVAM2QAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAs0bf0rRM8uUqwWXPL1ttYv745+OPyqZNPczr9s8a+X9Gqu3XflaI/24NJdayyuuxGeSWKRzvXHqjs77XrjGuOqfPwXwvj9T27P22v/ADtyeUls2W226rTmUWdusurtAEcgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAANOrt69MbNvKfjWPFb0a+XRGPWc/lZtn3Y8I4R6K2mNsjNu0/p8YnNZ5Km7ZGdNpnzrhhcVn1MoAjkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE9Wm+2emkZX/AL"
            + "KeU3jP8VyrlZRdt7W+qOqeNfOFKGWAAjZp3UvWK3nExwifCV068cbWrEerzR19O/to7nfF8Up8Y+7ODm3XNu3QAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAdpWb2isc5nABHo4ikdFfjH3+rgNY3T"
            + "peaT9PGGPu9Mar+3424wDnpz1meWcBwyAAAAAAAAAAAAAAAAAAAAcdAUAEAAAAAAAAAAAAf//Z";

    static final String IMG_10 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOV"
            + "GN6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZ"
            + "j0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6L"
            + "y9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIge"
            + "G1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDoxMzk3MUFEMzQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDoxMzk3MUFENDQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvb"
            + "SBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjEzOTcxQUQxNDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjEzOTcxQUQyNDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0a"
            + "W9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgH"
            + "CAmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAH0AAQADAQEBAAAAAAAAAAAAAAABBAUDAgYBAQEAAwEAAAAAAAAAAAAAAAABAgMEBRABAAICAAQCB"
            + "wYHAAAAAAAAAAECEQMhEgQFMUFRYdEiQmIT8IEyUnIzcZGhweFTFREBAQACAgIDAAAAAAAAAAAAAAERAhIDIUExcUL/2gAMAwEAAhEDEQA/AOgDteMAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAgAKAAACAAoAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAACAAoAAAIACgAAAAAAAAAAAgAKAAAAACAAAAoAIACgAgAKAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAgItaKxmeEGcTNbRi0eMJlli/KQFYgAoAIACgAAAAAAAAAAAgAAAAAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAIACgAgiZxGZ8ErPb+np1O2fqcYpi3L5TPr/gxtxGemvPbi7du6Kdkxv2xiscaVnz+b2OneO"
            + "kiIjqK4ry55oxxt/lr+DO7zfGmIj4rVhzy27ZehdJrpYxgHS80AVAAUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEABQAAAAAAAAAAAAAAAAAQAQBGfKImZ9EIm3LPLaJrPotGEzGXG4zh6AVBe7P+/sn5YUJ4cZ4NbsumYpbdbh9T"
            + "8P6Ya+y+HR0a27Zayh3bVzdNbHw+/8AyaGVbuF4p0+yflmGiO+zMfNRsrM4iXpb6/ZomldGJjbSK+9WPCceH3+anGZjM+Lp1uXm9unG+EgM2kAFABAAUAAAAAAAEABQAAAAAAAAAAAAAAAAAAAQAFAAAAAAAAAAAAAAAAAAABAAUAAAAAARMZjCQ"
            + "R61b92nhrtGI8pr9uLtPcN14xurTZX1xhXGF1jbOzaOk36a2ZtW+qZ/LPNDpq6TXu/b3xM+i0YV3m1K28YTjfVZTsn6ka+nstImLbbTsiPh8K/b72jt0xenJmYj5OD5inNqnOu1q49ftaGnvGzX7u6vNH5qexr21rp07dPpe+h1Wvjr2xb5dlf7w"
            + "q9dv3Tq5d2qaxmszas80Yj+rrTu/TW/FM0/VV3/AOl03+yuGGK3cpYwJ2zutbdPxznHq8ho779t2zNuaK285rmGZstWlois80TOPHMt2u3pw9nXc8pXoBtc4AKAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAIACk8fFHLHohImDNRMR6IRFYjwh6DBmgCoACgAAAAAAAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAgAKAAAAAAAAACAAoAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAgAKAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAACP//Z";

    static final String IMG_11 = "/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAAKAAD/4QMsaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOV"
            + "GN6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjUtYzAxNCA3OS4xNTE0ODEsIDIwMTMvMDMvMTMtMTI6MDk6MTUgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZ"
            + "j0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6L"
            + "y9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoTWFjaW50b3NoKSIge"
            + "G1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDoxMzk3MUFDRjQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDoxMzk3MUFEMDQyMDUxMUU5OTk4RkJCNjMyNTY5RTk2QSI+IDx4bXBNTTpEZXJpdmVkRnJvb"
            + "SBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjEzOTcxQUNENDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjEzOTcxQUNFNDIwNTExRTk5OThGQkI2MzI1NjlFOTZBIi8+IDwvcmRmOkRlc2NyaXB0a"
            + "W9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+/+4ADkFkb2JlAGTAAAAAAf/bAIQAFBAQGRIZJxcXJzImHyYyLiYmJiYuPjU1NTU1PkRBQUFBQUFERERERERERERERERERERERERERERERERERERERAEVGRkgH"
            + "CAmGBgmNiYgJjZENisrNkREREI1QkRERERERERERERERERERERERERERERERERERERERERERERERERE/8AAEQgBaAKAAwEiAAIRAQMRAf/EAHgAAQACAwEAAAAAAAAAAAAAAAAFBgEDBAIBAQEAAAAAAAAAAAAAAAAAAAABEAEAAgECBAIGBgkEA"
            + "wAAAAAAAQIDEQQhMRIFQVFhcdEiMhORobHBFQaB4UJSYnKCkhTxIzNDU2MWEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCfZBQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABry5aYazfJMVrHOZBsEDm/MVdZjb45tH71vdhor+Ydxr7+Okx6LSIso4dj3PDveFJ6bxzpbn+t3AACgNWfPTb0nJknSsCNorOfv2fJP8AsVrSv8cazLXj75vKTrbovHlp0yC1CP7f3"
            + "bDvvdr7uSOdLc/0eaQFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaN3use0xzlyTwj6/QqG73WXf3683CsfDj8I9fpdHdd5/mbia1n/axTpX028ZcYgDfstrbe5vk1t0xEdU25g5rRMaXrOl68ayuXb"
            + "91/l7embxtHH1+Ko5cc4ct8UzrNLdOseKe/LczO1mJ5RewJoAVhVe87mdxuZxRPuYuHrt4ytahTkm0XyeM2vb6wex173b7bDhxZdvk6r206o111/R4aOQRiYmLRek9N68a2ha+1dxjfY9Z4ZK8L19Pn6lVesG5tss1dxXlHC8edQXgeaWi9YtWdY"
            + "mNYl6FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHL3DPO32+TLHOtZ09bqRvfI12OXTyj7QVTHXSsRPPxeyAQIma2i9Zmto5TWdJBRjhWJmePOZmfFZ+w4px7KkzztNr/TKtY8Ft3lrtqc7fFPlXxXbHSMd"
            + "YpXlWIiP0IPYApzUOkTSbVnnW1o+tfFQ7ttp226tP7GX36z6fGBHHFaxOsRESyADEx1RpPiyxa0ViZnwBZvy/ltk2dYtzpM0+hKo7smCcGzpFudtbz/AFJEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAad"
            + "zhjPivin9qs1bmBFFrE11pbhas9Mx6mVm7j2em7n5lJ6Mv73hPrhCX7VvaTp8uL+mlo+8HIaTa0UpHVe3CKw7sfZd5lnSYrjjzmdZ+iE727tmPY11r72SfivPP9UA8dq7ZGyrNr8ct/in7od+TJXHWb3nSsRrMy9tebFXNS2O8a1tGkiuCO+bW0z"
            + "FJtbSNZ6aWnh9DX/wDQbaeNYvMfyo/P2LcYNZwW668tNem2nl5S4Me3y3vOKmO03r8VdOXrBZNv3nb58kYo6q2ty6o5uje7Km9xTiyeuJ8p80Z2/sl6Xrn3ExrWda0ry19Mp0FJ3W2y7G3Tnjh4ZI+GWmL1nlML3asWjS0ax5S4rdo2dp1nDXX1A"
            + "qFstK85d/be2X3t4yZazXBWdePOyyYu37bDOuPFWJ9TqBiGQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABhkYmdOMgNebcY8EdWW0Uj+KdETbu3+Xn/xdtaKRPD5tvH0Vjzbs+023bsN9zePmZIj4snvT"
            + "NgbZ7ztI+G/V/JE2+w/E4v/AMWLLf8Ao0+3RAdr7jbt95+Z72K/G2nOtvOPQmO6d3xVwTXBeL3yRpXpnlE85lA2+/3Hcon/ABYrjrE9NrWnW0f0x7Wz8MvtZ+btba3n/ljJ+36fRP1K3s9xfYZIy4ePDS1P3o9qT3/fJ3OP5WCtqdXx2tziPKAdm"
            + "271bPFunDe00npt8uYtH3N2Tus4qTe+DLFYjWZmscPrV/t27/D88X0n5do6LxH1S6O69znfT8vHExhjjOvCbT7AS2Du1txSMmLBktWfH3fa2R3bFSdM9b4p/wDZXh9Mawi+wb2MGS22vOlbz1UmfPxh39z7zjwxOHFpkyTz8a19fsBJ1tFo6qzrE"
            + "8ph6UzZ77LsJ6qz1Umdb08PXXy9S34c1c9K5KTrW0awo2AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIP8AMO7tSldtSdJyazaf4Y9qcVb8wTNd3SbfD0Tp9IIyccadPl9Tdl3GfcVrTNeb1pxrE/fPi"
            + "922e4rgnczTTHHHj8Wnno0c+IgxFYjjEaMfMrPiza0VjWQZG/Psdzt8UZ8tNKTz0njXXzc8zpp4zPCIjxBkbdztM+0its9YiLeMTrpPlLXFbXtFKRra06VgHm1YtGklaxWNIjRv3ezzbKYjPEaW5Wry18paqY7Zr1xY/ivPTHtBhOflvLM0yYZ5U"
            + "trX1W/0c3c+zTs8fz8NrWrX/ki3l5w6Py1jmaZc3he0RX+n/UVPMgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAj+57KdzWt6RE5cU9dItyn+GfWkAHFg7hg3tLYrT0XmOm+O/CY9qudr7dbeZZxX1+Vi"
            + "mYvb97SeEe1Z9zsdvup1zUi0x4+Lk/AtpHw1tX+W9o+9B073t2PcbadvWIrpHueiY5K52bBXNn69xMVrh8LTzv+pNfge0njMWn15LPdeybKv/VE+vWQbt1udpmx2xXy00tEx8UK52KcEZpy7i9Y+V7tNZ5z+8sVe2bSvLDT+2Hr8P23/ip/aDn32"
            + "62W7w2w2y096OHvcp8EX+XvkVmc+e9YyfBWJtHLxn9P2JyNhto5Yqf2w8z23aW54af2iPe7pg32G2GbVmLRwmJjhPhKvdhpFdxfJnmK/Jjo4zp70z7I+tM27Lsp/wCqI9WsfY807Fsqzr8vX+aZkVr3G+t3DXbbKOqs8L5pj3ax46ecpHbbem2x1"
            + "w4/hrGjZSlaR00iIrHhD0oAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA//9k=";

    static final String[] IMGS = { IMG_0, IMG_1, IMG_2, IMG_3, IMG_4, IMG_5, IMG_6, IMG_7, IMG_8, IMG_9, IMG_10, IMG_11 };
}
