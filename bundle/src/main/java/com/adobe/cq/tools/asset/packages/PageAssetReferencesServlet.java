package com.adobe.cq.tools.asset.packages;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.utils.HttpClientUtil;
import com.adobe.cq.utils.PackageHelper;
import com.adobe.cq.utils.PackageHelper.ConflictResolution;
import com.day.cq.wcm.api.Page;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.Packaging;

@SlingServlet(label = "Generic - Page Asset References Service", 
			  description = "Get Asset reference from page(s) Service", 
			  methods = { "POST" }, 
			  resourceTypes = { "/apps/aem-tools/component/tool-pages" }, 
			  selectors = { "pageAssetPackage" }, 
			  extensions = { "json" }
			 )
public class PageAssetReferencesServlet extends SlingAllMethodsServlet {

	Logger logger = LoggerFactory.getLogger(getClass());

	private static final long serialVersionUID = 9147809528194807800L;
	
	private static final String DOMAIN = "http://localhost:4502";

	private static final String AUTHOR_AUTHENTCATE_URL = "/libs/granite/core/content/login.html/j_security_check";
	
	private static final String USERNAME = "username";

	private static final String PASSWORD = "password";
	
	private static final String PACKAGE_DESCRIPTION = "packageDescription";

	private static final String PACKAGE_GROUP_NAME = "packageGroupName";

	private static final String PACKAGE_NAME = "packageName";

	private static final String PACKAGE_VERSION = "packageVersion";

	private static final String PACKAGE_CONFLICT_RESOLUTION = "conflictResolution";
	
	private static final String TARGET_PATH = "targetPath";

	private static final String TARGET_ELEMENT = "targetEle";

	private static final String TARGET_ATTRIBUTE = "targetAttr";
	
	@Reference   
	private Packaging packaging;
	
