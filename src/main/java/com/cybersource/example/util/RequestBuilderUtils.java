package com.cybersource.example.util;

import Model.GenerateUnifiedCheckoutCaptureContextRequest;
import Model.Upv1capturecontextsCaptureMandate;
import Model.Upv1capturecontextsOrderInformation;
import Model.Upv1capturecontextsOrderInformationAmountDetails;
import com.cybersource.example.domain.CaptureContextRequest;
import com.cybersource.example.domain.CaptureMandate;
import com.cybersource.example.domain.OrderInformation;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import static java.nio.charset.StandardCharsets.UTF_8;

@UtilityClass
public class RequestBuilderUtils {
    public static GenerateUnifiedCheckoutCaptureContextRequest getCaptureContextRequest(
            CaptureContextRequest mappedCaptureContextRequestJson) {
        CaptureMandate captureMandateJson = mappedCaptureContextRequestJson.captureMandate();
        Upv1capturecontextsCaptureMandate captureMandate = new Upv1capturecontextsCaptureMandate()
                .billingType(captureMandateJson.billingType())
                .requestEmail(captureMandateJson.requestEmail())
                .requestPhone(captureMandateJson.requestPhone())
                .requestShipping(captureMandateJson.requestShipping())
                .shipToCountries(captureMandateJson.shipToCountries())
                .showAcceptedNetworkIcons(captureMandateJson.showAcceptedNetworkIcons());

        OrderInformation.AmountDetails amountDetailsJson = mappedCaptureContextRequestJson.orderInformation().amountDetails();
        Upv1capturecontextsOrderInformationAmountDetails amountDetails = new Upv1capturecontextsOrderInformationAmountDetails()
                .totalAmount(amountDetailsJson.totalAmount())
                .currency(amountDetailsJson.currency());
        Upv1capturecontextsOrderInformation orderInformation = new Upv1capturecontextsOrderInformation()
                .amountDetails(amountDetails);

        return new GenerateUnifiedCheckoutCaptureContextRequest()
                .targetOrigins(mappedCaptureContextRequestJson.targetOrigins())
                .clientVersion(mappedCaptureContextRequestJson.clientVersion())
                .allowedCardNetworks(mappedCaptureContextRequestJson.allowedCardNetworks())
                .allowedPaymentTypes(mappedCaptureContextRequestJson.allowedPaymentTypes())
                .country(mappedCaptureContextRequestJson.country())
                .locale(mappedCaptureContextRequestJson.locale())
                .captureMandate(captureMandate)
                .orderInformation(orderInformation);
    }

    @SneakyThrows
    public static String getRequestJsonAsString(Resource input) {
        return IOUtils.toString(input.getInputStream(), UTF_8);
    }
}
