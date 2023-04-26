package com.cybersource.example.service;

import Api.UnifiedCheckoutCaptureContextApi;
import Invokers.ApiClient;
import Invokers.ApiResponse;
import Model.GenerateUnifiedCheckoutCaptureContextRequest;
import com.cybersource.authsdk.core.MerchantConfig;
import com.cybersource.example.config.ApplicationProperties;
import com.cybersource.example.domain.CaptureContextRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

import static com.cybersource.example.util.RequestBuilderUtils.getCaptureContextRequest;
import static com.cybersource.example.util.RequestBuilderUtils.getRequestJsonAsString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Log4j2
@RequiredArgsConstructor
public class CaptureContextService {

    @Autowired
    private final ApplicationProperties applicationProperties;

    @Value("classpath:capture-context-request.json")
    private Resource captureContextRequestJson;

    // This method invokes the Cybersource SDK to simplify constructing the request. We recommend using the SDK.
    @SneakyThrows
    public String requestCaptureContextUsingSDK() {
        // Cybersource's MerchantConfig class requires a generic Java Properties class, so let's map our Spring injected
        // properties to a normal Properties class
        final MerchantConfig merchantConfig = new MerchantConfig(applicationProperties.getAsJavaProperties());
        // Host is not set when instantiating the MerchantConfig
        merchantConfig.setRequestHost(applicationProperties.getHost());

        // Create an instance of Cybersource's generic API client using your merchant config and use it to instantiate the
        // Unified-Checkout-specific capture context API
        final ApiClient apiClient = new ApiClient(merchantConfig);
        final UnifiedCheckoutCaptureContextApi captureContextApi = new UnifiedCheckoutCaptureContextApi(apiClient);

        // For brevity, use Jackson to map our request JSON into a POJO. To play around with the various other fields you'll need to
        // update these POJOs in the domain package
        final CaptureContextRequest mappedCaptureContextRequestJson =
                new ObjectMapper().readValue(captureContextRequestJson.getFile(), CaptureContextRequest.class);
        // Then transform it into the Cybersource GenerateUnifiedCheckoutCaptureContextRequest object to make the request
        final GenerateUnifiedCheckoutCaptureContextRequest request = getCaptureContextRequest(mappedCaptureContextRequestJson);

        // TODO: Replace with UnifiedCheckout API code below when API Response is fixed and not set to return <Void>
        //  use the generic API client for now instead
        //  ApiResponse<Void> captureContextResponse = captureContextApi.generateUnifiedCheckoutCaptureContextWithHttpInfo(request);
        final Call apiCall = captureContextApi.generateUnifiedCheckoutCaptureContextCall(request, null,null);
        final ApiResponse<String> apiResponse = apiClient.execute(apiCall, String.class);
        // The response is your Capture Context as a JWT
        return apiResponse.getData();
    }

    // If you prefer to construct the request yourself, you can manually generate the headers, body, etc. To see this in action
    // just change the method call in the controller.
    @SneakyThrows
    public String requestCaptureContextWithoutSDK() {

        final String date = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
        final String url = "https://apitest.cybersource.com/up/v1/capture-contexts";

        // All of these headers are created automatically by the API client in the above method.
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set("digest", getDigest());
        headers.set("v-c-merchant-id", applicationProperties.getMerchantID());
        headers.set("date", date);
        headers.set("host", applicationProperties.getHost());
        headers.set("signature", getSignatureHeader(date, applicationProperties));
        headers.set("User-Agent", applicationProperties.getUserAgent());

        final HttpEntity<String> request = new HttpEntity<>(getRequestJsonAsString(captureContextRequestJson), headers);

        final RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(url, request, String.class);
    }

    private String getSignatureHeader(final String date, final ApplicationProperties properties) {
        return """
                keyid="%s", algorithm="HmacSHA256", \
                headers="host date (request-target) digest v-c-merchant-id", signature="%s"\
                """
                .formatted(properties.getMerchantKeyId(), getSignatureParam(date, applicationProperties));
    }

    @SneakyThrows
    private String getSignatureParam(final String date, final ApplicationProperties properties) {
        final String signatureString = """
                host: %s
                date: %s
                (request-target): %s
                digest: %s
                v-c-merchant-id: %s\
                """.formatted(properties.getHost(), date, "post /up/v1/capture-contexts", getDigest(), properties.getMerchantID());
        final SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(properties.getMerchantSecretKey()), "HmacSHA256");
        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        hmacSHA256.init(secretKey);
        hmacSHA256.update(signatureString.getBytes());
        return Base64.getEncoder().encodeToString(hmacSHA256.doFinal());
    }

    private String getDigest() throws NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final byte[] digestBytes = messageDigest.digest(getRequestJsonAsString(captureContextRequestJson).getBytes(UTF_8));
        return  "SHA-256=" + Base64.getEncoder().encodeToString(digestBytes);
    }

}
