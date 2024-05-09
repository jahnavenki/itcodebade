package au.com.cfs.winged.core.servlets;

import java.io.IOException;
import java.util.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import au.com.cfs.winged.core.models.pojo.PerformanceFamily;
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

import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import au.com.cfs.winged.core.services.FundCardComponentService;
import au.com.cfs.winged.core.util.PageUtils;


@Component(service = {
        Servlet.class
}, immediate = true, name = "Performance List Dropdown Servlet")
@SlingServletResourceTypes(resourceTypes = "pricePerformance/list/design", methods = HttpConstants.METHOD_GET)
@ServiceDescription("Price & Performance List Dropdown Servlet")
public class PerformanceDropDownServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceDropDownServlet.class);

    private static final long serialVersionUID = -4458186320139222562L;
    private static final String DATASOURCE = "datasource";
    private static final String MARKETING_NAME = "marketingName";
    private static final String EFFECTIVE_DATE = "effectiveDate";

    @Reference
    private transient FundCardComponentService fundCardComponentService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        PerformanceFamily[]  performanceSupportFamilies;
        ResourceResolver resourceResolver = request.getResourceResolver();
        String pagePath = PageUtils.getPagePathFromRequest(request);
        PageManager pageMgr = resourceResolver.adaptTo(PageManager.class);
        Page currentPage = null;
        if (pageMgr != null) {
            currentPage = pageMgr.getPage(pagePath);
        }
        String currentPageDesignId = "";
        int index = 0;
        if (currentPage != null && currentPage.getProperties().containsKey(MARKETING_NAME)) {
            currentPageDesignId = currentPage.getProperties().get(EFFECTIVE_DATE, String.class);
        }
        Locale locale = currentPage != null ? currentPage.getLanguage(false) : null;
        Resource resource = request.getResource();
        Resource res = resource.getChild(DATASOURCE);
        Boolean checkParentPage = null;
        if (res != null) {
            checkParentPage = res.getValueMap().get(MARKETING_NAME, Boolean.class);
        }
        boolean lookupParentFamily = checkParentPage != null && checkParentPage;
        Optional < String > langauge = Optional.of(locale.getLanguage());
        Page parentPage = PageUtils.getParentPage(request,currentPage);
        String marketingName = null;
        if (parentPage != null) {
            marketingName = parentPage.getProperties().get(MARKETING_NAME, String.class);
        }
        if (lookupParentFamily && StringUtils.isNotBlank(marketingName)) {
            performanceSupportFamilies = fundCardComponentService.getPerformanceFamilies(langauge);
        } else {
            performanceSupportFamilies = fundCardComponentService.getPerformanceFamilies(langauge);
        }
        Gson gson = new Gson();
        List < Resource > resourceList = new ArrayList < > ();
        if (null != performanceSupportFamilies) {
            for (int i = 0; i < performanceSupportFamilies.length; i++) {
                PerformanceFamily performanceFamily = performanceSupportFamilies[i];
                ValueMap vm = new ValueMapDecorator(new HashMap < > ());
                if (currentPageDesignId.equals(performanceFamily.getMarketingName())) {
                    index = i;
                }
                vm.put("value", Base64.getEncoder().encodeToString(gson.toJson(performanceFamily).getBytes()));
                vm.put("text", performanceFamily.getMarketingName());

                resourceList.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, vm));
            }
            if (!Objects.equals(parentPage, currentPage)) {
                Collections.swap(resourceList, 0, index);
            }
        }
    }
}