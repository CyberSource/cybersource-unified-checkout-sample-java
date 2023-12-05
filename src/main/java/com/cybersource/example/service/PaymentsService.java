package com.cybersource.example.service;

import Api.PaymentsApi;
import Invokers.ApiClient;
import Invokers.ApiException;
import Model.*;
import com.cybersource.authsdk.core.ConfigException;
import com.cybersource.authsdk.core.MerchantConfig;
import com.cybersource.example.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentsService {
    @Autowired
    private final ApplicationProperties applicationProperties;

    public String makePaymentWithTransientToken(final String transientToken) throws ConfigException, ApiException {
        final ApiClient apiClient = new ApiClient(new MerchantConfig(applicationProperties.getAsJavaProperties()));
        final PaymentsApi paymentApi = new PaymentsApi(apiClient);

        final CreatePaymentRequest paymentRequest = createPaymentRequest(transientToken);
        final PtsV2PaymentsPost201Response response = paymentApi.createPayment(paymentRequest);

        return response.toString();
    }

    private static CreatePaymentRequest createPaymentRequest(final String transientToken) {
        // Additional fields like billTo, shipTo, amountDetails can be provided here to override what Unified Checkout collected
        final Ptsv2paymentsClientReferenceInformation clientReferenceInfo =
                new Ptsv2paymentsClientReferenceInformation().code("test_payment");
        final Ptsv2paymentsTokenInformation tokenInformation = new Ptsv2paymentsTokenInformation().transientTokenJwt(transientToken);
        return new CreatePaymentRequest()
                .clientReferenceInformation(clientReferenceInfo)
                .tokenInformation(tokenInformation)
                // TODO: Adding these in case $.captureMandate.billingType = PARTIAL or NONE,
                //  or $.captureMandate.requestShipping = false. In clientVersion 0.18, this should be fixed and this shouldn't be necessary.
                .orderInformation(new Ptsv2paymentsOrderInformation()
                        .billTo(new Ptsv2paymentsOrderInformationBillTo().administrativeArea("NY"))
                        .shipTo(new Ptsv2paymentsOrderInformationShipTo().administrativeArea("NY"))
                );
    }
}
