package com.cybersource.example.web;

import Model.GenerateUnifiedCheckoutCaptureContextRequest;
import Model.RiskV1AuthenticationSetupsPost201Response;
import Model.RiskV1AuthenticationsPost201Response;
import Model.RiskV1AuthenticationResultsPost201Response;
import com.cybersource.example.service.CaptureContextService;
import com.cybersource.example.service.JwtProcessorService;
import com.cybersource.example.service.PayerAuthenticationService;
import com.cybersource.example.service.PaymentsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
@RequiredArgsConstructor
@SessionAttributes({"captureContextJwt", "bootstrapVersion", "unifiedCheckoutLibraryVersion",
        "encodedTransientToken", "decodedTransientToken", "referenceId", "transactionId"})
public class UnifiedCheckoutController {

    @Value("classpath:capture-context-request.json")
    private Resource captureContextRequestJson;

    private static final String BOOTSTRAP_VERSION = "https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css";
    @Autowired
    private final CaptureContextService captureContextService;

    @Autowired
    private final JwtProcessorService jwtProcessorService;

    @Autowired
    private final PaymentsService paymentsService;

    @Autowired
    private final PayerAuthenticationService payerAuthenticationService;

    private String threeDSTransactionId;

    @GetMapping("/")
    @SneakyThrows
    public String index(final Model model) {
        try (final Stream<String> lines = Files.lines(captureContextRequestJson.getFile().toPath())) {
            final long lineCount = lines.count();
            model.addAttribute("requestLineCount", lineCount);
        }
        model.addAttribute("requestJson", IOUtils.toString(captureContextRequestJson.getInputStream(), UTF_8));
        model.addAttribute("bootstrapVersion", BOOTSTRAP_VERSION);
        return "index";
    }

    @PostMapping("/capture-context")
    @SneakyThrows
    public String requestCaptureContext(final @RequestParam("captureContextRequest") String captureContextRequest,
                                        final Model model) {
        final GenerateUnifiedCheckoutCaptureContextRequest mappedRequest = new ObjectMapper()
                .readValue(captureContextRequest.getBytes(UTF_8), GenerateUnifiedCheckoutCaptureContextRequest.class);

        // Use the SDK to get the capture context using our mapped request
        final String jwt = captureContextService.requestCaptureContext(mappedRequest);
        // Or uncomment the line below and comment the line above to test without using the SDK
        // final String jwt = captureContextService.requestCaptureContextWithoutSDK(captureContextRequest);

        // Verify that the response is from CyberSource
        final String decodedBody = jwtProcessorService.verifyJwtAndGetDecodedBody(jwt);
        // Dynamically retrieve the client library version to pass back to our frontend
        final String clientVersion = jwtProcessorService.getClientVersionFromDecodedBody(decodedBody);

        model.addAttribute("captureContextJwt", jwt);
        model.addAttribute("decodedBody", new ObjectMapper().readTree(decodedBody).toPrettyString());
        model.addAttribute("unifiedCheckoutLibraryVersion", clientVersion);

        return "capture-context";
    }

    @PostMapping("/checkout")
    public String checkout(final Model model) {
        return "checkout";
    }

    @PostMapping("/token")
    @SneakyThrows
    public String viewToken(final @RequestParam String transientToken, final Model model) {
        // Verify that the transient token is legitimate so we can display it and use it later.
        final String decodedBody = jwtProcessorService.verifyJwtAndGetDecodedBody(transientToken);

        model.addAttribute("encodedTransientToken", transientToken);
        model.addAttribute("decodedTransientToken", new ObjectMapper().readTree(decodedBody).toPrettyString());

        return "token";
    }

    @PostMapping("/receipt")
    @SneakyThrows
    public String receipt(final Model model) {
        // Call the payments endpoint to make a request using our previously verified transient token
        final String paymentsResponse = paymentsService.makePaymentWithTransientToken(
                model.getAttribute("encodedTransientToken").toString());

        model.addAttribute("paymentsResponse", paymentsResponse);

        return "receipt";
    }

