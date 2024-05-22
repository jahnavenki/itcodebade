package au.com.cfs.winged.core.servlets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Card Fund Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.paths=" + "/bin/jsonDataDropdown"
        })
public class JsonDataDropdownServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDataDropdownServlet.class);

    private static final String BASE_API_URL = "https://secure.colonialfirststate.com.au/fp/pricenperformance/products/funds/performance";
    private static final Map<String, String[]> API_PARAMETERS;

    static {
        API_PARAMETERS = new LinkedHashMap<>();
        API_PARAMETERS.put("companyCode", new String[]{"001"});
        API_PARAMETERS.put("mainGroup", new String[]{"SF"});
        API_PARAMETERS.put("productId", new String[]{"11"});
        API_PARAMETERS.put("category", new String[]{"Conservative", "Defensive", "Geared", "Growth", "High Growth", "Moderate", "Single sector option"});
        API_PARAMETERS.put("asset", new String[]{"Alternatives", "Australian Property Securities", "Australian Share", "Cash and other income", "Fixed Interest", "Global Property Securities", "Global Share", "Infrastructure securities", "Multi-Sector"});
        API_PARAMETERS.put("risk", new String[]{"1", "3", "4", "5", "6", "7"});
        API_PARAMETERS.put("mintimeframe", new String[]{"At least 10 years", "At Least 3 years", "At least 5 years", "At least 7 years", "No minimum"});
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LOGGER.debug("Servlet invoked: /bin/jsonDataDropdown");

        try {
            JSONObject apiResponse = callExternalAPI(BASE_API_URL, API_PARAMETERS);
            if (apiResponse != null) {
                writeToResponse(response, apiResponse);
            } else {
                LOGGER.error("API response is null.");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"API response is null\"}");
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private JSONObject callExternalAPI(String baseUrl, Map<String, String[]> parameters) throws IOException, JSONException {
        StringBuilder apiURL = new StringBuilder(baseUrl + "?");
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            for (String value : values) {
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                apiURL.append(key).append("=").append(encodedValue).append("&");
            }
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(apiURL.toString());
            try (CloseableHttpResponse apiResponse = httpClient.execute(httpGet)) {
                if (apiResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = apiResponse.getEntity();
                    if (entity != null) {
                        String apiResponseString = EntityUtils.toString(entity);
                        return new JSONObject(apiResponseString);
                    }
                } else {
                    LOGGER.error("API response status: {}", apiResponse.getStatusLine().getStatusCode());
                }
            }
        }
        return null;
    }

    private void writeToResponse(SlingHttpServletResponse response, JSONObject apiResponse) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(apiResponse.toString());
    }
}
