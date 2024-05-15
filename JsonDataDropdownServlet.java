package au.com.cfs.winged.core.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(service = Servlet.class, property = {"sling.servlet.paths=/cfs/jsonDataDropdown"})
public class JsonDataDropdownServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDataDropdownServlet.class);

    // Constants for API parameters
    private static final String BASE_API_URL = "https://secure.colonialfirststate.com.au/fp/pricenperformance/products/funds/performance";
    private static final Map<String, String[]> API_PARAMETERS;

	 static {
		// Add more parameters as needed...
		API_PARAMETERS = new LinkedHashMap<>(); // Use LinkedHashMap to preserve insertion order
		API_PARAMETERS.put("companyCode", new String[]{"001"});
		API_PARAMETERS.put("mainGroup", new String[]{"SF"});
		API_PARAMETERS.put("productId", new String[]{"11"});
		API_PARAMETERS.put("mintimeframe", new String[]{"At least 10 years", "At Least 3 years", "At least 5 years", "At least 7 years", "No minimum"});
		// Additional parameters
		API_PARAMETERS.put("category", new String[]{"Conservative", "Defensive", "Geared", "Growth", "High Growth", "Moderate", "Single sector option"});
		API_PARAMETERS.put("asset", new String[]{"Alternatives", "Australian Property Securities", "Australian Share", "Cash and other income", "Fixed Interest", "Global Property Securities", "Global Share", "Infrastructure securities", "Multi-Sector"});
		API_PARAMETERS.put("risk", new String[]{"1", "3", "4", "5", "6", "7"});
	}

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
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
                populateDropdown(response, apiResponse);
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

    private void writeToJSONFile(ResourceResolver resourceResolver, String jsonDataPath, String apiResponse) throws RepositoryException, PersistenceException {
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
} By using this servlet trying to hit external API and and storing inside aem dilog node but it is giving an error saying JSON data path not provided see below dilog
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0" xmlns:granite="http://www.adobe.com/jcr/granite/1.0" xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    jcr:primaryType="nt:unstructured">
    <items jcr:primaryType="nt:unstructured">
        <fundPerformance
            granite:class="cq-dialog-checkbox-showhide"
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/form/checkbox"
            fieldDescription="When checked, Retrieve API performance numbers from the funds and performance tool"
            name="./fundPerformance"
            text="Dynamic Fund Performance Values"
            value="{Boolean}false">
            <granite:data
                jcr:primaryType="nt:unstructured"
                cq-dialog-checkbox-showhide-target=".togglefield"/>
        </fundPerformance>
        <toggle
            granite:class="togglefield"
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/container">
            <items jcr:primaryType="nt:unstructured">
                <marketing
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="granite/ui/components/coral/foundation/form/select"
                    fieldLabel="Marketing Name"
                    name="./marketingName">
                    <datasource
                        jcr:primaryType="nt:unstructured"
                        sling:resourceType="/cfs/jsonDataDropdown"
                        jsonDataPath="cfs-winged/global/cards/fundsDialog/items/toggle/items/marketing/dropdown.json"/>
                    <dropdown.json/>
                </marketing>
            </items>
        </toggle>
        <size
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/form/select"
            fieldLabel="Size"
            name="./cardSize">
            <items jcr:primaryType="nt:unstructured">
                <default
                    jcr:primaryType="nt:unstructured"
                    text="Large"
                    value="large"/>
                <small
                    jcr:primaryType="nt:unstructured"
                    text="Small"
                    value="small"/>
            </items>
        </size>
        <heading
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
            fieldDescription="Add the Main header for the card."
            fieldLabel="Heading"
            name="./heading"/>
        <superHeading
            jcr:primaryType="nt:unstructured"
            sling:orderBefore="content"
            sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
            fieldDescription="Add the Super header content for the card, for the 'Card - Fund' component."
            fieldLabel="Super Heading"
            name="./superHeading"/>
        <headingLink
            jcr:primaryType="nt:unstructured"
            sling:resourceType="cq/gui/components/coral/common/form/pagefield"
            fieldDescription="Add the Main header link if required, otherwise keep empty."
            fieldLabel="Heading Link (Optional)"
            name="./headingLink"
            rootPath="/content"/>
        <content
            jcr:primaryType="nt:unstructured"
            sling:resourceType="cq/gui/components/authoring/dialog/richtext"
            fieldDescription="Add the body content of the card."
            fieldLabel="Content"
            name="./content"
            useFixedInlineToolbar="{Boolean}true">
            <rtePlugins
                jcr:primaryType="nt:unstructured"
                sling:resourceSuperType="/apps/cfs-winged/global/rtepluginConfig/rtePlugins"/>
            <uiSettings
                jcr:primaryType="nt:unstructured"
                sling:resourceSuperType="/apps/cfs-winged/global/rtepluginConfig/uiSettings"/>
        </content>
    </items>
</jcr:root>
