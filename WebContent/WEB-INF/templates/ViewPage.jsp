<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<tiles:insertTemplate template="SiteTemplate.jsp">
  <tiles:putAttribute name="title"><c:out value="${pageInfo.path} - ${pageInfo.revisionName}"/></tiles:putAttribute>
  <tiles:putAttribute name="content">
    <h1><c:out value="${pageInfo.path}"/></h1>
    <div id="content">
    ${renderedContents}
    </div>
    <hr/>
    <form action="" method="post">
      <input type="submit" value="Edit"/>
    </form> 
  </tiles:putAttribute>
</tiles:insertTemplate>
