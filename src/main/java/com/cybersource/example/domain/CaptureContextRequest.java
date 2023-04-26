package com.cybersource.example.domain;

import java.util.List;

// Request with minimum required attributes. If you want to test out pre-filling billing info, etc. you'll need to add fields here
public record CaptureContextRequest(List<String> targetOrigins, String clientVersion, List<String> allowedCardNetworks,
                                    List<String> allowedPaymentTypes, String country, String locale,
                                    CaptureMandate captureMandate, OrderInformation orderInformation) {
}
