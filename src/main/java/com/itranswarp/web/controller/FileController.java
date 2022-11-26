package com.itranswarp.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.itranswarp.bean.DownloadBean;

@Controller
public class FileController extends AbstractController {

    String maxAge = "max-age=" + 3600 * 24 * 365;

    byte[] faviconData = null;

    @PostConstruct
    public void init() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/favicon.ico")) {
            this.faviconData = input.readAllBytes();
        }
    }

    @GetMapping("/favicon.ico")
    public void favicon(HttpServletResponse response) throws IOException {
        response.setContentType("image/x-icon");
        response.setContentLength(faviconData.length);
        response.setHeader("Cache-Control", maxAge);
        OutputStream output = response.getOutputStream();
        output.write(faviconData);
        output.flush();
    }

    @GetMapping("/files/attachments/" + ID)
    public void process(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        process(id, '0', response);
    }

    @GetMapping("/files/attachments/" + ID + "/0")
    public void process0(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        process(id, '0', response);
    }

    @GetMapping("/files/attachments/" + ID + "/l")
    public void processL(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        process(id, 'l', response);
    }

    @GetMapping("/files/attachments/" + ID + "/m")
    public void processM(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        process(id, 'm', response);
    }

    @GetMapping("/files/attachments/" + ID + "/s")
    public void processS(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        process(id, 's', response);
    }

    void process(long id, char size, HttpServletResponse response) throws IOException {
        DownloadBean bean = attachmentService.downloadAttachment(id, size);
        response.setContentType(bean.mime);
        response.setContentLength(bean.data.length);
        response.setHeader("Cache-Control", maxAge);
        ServletOutputStream output = response.getOutputStream();
        output.write(bean.data);
        output.flush();
    }
}