	@Reference
	private PackageHelper packageHelper;

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	protected void doPost(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws IOException {
		logger.debug("start to create JCR package");
		final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));
		logger.trace("Preview mode: {}", preview);
		try {
			final Map<String, String> packageDefinitionProperties = new HashMap<String,String>();
			final ValueMap properties = this.getProperties(request);
			String authorAuthentcateUsername = properties.get(USERNAME, "admin");
			String authorAuthentcatePassword = properties.get( PASSWORD, "");;
			String groupName = properties.get(PACKAGE_GROUP_NAME, getDefaultPackageGroupName());
			String name = properties.get(PACKAGE_NAME, getDefaultPackageName());
			String version = properties.get(PACKAGE_VERSION, getDefaultPackageVersion());
			packageDefinitionProperties.put(
			           JcrPackageDefinition.PN_DESCRIPTION,
			           properties.get(PACKAGE_DESCRIPTION, getDefaultPackageDescription()));
			String  conflictResolutionStr =  properties.get(PACKAGE_CONFLICT_RESOLUTION, getDefaultConflictResolution());
			ConflictResolution conflictResolution = ConflictResolution.valueOf(conflictResolutionStr);
			final String[] targetPaths = properties.get(TARGET_PATH, new String[]{});
			final String[] targetEles = properties.get(TARGET_ELEMENT, new String[]{});
			final String[] targetAttrs = properties.get(TARGET_ATTRIBUTE, new String[]{});
			
			HttpClientContext localContext = new HttpClientContext();
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpClientUtil.autenticate(httpclient, localContext, DOMAIN, AUTHOR_AUTHENTCATE_URL, authorAuthentcateUsername, authorAuthentcatePassword);
			
			final Set<PathFilterSet> pathFilterSets = new HashSet<PathFilterSet>();
			for(String pagePath : targetPaths){
				ResourceResolver resourceResolver = request.getResourceResolver();
				Resource rootPageRes = resourceResolver.getResource(pagePath);
				Page rootPage = rootPageRes.adaptTo(Page.class);
				//Find resurces from page itself
				if(rootPage!=null&&rootPage.isValid()){
					findPathFilterSets(pathFilterSets,httpclient,localContext, rootPage.getPath()+".html", targetEles, targetAttrs);
				}
				
				//Find resurces from child pages 
				String pageResJcrSql2 = "select * from [cq:Page] as s where ISDESCENDANTNODE(s,'"+pagePath+"')";
                Iterator<Resource> pageRes = resourceResolver.findResources(pageResJcrSql2, Query.JCR_SQL2);
                while (pageRes.hasNext()) {
                	Resource res = pageRes.next();
                	String path = res.getPath() + ".html";
                	findPathFilterSets(pathFilterSets,httpclient,localContext, path, targetEles, targetAttrs);
				}
				
                //Find resurces from file references of child pages 
				String jcrSql2 = "select * from [nt:unstructured] as s where ISDESCENDANTNODE(s,'"+pagePath+"') and s.[fileReference] is not null";
                Iterator<Resource> resources = resourceResolver.findResources(jcrSql2, Query.JCR_SQL2);
                while (resources.hasNext()) {
                    Resource resource = resources.next();
                    ValueMap imgProp = resource.adaptTo(ValueMap.class);
                    String[] fileReferences = imgProp.get("fileReference", new String[]{});
                    for(String fileReference : fileReferences){
	                    if (fileReference != null && fileReference.startsWith("/content/dam/")) {
							pathFilterSets.add(new PathFilterSet(fileReference));
						}
                    }
                }
			}
			
			if (preview) {
	            response.getWriter().print(packageHelper.getPathFilterSetPreviewJSON(pathFilterSets));
	        }else if (pathFilterSets == null || pathFilterSets.isEmpty()) {
	            response.getWriter().print(packageHelper.getErrorJSON("Refusing to create a package with no filter set rules."));
	        }else {
	        	JcrPackage jcrPackage = packageHelper.createPackageFromPathFilterSets(pathFilterSets, request.getResourceResolver().adaptTo(Session.class), groupName, name, version, conflictResolution, packageDefinitionProperties);
	        	String thumbnailPath = getPackageThumbnailPath();
	        	if (thumbnailPath != null) {
					packageHelper.addThumbnail(jcrPackage,request.getResourceResolver().getResource(thumbnailPath));
				}
	        	logger.debug("Successfully created JCR package");
				response.getWriter().print(packageHelper.getSuccessJSON(jcrPackage));
	        }
			
		} catch (IOException e) {
			logger.error(e.getMessage());
			response.getWriter().print(packageHelper.getErrorJSON(e.getMessage()));
		} catch (RepositoryException e) {
			logger.error(e.getMessage());
			response.getWriter().print(packageHelper.getErrorJSON(e.getMessage()));
		} catch (JSONException e) {
			logger.error(e.getMessage());
			response.getWriter().print(packageHelper.getErrorJSON(e.getMessage()));
		}
	}
	
	private void findPathFilterSets(Set<PathFilterSet> pathFilterSets,CloseableHttpClient httpclient,HttpClientContext localContext, String path, String[] targetEles, String[] targetAttrs){
		String html = HttpClientUtil.doGetWithHtml(httpclient, localContext, DOMAIN + path);
		Document doc = Jsoup.parseBodyFragment(html);
		Element body = doc.body();
		for (String tag : targetEles) {
			Elements eles = body.getElementsByTag(tag);
			Iterator<Element> eleIte = eles.iterator();
			while (eleIte.hasNext()) {
				Element ele = eleIte.next();
				for (String attr : targetAttrs) {
					String imgSrc = ele.attr(attr);
					if (imgSrc != null && imgSrc.startsWith("/content/dam/")) {
						pathFilterSets.add(new PathFilterSet(imgSrc));
					}
				}
			}
		}
	}
	
	private ValueMap getProperties(final SlingHttpServletRequest request) {
		if (request.getResource().getChild("configuration") == null) {
			logger.warn("Packager Configuration node could not be found for: {}",request.getResource());
			return new ValueMapDecorator(new HashMap<String, Object>());
		} else {
			return request.getResource().getChild("configuration").adaptTo(ValueMap.class);
		}
	}
	
	protected String getDefaultPackageDescription() {
		return "This Package initially defined by a AEM Tools - Page Asset Packager Configuration.";
	}

	protected String getDefaultPackageGroupName() {
		return "Page-Asset-Ref";
	}

	protected String getDefaultPackageName() {
		return "My-Package";
	}

	protected String getPackageThumbnailPath() {
		return null;
	}
	
	protected String getDefaultConflictResolution() {
		return PackageHelper.ConflictResolution.IncrementVersion.toString();
	}

	protected String getDefaultPackageVersion() {
		return "1.0.0";
	}
}