    @PostMapping("/payer-authentication-setup")
    @SneakyThrows
    public String payerAuthenticationSetup(final Model model) {
        // Call the payer authentication setup endpoint with our transient token
        final RiskV1AuthenticationSetupsPost201Response payerAuthSetupResponse =
                payerAuthenticationService.setupPayerAuthentication(model.getAttribute("decodedTransientToken").toString());

        // Cardinal, which is owned by Visa, provides the Javascript library to perform the step-up process.
        final URL cardinalUrl = new URL(payerAuthSetupResponse.getConsumerAuthenticationInformation().getDeviceDataCollectionUrl());

        model.addAttribute("payerAuthenticationSetupResponse", payerAuthSetupResponse.toString());
        model.addAttribute("payerAuthenticationSetupJwt", payerAuthSetupResponse.getConsumerAuthenticationInformation().getAccessToken());
        model.addAttribute("cardinalDataCollectionUrl", cardinalUrl);
        model.addAttribute("cardinalDataCollectionEnvironment", cardinalUrl.getProtocol() + "://" + cardinalUrl.getHost());
        model.addAttribute("referenceId", payerAuthSetupResponse.getConsumerAuthenticationInformation().getReferenceId());

        return "payer-authentication-setup";
    }

    @PostMapping("/payer-authentication-enrollment")
    public String payerAuthenticationEnrollment(final Model model) {
        // Call payer authentication enrollment with our transient token and the reference ID we got from setup.
        final RiskV1AuthenticationsPost201Response payerAuthEnrollmentResponse =
                payerAuthenticationService.enrollPayerAuthentication(
                        model.getAttribute("decodedTransientToken").toString(), model.getAttribute("referenceId").toString()
                );
        model.addAttribute("payerAuthenticationEnrollmentResponse", payerAuthEnrollmentResponse);

        final String authenticationStatus = payerAuthEnrollmentResponse.getStatus();
        // If authentication was successful straight away we can skip the step-up process and proceed with payment.
        if ("AUTHENTICATION_SUCCESSFUL".equals(authenticationStatus)) {
            model.addAttribute("stepUpRequired", false);
            // Otherwise, we need to proceed with step-up.
        } else if ("PENDING_AUTHENTICATION".equals(authenticationStatus)) {
            model.addAttribute("stepUpRequired", true);
            model.addAttribute("stepUpUrl", payerAuthEnrollmentResponse.getConsumerAuthenticationInformation().getStepUpUrl());
            model.addAttribute("stepUpAccessToken", payerAuthEnrollmentResponse.getConsumerAuthenticationInformation().getAccessToken());
        } else {
            throw new RuntimeException(String.format(
                    "This demo app is not built to handle status: %s, but best to do so elegantly on the frontend.",
                    authenticationStatus));
        }

        return "payer-authentication-enrollment";
    }

    @PostMapping("/blank-page")
    // Stub for a blank page for our somewhat hacky iframe redirect solution. Makes the redirect less jarring.
    public String blankPage(final @RequestParam("TransactionId") String transactionId) {
        // TODO: Set this variable that needs to be reused more elegantly
        //  when called by Cardinal's redirect this method uses a different model so we can't set it there...

        // Cardinal sends a POST to the redirect URL with the transactionId, we need that to validate our results later.
        threeDSTransactionId = transactionId;
        return "blank-page";
    }

    @GetMapping("/payer-authentication-validation")
    @SneakyThrows
    public String payerAuthenticationValidation(final Model model) {
        // In the case we did perform step-up, we can check the results using the authentication results API using our
        // transientToken and the transactionId we got from Cardinal.
        final RiskV1AuthenticationResultsPost201Response authenticationResultsResponse =
            payerAuthenticationService.validateAuthenticationResults(
                    model.getAttribute("decodedTransientToken").toString(),
                    threeDSTransactionId
            );

        final boolean isSuccessful = "AUTHENTICATION_SUCCESSFUL".equals(authenticationResultsResponse.getStatus());
        if (!isSuccessful) {
            model.addAttribute("errorMessage", authenticationResultsResponse.getErrorInformation().getMessage());
        }

        model.addAttribute("isSuccessful", isSuccessful);
        model.addAttribute("authenticationResultsResponse", authenticationResultsResponse);
        return "payer-authentication-validation";
    }
}
