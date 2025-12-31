package banking.auth.controller;

import banking.auth.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ProxyController {
    private final ProxyService proxyService;

    @RequestMapping("/proxy/{serviceKey}/**")
    public ResponseEntity<byte[]> proxy(@PathVariable String serviceKey, HttpServletRequest request) throws IOException {
        return proxyService.redirect(request);
    }
}
