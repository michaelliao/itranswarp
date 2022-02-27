package com.itranswarp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.util.JsonUtil;

/**
 * Generate i18n files for UI.
 *
 * @author liaoxuefeng
 */
public class I18NBuilder {

    static final Logger logger = LoggerFactory.getLogger(I18NBuilder.class);

    public static void main(String[] args) throws Exception {
        Path templatePath = Paths.get("./src/main/resources/templates").toAbsolutePath().normalize();
        logger.info("Scan path {}...", templatePath.toString());
        Set<String> keys = new TreeSet<>();
        Files.walk(templatePath).filter(Files::isRegularFile).filter(p -> {
            return p.toFile().getName().endsWith(".html");
        }).forEach(p -> {
            logger.info("Scan file: {}...", p.toFile());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(p.toFile()), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Set<String> set = extractKeys(line);
                    keys.addAll(set);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // update json:
        Path i18nPath = Paths.get("./src/main/resources/i18n").toAbsolutePath().normalize();
        logger.info("Scan path {}...", i18nPath.toString());
        Files.walk(i18nPath, 1).filter(Files::isRegularFile).filter(p -> {
            return p.toFile().getName().endsWith(".json");
        }).forEach(p -> {
            logger.info("Scan i18n file: {}...", p);
            Map<String, String> map = null;
            try (InputStream input = new FileInputStream(p.toFile())) {
                map = JsonUtil.readJson(input, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // find missing keys:
            for (String key : keys) {
                if (!map.containsKey(key)) {
                    logger.info("Add missing key: {}", key);
                    map.put(key, key);
                }
            }
            // find non-used keys:
            for (String key : map.keySet()) {
                if (!key.equals("__name__") && !keys.contains(key)) {
                    logger.warn("Found unused key: {}", key);
                }
            }
            // write map:
            try {
                writeJson(p.toFile(), map);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void writeJson(File file, Map<String, String> data) throws IOException {
        Map<String, String> map = new TreeMap<>(String::compareToIgnoreCase);
        map.putAll(data);
        logger.info("Update json file: {}...", file);
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            lines.add(String.format("    %s: %s,", JsonUtil.writeJson(key), JsonUtil.writeJson(value)));
            if (key.equals(value)) {
                logger.warn("Untranslated key: {}", key);
            }
        }
        // remove last ",":
        if (!lines.isEmpty()) {
            String last = lines.remove(lines.size() - 1);
            lines.add(last.substring(0, last.length() - 1));
        }
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            writer.println("{");
            char last = ' ';
            for (String line : lines) {
                char ch = line.trim().toLowerCase().charAt(1);
                if (last != ch) {
                    last = ch;
                    writer.println();
                }
                writer.println(line);
            }
            writer.println("}");
        }
    }

    static Set<String> extractKeys(String line) {
        Matcher m = I18N_PATTERN.matcher(line);
        Set<String> keys = new HashSet<>();
        while (m.find()) {
            String key = m.group(1);
            logger.info("Found key: {}", key);
            keys.add(key);
        }
        return keys;
    }

    static final Pattern I18N_PATTERN = Pattern.compile("\\{\\{\\s*\\_\\([\\'\\\"]([^\\'\\\"]+)[\\'\\\"]\\)\\s*\\}\\}");
}
