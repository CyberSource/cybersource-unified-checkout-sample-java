package com.cybersource.example.domain;


import java.util.List;

public record CaptureMandate(String billingType, boolean requestEmail, boolean requestPhone, boolean requestShipping,
                             List<String> shipToCountries, boolean showAcceptedNetworkIcons) {
}
