<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.hillsdon.net/ns/reviki/tags" prefix="sw" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <c:set var="titlePrefix">
    <c:choose>
      <c:when test="${not empty wikiName}">${wikiName}</c:when>
      <c:otherwise>reviki</c:otherwise>
    </c:choose>
  </c:set>
  <%-- Prevent indexing of 'unusual' pages. --%>
  <% if (!request.getParameterMap().isEmpty()) { %>
  <meta name="robots" content="noindex, nofollow" />
  <% } %>
  <title><c:out value="${titlePrefix}"/> - <tiles:insertAttribute name="title"/></title>
  <link rel="shortcut icon" href="<sw:iconUrl name="favicon.ico"/>" />
  <c:if test="${wikiIsValid != null and wikiIsValid}">
    <link rel="alternate" type="application/atom+xml" title="RecentChanges feed" href="<sw:wikiUrl page="RecentChanges" query="ctype=atom"/>" />
    <link rel="search" href="<sw:wikiUrl page="FindPage" extraPath="/opensearch.xml"/>" type="application/opensearchdescription+xml" title="Wiki Search" />
  </c:if>
  <link rel="stylesheet" href="<c:url value="${cssUrl}"/>" media="all" type="text/css" />
  <link rel="stylesheet" href="<sw:resourceUrl path="themes/reviki-flat/reviki-flat.css"/>" media="screen" type="text/css" />
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.ui.core.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.ui.widget.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.ui.tabs.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.ui.position.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.ui.autocomplete.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.ui.autocomplete.html.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.hotkeys.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="jquery.textchange.js"/>"></script>
  <script type="text/javascript" src="<sw:resourceUrl path="common.js"/>"></script>
  <script type="text/javascript">
    reviki.SEARCH_URL = "<sw:wikiUrl page="FindPage"/>";
    $(function() {
    	$(".nojs").removeClass("nojs");
    });
  </script>
</head>
<body>
  <c:if test="${not empty flash}">
    <div id="flash">
      <p>
        <c:out value="${flash}"/>
      </p>
    </div>
  </c:if>
  <div id="topbar" class="auxillary">
    <c:if test="${wikiIsValid != null and wikiIsValid}">
      <ul class="menu">
        <c:set var="menuItems"><tiles:getAsString name="menuItems" ignore="true"/></c:set>
        <c:out value="${menuItems}" escapeXml="false"/>
        <c:if test="${not empty menuItems}">
          <li class="menu menu-separator"></li>
        </c:if>
        <li class="menu"><sw:wikiPage page="FrontPage"/></li>
        <li class="menu"><sw:wikiPage page="RecentChanges"/></li>
        <li class="menu"><sw:wikiPage page="AllPages"/></li>
        <li class="menu">
          <form id="searchForm" name="searchForm" style="display: inline; margin-top:0.2em;" action="<sw:wikiUrl page="FindPage"/>" method="get">
            <input id="query" name="query" type="text" value="<c:out value="${param.query}"/>"/>
            <input value="Go" type="submit"/>
          </form>
        </li>
      </ul>
    </c:if>
  </div>
  <div id="header" class="auxillary">
  ${renderedHeader}
  </div>
  <div id="content-area">
    <h1 class="title"><tiles:insertAttribute name="heading"/></h1>
    <div id="sidebar" class="auxillary" style="float:right">
    ${renderedSideBar}
    </div>
    <tiles:insertAttribute name="content"/>
  </div>
  <div id="footer" class="auxillary clear-both">
  ${renderedFooter}
    <p id="build-details">Version $Version$.</p>
  </div>
  <tiles:insertAttribute name="body-level" ignore="true" />
</body>
</html>
