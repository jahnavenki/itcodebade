package com.ifx.core.servlets;
 
import java.io.IOException;
import java.util.*;
 
import javax.servlet.Servlet;
import javax.servlet.ServletException;
 
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import com.ifx.core.pojos.design.DesignFamily;
import com.ifx.core.services.design.DesignService;
import com.ifx.core.utils.PageUtil;
 
@Component(service = { Servlet.class }, immediate = true, name = "Design List Dropdown Servlet")
@SlingServletResourceTypes(resourceTypes="ifx/list/design", methods=HttpConstants.METHOD_GET)
@ServiceDescription("Design List Dropdown Servlet")
public class DesignDropDownServlet extends SlingSafeMethodsServlet {
 
  private static final Logger LOGGER = LoggerFactory.getLogger(DesignDropDownServlet.class);
 
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -4458186320139222562L;
  private static final String DATASOURCE = "datasource";
  private static final String PRODUCT_FAMILY_ID = "productFamilyId";
  private static final String GET_FAMILIES_FOR_PARENT = "getFamiliesForParent";
 
  @Reference
  private transient DesignService designService;
 
  
 
  @Override
  protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
      throws ServletException, IOException {

    DesignFamily[] designSupportFamilies;
    ResourceResolver resourceResolver = request.getResourceResolver();
    String pagePath = PageUtil.getPagePathFromRequest(request);
    PageManager pageMgr = resourceResolver.adaptTo(PageManager.class);
    Page currentPage = pageMgr.getPage(pagePath);
    String currentPageDesignId="";
    int index=0;
    if(currentPage.getProperties().containsKey(PRODUCT_FAMILY_ID)) {
      currentPageDesignId = currentPage.getProperties().get(PRODUCT_FAMILY_ID, String.class);
    }
    Locale locale = currentPage.getLanguage(false);
    Resource resource = request.getResource();
    Resource res = resource.getChild(DATASOURCE);
    Boolean checkParentPage = res.getValueMap().get(GET_FAMILIES_FOR_PARENT, Boolean.class);
    //converting the null value to false
    boolean lookupParentFamily = checkParentPage!=null && checkParentPage;
    Optional<String> langauge = Optional.of(locale.getLanguage());
    Page parentPage = PageUtil.getParentPage(request,currentPage);
    String productFamilyId= parentPage.getProperties().get(PRODUCT_FAMILY_ID, String.class);
    if (lookupParentFamily && StringUtils.isNotBlank(productFamilyId)) {
      designSupportFamilies = designService.getDesignFamilies(langauge,productFamilyId);
    } else {
      designSupportFamilies = designService.getDesignFamilies(langauge);
    }

    Gson gson = new Gson();
    List<Resource> resourceList = new ArrayList<>();
    if (null != designSupportFamilies) {
      for (int i=0;i<designSupportFamilies.length;i++) {
        DesignFamily designFamily=designSupportFamilies[i];
        ValueMap vm = new ValueMapDecorator(new HashMap<>());
        if(currentPageDesignId.equals(designFamily.getProductFamilyId())){
          index=i;
        }
        vm.put("value", Base64.getEncoder()
            .encodeToString(gson.toJson(designFamily).getBytes()));
        vm.put("text", designFamily.getProductFamilyName());
        resourceList.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED , vm));
      }
      if(!Objects.equals(parentPage,currentPage)) {
        Collections.swap(resourceList, 0, index);
      }
    }
    DataSource dataSource = new SimpleDataSource(resourceList.iterator());
    request.setAttribute(DataSource.class.getName(), dataSource);
  }
 
}

// Overloading the method to get product subfamilies for product ID
  public DesignFamily[] getDesignFamilies(Optional<String> language, String productFamilyId) {
 
    Map<String, String> queryParameters = new HashMap<>();
    queryParameters.put(Constants.LANGUAGE, getLanguage(language));
    queryParameters.put(Constants.FAMILY_ID, String.valueOf(productFamilyId));
    return getDesignDetails(queryParameters);
  }

private DesignFamily[] getDesignDetails(Map<String, String> queryParameter) {
    DesignFamily[] designFamilies = new DesignFamily[0];
    String url;
    String langCode = queryParameter.get(Constants.LANGUAGE);
    if(langCode.equalsIgnoreCase(Constants.JA) || langCode.equalsIgnoreCase(Constants.ZH)) {
     url = PageUtil.replaceLanguageCode(apiGatewayService.getDesignFamilyApi(queryParameter));
    }else {
      url = apiGatewayService.getDesignFamilyApi(queryParameter);
    }
    try {
      HttpRequest request = resourceOwnerTokenService.getHttpRequest().uri(new URI(url)).GET().build();
      HttpResponse<String> response = HttpClient.newBuilder()
          .connectTimeout(Duration.ofMillis(apiGatewayService.getTimeout())).build()
          .send(request, HttpResponse.BodyHandlers.ofString());
      String responseJSONStr = response.body();
      if (response.statusCode() == 200) {
        return getDesignFamilies(responseJSONStr);
      }
    }  catch (URISyntaxException | InterruptedException | IOException e) {
      LOGGER.error("getProductDetails(): IO exception occured", e);
    }
    return designFamilies;
  }
[
@Override
  public String getDesignFamilyApi(Map<String, String> queryParameters) {
    String designApi = null;
    String pathVal = this.apiGatewayServiceConfig.designFamilyApiPath();
    if (null != pathVal && !"".equals(pathVal))
      designApi = getApiBuildUrl(this.apiGatewayServiceConfig.designFamilyApiPath(), queryParameters);
    return designApi;
  }
