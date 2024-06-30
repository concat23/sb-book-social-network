package com.dev.sbbooknetwork.helper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HttpRequestHelper {
    public String getCurrentServerDomain() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
