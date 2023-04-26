package com.cybersource.example.domain;

public record OrderInformation(OrderInformation.AmountDetails amountDetails) {
        public record AmountDetails(String totalAmount, String currency) {
    }
}
