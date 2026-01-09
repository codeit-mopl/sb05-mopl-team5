package com.mopl.api.global.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        Supplier<CsrfToken> csrfToken
    ) {
        // 1) 응답에 토큰을 렌더링할 때 XOR 방식(BREACH 방어) 적용
        this.xor.handle(request, response, csrfToken);

        // 2) 토큰을 강제로 로드해서 "쿠키로 토큰이 저장/발급되도록" 트리거 확실히
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());

        // 헤더가 있으면 SPA 방식(plain)으로 검증
        // 없으면 폼 파라미터(_csrf) 등 XOR 방식으로 검증
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
            .resolveCsrfTokenValue(request, csrfToken);
    }
}
