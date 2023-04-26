package com.cybersource.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties (ignoreUnknown = true)
public record CaptureContextResponse (List<CTX> ctx) {
    @JsonIgnoreProperties (ignoreUnknown = true)
    public record CTX (Data data) {}
    @JsonIgnoreProperties (ignoreUnknown = true)
    public record Data (String clientLibrary) {}
}
