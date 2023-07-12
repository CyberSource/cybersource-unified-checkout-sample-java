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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Log4j2
@RequiredArgsConstructor
public class CaptureContextService {

    @Autowired
    private final ApplicationProperties applicationProperties;

    // This method uses the Cybersource SDK to simplify constructing the request. We recommend using the SDK.
    @SneakyThrows
    public String requestCaptureContextUsingSDK(final GenerateUnifiedCheckoutCaptureContextRequest captureContextRequest) {
        // Cybersource's MerchantConfig class requires a generic Java Properties class, so let's map our Spring injected
        // properties to a normal Properties class
        final MerchantConfig merchantConfig = new MerchantConfig(applicationProperties.getAsJavaProperties());

        // Create an instance of Cybersource's generic API client using your merchant config and use it to instantiate the
        // Unified-Checkout-specific capture context API
        final ApiClient apiClient = new ApiClient(merchantConfig);
        final UnifiedCheckoutCaptureContextApi captureContextApi = new UnifiedCheckoutCaptureContextApi(apiClient);

        // Invoke the API. The response is a Capture Context defined in part by your requested values, returned as a JWT String
        return captureContextApi.generateUnifiedCheckoutCaptureContext(captureContextRequest);
    }

    // If you prefer to construct the request yourself, you can manually generate the headers, body, etc. To see this in action
    // just change the method call in the controller.
    @SneakyThrows
    public String requestCaptureContextWithoutSDK(final String captureContextRequest) {

        final String date = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
        final String url = "https://apitest.cybersource.com/up/v1/capture-contexts";

        // All of these headers are created automatically by the API client in the above method.
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set("digest", getDigest(captureContextRequest));
        headers.set("v-c-merchant-id", applicationProperties.getMerchantID());
        headers.set("date", date);
        headers.set("host", applicationProperties.getRequestHost());
        headers.set("signature", getSignatureHeader(date, applicationProperties, captureContextRequest));
        headers.set("User-Agent", applicationProperties.getUserAgent());

        final HttpEntity<String> request = new HttpEntity<>(captureContextRequest, headers);

        final RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(url, request, String.class);
    }

    private String getSignatureHeader(final String date, final ApplicationProperties properties,
                                      final String captureContextRequest) {
        return """
                keyid="%s", algorithm="HmacSHA256", \
                headers="host date (request-target) digest v-c-merchant-id", signature="%s"\
                """
                .formatted(properties.getMerchantKeyId(),
                        getSignatureParam(date, applicationProperties, captureContextRequest));
    }

    @SneakyThrows
    private String getSignatureParam(final String date, final ApplicationProperties properties,
                                     final String captureContextRequest) {
        final String signatureString = """
                host: %s
                date: %s
                (request-target): %s
                digest: %s
                v-c-merchant-id: %s\
                """.formatted(properties.getRequestHost(), date, "post /up/v1/capture-contexts",
                getDigest(captureContextRequest), properties.getMerchantID());
        final SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(properties.getMerchantSecretKey()), "HmacSHA256");
        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        hmacSHA256.init(secretKey);
        hmacSHA256.update(signatureString.getBytes());
        return Base64.getEncoder().encodeToString(hmacSHA256.doFinal());
    }

    private static String getDigest(final String captureContextRequest) throws NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final byte[] digestBytes = messageDigest.digest(captureContextRequest.getBytes(UTF_8));
        return  "SHA-256=" + Base64.getEncoder().encodeToString(digestBytes);
    }

}
