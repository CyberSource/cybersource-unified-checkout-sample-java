package com.cybersource.example.web;

import com.cybersource.example.domain.TransientTokenRequest;
import com.cybersource.example.service.CaptureContextService;
import com.cybersource.example.service.JwtDecoderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.util.stream.Stream;

import static com.cybersource.example.util.RequestBuilderUtils.getRequestJsonAsString;

@Controller
@RequiredArgsConstructor
@SessionAttributes({"jwt", "bootstrapVersion", "unifiedCheckoutLibraryVersion"})
public class UnifiedCheckoutController {

    @Value("classpath:capture-context-request.json")
    private Resource captureContextRequestJson;

    private static final String BOOTSTRAP_VERSION = "https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css";
    @Autowired
    private final CaptureContextService captureContextService;

    @Autowired
    private final JwtDecoderService jwtDecoderService;

    @GetMapping("/")
    @SneakyThrows
    public String index(final Model model) {
        try (final Stream<String> lines = Files.lines(captureContextRequestJson.getFile().toPath())) {
            final long lineCount = lines.count();
            model.addAttribute("requestLineCount", lineCount);
        }
        model.addAttribute("requestJson", getRequestJsonAsString(captureContextRequestJson));
        model.addAttribute("bootstrapVersion", BOOTSTRAP_VERSION);
        return "index";
    }

    @PostMapping("/capture-context")
    public String requestCaptureContext(final Model model) {
        final String jwt = captureContextService.requestCaptureContextUsingSDK();
        model.addAttribute("jwt", jwt);
        model.addAttribute("unifiedCheckoutLibraryVersion", jwtDecoderService.getUnifiedCheckoutLibraryVersion(jwt));
        return "capture-context";
    }

    @PostMapping("/checkout")
    public String checkout(final Model model) {
        return "checkout";
    }

    @PostMapping("/token")
    public String viewToken(final @RequestParam TransientTokenRequest transientToken, final Model model){
        model.addAttribute("transientToken", transientToken.token());
        return "token";
    }
}
