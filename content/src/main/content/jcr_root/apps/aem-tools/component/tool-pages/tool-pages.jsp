<%@include file="/libs/foundation/global.jsp"%>
<cq:include script="/libs/wcm/core/components/init/init.jsp"/><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
    import="com.adobe.granite.xss.XSSAPI,
                com.day.cq.wcm.api.WCMMode,
                org.apache.commons.lang.StringUtils,
                com.adobe.acs.commons.util.TextUtil"%><%
	final XSSAPI xss = sling.getService(XSSAPI.class);
    /* Page Properties */
    final String pageTitle = currentPage.getTitle();
	final String pageDescription = currentPage.getDescription();
%>

<!DOCTYPE html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="Content-Type" content="text/html; utf-8" />

        <title><%= xss.encodeForHTML(pageTitle) %></title>
        <meta name="description" content="<%= xss.encodeForHTML(pageDescription) %>">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <cq:includeClientLib css="cq.wcm.edit,aem-tools.utilities.page"/>
        <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>
        <cq:includeClientLib js="cq.wcm.edit,aem-tools.utilities.page"/>

        <cq:include script="headlibs.jsp"/>
    </head>

    <body>
        <div id="acs-commons" class="acs-commons">

            <h1>Page Asset Reference Packager</h1>
            
            <div class="notifications">
                <div class="notification preview hidden">
                    <h2>Preview</h2>
            
                    <p>The following filter paths will be used in the package definition:</p>
            
                    <ul class="filters"></ul>
            
                    <p>If the above filter paths appear satisfactory, press the &quot;Create Package&quot; button below to
                        create the actual package definition in <a target="_blank" x-cq-linkchecker="skip" href="/crx/packmgr/index.jsp">CRX Package
                            Manager</a>.</p>
                </div>
            
                <div class="notification success hidden">
                    <h2>Success</h2>
            
                    <p>A new ACL package has been created at: <a class="package-manager-link" target="_blank" x-cq-linkchecker="skip" href=""><span
                            class="package-path"></span></a></p>
            
                    <ul class="filters"></ul>
            
                    <p>Go to the <a class="package-manager-link" target="_blank" x-cq-linkchecker="skip" href="">CRX Package manager</a> to build and
                        download this package.</p>
                </div>
            
                <div class="notification error hidden">
                    <h2>Error</h2>
            
                    <p>An error occurred while building the ACL Package.</p>
            
                    <p class="msg"></p>
            
                    <p>Please check the following</p>
                    <ul>
                        <li>Review your packaging settings on this page (especially Conflict Resolution)</li>
                        <li>Verify you have read and write access to /etc/packages</li>
                    </ul>
                </div>
            </div>
            
            <%-- Custom impl of the packager configuration --%>
            <cq:include path="configuration" resourceType="/apps/aem-tools/component/assetReferences"/>

		</div>
    </body>
</html>