package com.itranswarp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merge css and js from src/main/resources/static/3rdparty to reduce http
 * request.
 * 
 * @author liaoxuefeng
 */
public class MergeResources {

    static final String basedir = "src/main/resources";
    static final Path basedirPath = Path.of(basedir);
    static final String thirdpartydir = "/static/3rdparty";

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(basedir + "/templates/_inc_static.html"), StandardCharsets.UTF_8);
        List<String> cssFiles = new ArrayList<>();
        List<String> jsFiles = new ArrayList<>();

        lines.stream().map(String::strip).forEach(line -> {
            if (line.startsWith("<script ") && line.endsWith("</script>")) {
                String find = " src=\"";
                int n1 = line.indexOf(find);
                int n2 = line.indexOf("\"", n1 + find.length());
                String src = line.substring(n1 + find.length(), n2);
                if (src.startsWith(thirdpartydir)) {
                    jsFiles.add(src);
                }
            }
            if (line.startsWith("<link ") && line.endsWith(">")) {
                String find = " href=\"";
                int n1 = line.indexOf(find);
                int n2 = line.indexOf("\"", n1 + find.length());
                String src = line.substring(n1 + find.length(), n2);
                if (src.startsWith(thirdpartydir)) {
                    cssFiles.add(src);
                }
            }
        });
        merge(cssFiles, "/static/css/bundle.css");
        merge(jsFiles, "/static/js/bundle.js");
    }

    static void merge(List<String> files, String mergeToFile) throws IOException {
        System.out.printf("Merge %s files to %s...\n", files.size(), mergeToFile);
        List<String> buffer = new ArrayList<>();
        for (String file : files) {
            System.out.printf("  Merge %s...\n", file);
            List<String> lines = Files.readAllLines(Path.of(basedir + file), StandardCharsets.UTF_8);
            if (mergeToFile.endsWith(".css")) {
                for (String line : lines) {
                    // find data url:
                    StringBuilder sb = new StringBuilder();
                    Matcher matcher = URL_PATTERN.matcher(line);
                    while (matcher.find()) {
                        String s = matcher.group(1);
                        System.out.println("-- " + s + "" + "\n-- " + matcher.group(0));
                        if (s.startsWith("data:")) {
                            matcher.appendReplacement(sb, matcher.group(0));
                        } else {
                            Path src = basedirPath.resolve(Path.of(file).getParent().resolve(Path.of(s))).normalize();
                            Path dest = Path.of(mergeToFile).getParent().resolve(Path.of("font")).resolve(Path.of(s).getFileName()).normalize();
                            System.out.printf("Copy %s to %s...\n", src, dest);
                            Files.copy(Path.of(basedir + src), Path.of(basedir + dest), StandardCopyOption.REPLACE_EXISTING);
                            matcher.appendReplacement(sb, "url(font/" + Path.of(s).getFileName() + ")");
                        }
                    }
                    matcher.appendTail(sb);
                    buffer.add(sb.toString());
                }
            } else {
                buffer.addAll(lines);
            }
        }
        buffer.add("");
        Files.writeString(Path.of(basedir + mergeToFile), String.join("\n", buffer), StandardCharsets.UTF_8);
    }

    static final Pattern URL_PATTERN = Pattern.compile("url\\([\\\"\\\']?([^\\(\\)]+)[\\\"\\\']?\\)");
}
