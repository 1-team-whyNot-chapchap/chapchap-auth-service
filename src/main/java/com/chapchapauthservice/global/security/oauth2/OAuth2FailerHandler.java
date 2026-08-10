package com.chapchapauthservice.global.security.oauth2;

import com.chapchapauthservice.global.config.SubServiceUriConfig;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OAuth2FailerHandler extends SimpleUrlAuthenticationFailureHandler {
    private final Pattern OAUTH2_ERROR_CODE_PATTERN = Pattern.compile("^E[0-9]{2}$");
    private final SubServiceUriConfig subServiceUriConfig;

    @Override
    public void onAuthenticationFailure(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AuthenticationException exception) throws IOException, ServletException {
        String errorCode = CustomResponseCode.OAUTH2_ERROR.getCode();
        String errorMessage = CustomResponseCode.OAUTH2_ERROR.name();

        if (exception instanceof OAuth2AuthenticationException oAuth2AuthenticationException) {
            String oAuth2ErrorCode = oAuth2AuthenticationException.getError().getErrorCode();
            if (OAUTH2_ERROR_CODE_PATTERN.matcher(oAuth2ErrorCode).matches()) {
                errorCode = oAuth2ErrorCode;
                errorMessage = oAuth2AuthenticationException.getError().getDescription();
            }
        }
        logger.debug("OAuth2 Error: " + errorCode, exception);
        if (errorCode.equals(CustomResponseCode.OAUTH2_ERROR.getCode())) {
            logger.error("OAuth2 Error: " + errorCode, exception);
        }

        // 쿼리 파라미터 URL 인코딩
        String redirectUri = UriComponentsBuilder
                                 .fromUri(URI.create(subServiceUriConfig.frontendCallbackUri()))
                                 .queryParam("code", errorCode)
                                 .queryParam("message", errorMessage)
                                 .build()
                                 .toString();

        this.getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
