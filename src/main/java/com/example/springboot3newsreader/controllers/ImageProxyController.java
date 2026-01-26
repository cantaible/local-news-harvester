package com.example.springboot3newsreader.controllers;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImageProxyController {

  private static final String USER_AGENT = "Mozilla/5.0";
  private static final int TIMEOUT_MS = 10000;
  private static final int MAX_REDIRECTS = 5;

  @GetMapping("/api/image")
  public ResponseEntity<byte[]> proxy(
    @RequestParam(required = false) String url,
    @RequestParam(required = false) String referer) {
    if (url == null || url.trim().isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
    String trimmed = url.trim();
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    try {
      String current = trimmed;
      for (int i = 0; i <= MAX_REDIRECTS; i++) {
        HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);

        String ref = referer;
        if ((ref == null || ref.isBlank()) && current.contains("qbitai.com")) {
          ref = "https://www.qbitai.com/";
        }
        if (ref != null && !ref.isBlank()) {
          conn.setRequestProperty("Referer", ref.trim());
        }

        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_PERM
          || status == HttpURLConnection.HTTP_MOVED_TEMP
          || status == HttpURLConnection.HTTP_SEE_OTHER
          || status == 307 || status == 308) {
          String location = conn.getHeaderField("Location");
          if (location == null || location.isBlank()) {
            System.err.println("[image-proxy] redirect without location: " + current);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
          }
          current = location;
          continue;
        }

        if (status < 200 || status >= 300) {
          System.err.println("[image-proxy] fetch failed url=" + current + " status=" + status);
          return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
        }

        String contentType = conn.getContentType();
        if (contentType == null || contentType.isBlank()) {
          contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        byte[] body;
        try (InputStream in = conn.getInputStream();
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          byte[] buf = new byte[8192];
          int len;
          while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
          }
          body = out.toByteArray();
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Content-Type", contentType);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
      }
      System.err.println("[image-proxy] too many redirects url=" + trimmed);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
    } catch (Exception e) {
      System.err.println("[image-proxy] exception url=" + trimmed);
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
    }
  }
}
