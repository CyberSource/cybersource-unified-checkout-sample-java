package com.cybersource.example.web;

import Model.GenerateUnifiedCheckoutCaptureContextRequest;
import com.cybersource.example.service.CaptureContextService;
import com.cybersource.example.service.JwtProcessorService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.nio.file.Files;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
@RequiredArgsConstructor
@SessionAttributes({"captureContextJwt", "bootstrapVersion", "unifiedCheckoutLibraryVersion", "encodedTransientToken"})
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
        final String jwt = captureContextService.requestCaptureContextUsingSDK(mappedRequest);
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
        final String decodedBody = jwtProcessorService.verifyJwtAndGetDecodedBody(transientToken);
        model.addAttribute("encodedTransientToken", transientToken);
        model.addAttribute("decodedTransientToken", new ObjectMapper().readTree(decodedBody).toPrettyString());
        return "token";
    }

    @PostMapping("/receipt")
    @SneakyThrows
    public String receipt(final Model model) {
        // Call the payments endpoint to make a request using our transient token
        final String paymentsResponse = paymentsService.makePaymentWithTransientToken(
                model.getAttribute("encodedTransientToken").toString());
        model.addAttribute("paymentsResponse", paymentsResponse);
        return "receipt";
    }
}
