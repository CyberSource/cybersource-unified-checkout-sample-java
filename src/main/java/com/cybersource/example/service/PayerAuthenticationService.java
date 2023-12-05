package com.cybersource.example.service;

import Api.PayerAuthenticationApi;
import Invokers.ApiClient;
import Model.*;
import com.cybersource.authsdk.core.MerchantConfig;
import com.cybersource.example.config.ApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayerAuthenticationService {
    @Autowired
    private final ApplicationProperties applicationProperties;

    @SneakyThrows
    public RiskV1AuthenticationSetupsPost201Response setupPayerAuthentication(final String decodedTransientToken) {
        // Note that the payer authentication APIs only take the JTI instead of the full JWT.
        final String jti = getJtiFromTransientToken(decodedTransientToken);

        // The transient token's JTI is all we need to perform payer auth setup.
        final PayerAuthSetupRequest payerAuthSetupRequest = new PayerAuthSetupRequest()
                .tokenInformation(new Riskv1authenticationsetupsTokenInformation().transientToken(jti));

        // The payer authentication APIs all share a common client class, so we can extract instantiation out to a method
        // and just call the specific methods here.
        return getPayerAuthenticationApi().payerAuthSetup(payerAuthSetupRequest);
    }

    @SneakyThrows
    public RiskV1AuthenticationsPost201Response enrollPayerAuthentication(final String decodedTransientToken, final String referenceId) {
        final String jti = getJtiFromTransientToken(decodedTransientToken);

        // We use our transient token JTI again as well as the reference ID we got in the previous step to perform an enrollment request
        final CheckPayerAuthEnrollmentRequest payerAuthEnrollmentRequest = new CheckPayerAuthEnrollmentRequest()
                .tokenInformation(new Riskv1authenticationsetupsTokenInformation().transientToken(jti))
                .consumerAuthenticationInformation(new Riskv1decisionsConsumerAuthenticationInformation()
                        .referenceId(referenceId)
                        //TODO: We're just using our blank page URL to smooth the iframe close + redirect.
                        .returnUrl("https://localhost:8080/blank-page"));

        return getPayerAuthenticationApi().checkPayerAuthEnrollment(payerAuthEnrollmentRequest);
    }

    @SneakyThrows
    public RiskV1AuthenticationResultsPost201Response validateAuthenticationResults(final String decodedTransientToken, final String transactionId) {
        final String jti = getJtiFromTransientToken(decodedTransientToken);

        // Again, the validation request takes our transient token JTI and the transaction ID we got back from Cardinal.
        final ValidateRequest validationRequest = new ValidateRequest()
                .tokenInformation(new Riskv1decisionsTokenInformation().jti(jti))
                .consumerAuthenticationInformation(new Riskv1authenticationresultsConsumerAuthenticationInformation()
                        // to test failure, you can hardcode this to any random String
                        .authenticationTransactionId(transactionId));

        return getPayerAuthenticationApi().validateAuthenticationResults(validationRequest);

    }

    private static String getJtiFromTransientToken(final String decodedTransientToken) throws JsonProcessingException {
        return new ObjectMapper().readTree(decodedTransientToken).get("jti").textValue();
    }

    @SneakyThrows
    private PayerAuthenticationApi getPayerAuthenticationApi() {
        final ApiClient apiClient = new ApiClient(new MerchantConfig(applicationProperties.getAsJavaProperties()));
        return new PayerAuthenticationApi(apiClient);
    }

}
