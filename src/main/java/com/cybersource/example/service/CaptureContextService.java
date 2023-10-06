package com.cybersource.example.service;

import Api.UnifiedCheckoutCaptureContextApi;
import Invokers.ApiClient;
import Model.GenerateUnifiedCheckoutCaptureContextRequest;
import com.cybersource.authsdk.core.MerchantConfig;
import com.cybersource.example.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class CaptureContextService {

    @Autowired
    private final ApplicationProperties applicationProperties;

    @SneakyThrows
    public String requestCaptureContext(final GenerateUnifiedCheckoutCaptureContextRequest captureContextRequest) {
        // Cybersource's MerchantConfig class requires a generic Java Properties class, so let's map our Spring injected
        // properties to a normal Properties class

        // Create an instance of Cybersource's generic API client using our merchant config.
        final ApiClient apiClient = new ApiClient(new MerchantConfig(applicationProperties.getAsJavaProperties()));

        // Use it to instantiate the Unified-Checkout-specific capture context API
        final UnifiedCheckoutCaptureContextApi captureContextApi = new UnifiedCheckoutCaptureContextApi(apiClient);

        // Invoke the API. The response is a Capture Context defined in part by your requested values, returned as a JWT String
        return captureContextApi.generateUnifiedCheckoutCaptureContext(captureContextRequest);
    }

}
