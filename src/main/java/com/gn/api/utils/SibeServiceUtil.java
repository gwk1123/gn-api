package com.gn.api.utils;

import com.gn.config.SibeProperties;
import com.gn.ota.site.SibeOrderRequest;
import com.gn.ota.site.SibeVerifyRequest;
import org.springframework.stereotype.Component;

@Component
public class SibeServiceUtil {

    public SibeVerifyRequest getSibeVerifyRequest(SibeProperties sibeProperties) {
        SibeVerifyRequest sibeVerifyRequest = new SibeVerifyRequest();
        sibeVerifyRequest.setSibeProperties(sibeProperties);
        return sibeVerifyRequest;
    }

    public SibeOrderRequest getSibeOrderRequest(SibeProperties sibeProperties) {
        SibeOrderRequest sibeOrderRequest = new SibeOrderRequest();
        sibeOrderRequest.setSibeProperties(sibeProperties);
        return sibeOrderRequest;
    }
}
