package banking.auth.service;

import banking.auth.config.ProxyRoutesProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Enumeration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {
    private final RestTemplate restTemplate;
    private final ProxyRoutesProperties routesProperties;

    public ResponseEntity<byte[]> redirect(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();

        String[] parts = uri.split("/");
        String serviceKey;
        if (parts.length > 2) {
            serviceKey = parts[2];
        } else {
            serviceKey = "";
        }

        String baseUrl = routesProperties.getRoutes().get(serviceKey);
        if (baseUrl == null || baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"message\":\"Unknown proxy route: " + serviceKey + "\"}").getBytes());
        }

        String prefix = "/proxy/" + serviceKey;
        String internalPath = uri.substring(prefix.length());
        if (internalPath.isBlank()) {
            internalPath = "/";
        }

        String query = request.getQueryString();
        String targetUrl = trim(baseUrl) + internalPath;
        if (query != null) {
            targetUrl = targetUrl + "?" + query;
        }

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if ("host".equalsIgnoreCase(name)) {
                continue;
            }
            headers.add(name, request.getHeader(name));
        }

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpEntity<byte[]> entity;
        if (body.length == 0) {
            entity = new HttpEntity<>(headers);
        } else {
            entity = new HttpEntity<>(body, headers);
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        return restTemplate.exchange(targetUrl, method, entity, byte[].class);
    }

    private String trim(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
