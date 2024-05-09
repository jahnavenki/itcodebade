package au.com.cfs.winged.core.services;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import au.com.cfs.winged.core.models.pojo.PerformanceFamily;
import au.com.cfs.winged.core.util.PageUtils;


public class FundCardComponentService {

    // Overloading the method to get product subfamilies for product ID
    public PerformanceFamily[] getPerformanceFamilies(Optional< String > language) {
        Map< String, String > queryParameters = new HashMap < > ();
        queryParameters.put(Constants.LANGUAGE, getLanguage(language));
        queryParameters.put(Constants.FAMILY_ID, String.valueOf(marcetingName));
        return getPerformanceDetails(queryParameters);
    }
    private PerformanceFamily[] getPerformanceDetails(Map < String, String > queryParameter) {
        PerformanceFamily[] designFamilies = new PerformanceFamily[0];
        String url;
        String langCode = queryParameter.get(Constants.LANGUAGE);
        if (langCode.equalsIgnoreCase(Constants.JA) || langCode.equalsIgnoreCase(Constants.ZH)) {
            url = PageUtils.replaceLanguageCode(apiGatewayService.getDesignFamilyApi(queryParameter));
        } else {
            url = apiGatewayService.getDesignFamilyApi(queryParameter);
        }
        try {
            HttpRequest request = resourceOwnerTokenService.getHttpRequest().uri(new URI(url)).GET().build();
            HttpResponse< String > response = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(apiGatewayService.getTimeout())).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            String responseJSONStr = response.body();
            if (response.statusCode() == 200) {
                eturn getDesignFamilies(responseJSONStr);
            }
        } catch (URISyntaxException | InterruptedException | IOException e) {
            LOGGER.error("getProductDetails(): IO exception occured", e);
        }
        return PerformanceFamilies;
    }
    @Override
    public String getDesignFamilyApi(Map < String, String > queryParameters) {
        String designApi = null;
        String pathVal = this.apiGatewayServiceConfig.designFamilyApiPath();
        if (null != pathVal && !"".equals(pathVal))
            designApi = getApiBuildUrl(this.apiGatewayServiceConfig.designFamilyApiPath(), queryParameters);
        return designApi;
    }


}