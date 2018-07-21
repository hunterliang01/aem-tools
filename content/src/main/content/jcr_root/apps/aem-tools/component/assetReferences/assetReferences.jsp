<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8" %><%

    /* Package Definition */
    final String packageName = properties.get("packageName", "my-package");
    final String packageGroupName = properties.get("packageGroupName", "aem-tools");
    final String packageVersion = properties.get("packageVersion", "1.0.0");
    final String packageDescription = properties.get("packageDescription", "This Package initially defined by a AEM Tools - Page Asset Packager Configuration.");
    final String conflictResolution = properties.get("conflictResolution", "IncrementVersion");
    final String[] targetPaths = properties.get("targetPath", new String[]{});
    final String[] targetEles = properties.get("targetEle", new String[]{});
    final String[] targetAttrs = properties.get("targetAttr", new String[]{});
    
    final String username = properties.get("username", "admin");
    final String password = properties.get("password", "");
%>

<h3>Package definition</h3>
<ul>
    <li>Package name: <%= xssAPI.encodeForHTML(packageName) %></li>
    <li>Package group: <%= xssAPI.encodeForHTML(packageGroupName) %></li>
    <li>Package version: <%= xssAPI.encodeForHTML(packageVersion) %></li>
    <li>Package description: <%= xssAPI.encodeForHTML(packageDescription) %></li>
    <li>Conflict resolution: <%= xssAPI.encodeForHTML(conflictResolution) %></li>
</ul>

<h3>Targeted paths</h3>
<ul>
    <% if(targetPaths.length == 0) { %>
    <li class="not-set">Not Set</li>
    <% } %>
    <% for(final String targetPath : targetPaths) { %>
        <li><%= xssAPI.encodeForHTML(targetPath) %></li>
    <% } %>
</ul>

<h3>Target Elements</h3>
<ul>
    <% if(targetEles.length == 0) { %>
    <li class="not-set">Not Set</li>
    <% } %>
    <% for(final String targetEle : targetEles) { %>
    <li><%= xssAPI.encodeForHTML(targetEle) %></li>
    <% } %>
</ul>

<h3>Target Attributes</h3>
<ul>
    <% if(targetAttrs.length == 0) { %>
    <li class="not-set">Not Set</li>
    <% } %>
    <% for(final String targetAttr : targetAttrs) { %>
    <li><%= xssAPI.encodeForHTML(targetAttr) %></li>
    <% } %>
</ul>
<%-- Common Form (Preview / Create Package) used for submittins Packager requests --%>
<%-- Requires this configuration component have a sling:resourceSuperType of the ACS AEM Commons Packager --%>
<cq:include script="form.jsp"/>
