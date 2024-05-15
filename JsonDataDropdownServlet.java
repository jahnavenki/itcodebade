import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Component(service = Servlet.class, property = {"sling.servlet.paths=/bin/jsonDataDropdown"})
public class JsonDataDropdownServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDataDropdownServlet.class);

    // Constants for API parameters
    private static final String BASE_API_URL = "https://secure.colonialfirststate.com.au/fp/pricenperformance/products/funds/performance";
    private static final Map<String, String[]> API_PARAMETERS = Map.of(
            "companyCode", new String[]{"001"},
            "mainGroup", new String[]{"SF"},
            "productId", new String[]{"11"},
            // Add more parameters as needed...
            "mintimeframe", new String[]{"At least 10 years", "At Least 3 years", "At least 5 years", "At least 7 years", "No minimum"}
    );

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String jsonDataPath = getJsonDataPath(request);
        if (jsonDataPath == null) {
            LOGGER.error("JSON Data path is not provided.");
            return;
        }

        try {
            String apiResponse = callExternalAPI(BASE_API_URL, API_PARAMETERS);
            if (apiResponse != null) {
                writeToJSONFile(request.getResourceResolver(), jsonDataPath, apiResponse);
                populateDropdown(out, apiResponse);
            }
        } catch (IOException | RepositoryException | JSONException e) {
            LOGGER.error("Error: {}", e.getMessage());
        }
    }

    private String getJsonDataPath(SlingHttpServletRequest request) {
        Resource pathResource = request.getResource();
        Resource dataSourceResource = pathResource.getChild("datasource");
        if (dataSourceResource != null) {
            return dataSourceResource.getValueMap().get("jsonDataPath", String.class);
        }
        return null;
    }

    private String callExternalAPI(String baseUrl, Map<String, String[]> parameters) throws IOException {
        StringBuilder apiURL = new StringBuilder(baseUrl + "?");

        // Append parameters to API URL
        parameters.forEach((key, values) -> {
            for (String value : values) {
                apiURL.append(key).append("=").append(value).append("&");
            }
        });

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(apiURL.toString());
            try (CloseableHttpResponse apiResponse = httpClient.execute(httpGet)) {
                if (apiResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return EntityUtils.toString(apiResponse.getEntity());
                }
            }
        }
        return null;
    }

    private void writeToJSONFile(ResourceResolver resourceResolver, String jsonDataPath, String apiResponse) throws RepositoryException {
        Resource jsonResource = resourceResolver.getResource(jsonDataPath);
        Node jsonNode = jsonResource.adaptTo(Node.class);

        if (jsonNode != null) {
            Node contentNode = jsonNode.getNode(JcrConstants.JCR_CONTENT);
            if (contentNode != null) {
                contentNode.setProperty("jcr:data", apiResponse);
                resourceResolver.commit();
            }
        }
    }

private void populateDropdown(SlingHttpServletResponse response, String apiResponse) throws IOException, JSONException {
    JSONObject jsonObject = new JSONObject(apiResponse);
    Iterator<String> jsonKeys = jsonObject.keys();

    JSONObject dropdownOptions = new JSONObject();
    while (jsonKeys.hasNext()) {
        String jsonKey = jsonKeys.next();
        String jsonValue = jsonObject.getString(jsonKey);
        dropdownOptions.put(jsonKey, jsonValue);
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(dropdownOptions.toString());
}
}
