package com.itranswarp.web.controller;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class IconController {

    int cache = 365 * 24 * 3600;

    String maxAge;

    @PostConstruct
    void init() {
        this.maxAge = "public, max-age=" + this.cache;
    }

    @GetMapping("/avatar/{name}")
    public void avatar(@PathVariable("name") String name, HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        response.setHeader("Cache-Control", this.maxAge);
        PrintWriter pw = response.getWriter();
        pw.write(svgAvatar(name.hashCode() & Integer.MAX_VALUE));
        pw.flush();
    }

    String svgAvatar(int hash) {
        StringBuilder sb = new StringBuilder(1024);
        int color = (hash % 36) * 10;
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-1.5 -1.5 8 8\" fill=\"hsl(").append(color).append(" 50% 50%)\">\n");
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                int bit = hash & 1 << (y * 3 + x);
                if (bit > 0) {
                    append(sb, x, y);
                    if (x < 2) {
                        append(sb, 4 - x, y);
                    }
                }
            }
        }
        sb.append("</svg>");
        return sb.toString();
    }

    void append(StringBuilder sb, int x, int y) {
        sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"1\" height=\"1\"/>\n");
    }
}
