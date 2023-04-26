package com.cybersource.example.service;

import com.cybersource.example.domain.CaptureContextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Base64.Decoder;

@Service
@RequiredArgsConstructor
public class JwtDecoderService {
    @SneakyThrows
    public String getUnifiedCheckoutLibraryVersion(final String jwt) {
        // Parse the JWT response into header, payload, and signature
        final String[] jwtChunks = jwt.split("\\.");
        final Decoder decoder = Base64.getUrlDecoder();
        final String payload = new String(decoder.decode(jwtChunks[1]));

        // Map the response payload to a POJO
        final CaptureContextResponse mappedCaptureContextResponse =
                new ObjectMapper().readValue(payload, CaptureContextResponse.class);

        // Dynamically retrieve the client library
        return mappedCaptureContextResponse.ctx().stream().findFirst()
                .map(wrapper -> wrapper.data().clientLibrary())
                .orElseThrow();
    }
}
