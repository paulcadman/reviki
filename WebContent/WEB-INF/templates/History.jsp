<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="f" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/functions' prefix='fn' %>
<%@ taglib uri="http://www.hillsdon.net/ns/reviki/tags" prefix="sw" %>
 
 <c:set var="encodedPageName" value="${sw:urlEncode(page.name)}" />
 
<tiles:insertTemplate template="SiteTemplate.jsp">
  <tiles:putAttribute name="title">History for <c:out value="${page.title}"/></tiles:putAttribute>
  <tiles:putAttribute name="heading">History for <a href="<c:url value="${encodedPageName}"/>"><c:out value="${page.title}"/></a></tiles:putAttribute>
  <tiles:putAttribute name="content">
    <form method="get" name="compareForm" action="<c:url value="${encodedPageName}"/>">
	    <table class="history">
	      <tr class="history">
	        <th class="history" style="white-space: nowrap;">Date</th>
	        <th class="history" style="white-space: nowrap;">Page</th>
	        <th class="history">User</th>
	        <th class="history">Description</th>
	        <th class="history" colspan="2"><input type="submit" value="Compare" name="compare" /></th>
	      </tr>
	      <tr class="history">
	        <td class="history" style="white-space: nowrap;"></td>
	        <td class="history" style="white-space: nowrap;"></td>
	        <td class="history"></td>
	        <td class="history"></td>
	        <td class="history">from</td>
	        <td class="history">to&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
	      </tr>
	      <c:forEach var="change" items="${changes}" varStatus="status">
	        <tr class="history">
	          <td class="history" style="white-space: nowrap;">
	            <c:choose>
	              <c:when test="${change.deletion}">
	                <del>
	                  <a href="<c:url value="${change.name}"/>?revision=${change.revision}&amp;diff=${change.revision - 1}">
	                    <f:formatDate type="both" value="${change.date}"/>
	                  </a>
	                </del>
	              </c:when>
	              <c:otherwise>
	                <a href="<c:url value="${change.name}"/>?revision=${change.revision}&amp;diff=${change.revision - 1}">
	                  <f:formatDate type="both" value="${change.date}"/>
	                </a>
	              </c:otherwise>
	            </c:choose>
	          </td>
	          <td class="history" style="white-space: nowrap;">
	            <c:choose>
	              <c:when test="${change.deletion}">
	                <del>r<c:out value="${change.revision}"/></del>
	              </c:when>
	              <c:otherwise>
	                <a href="<c:url value="${change.name}"/>?revision=${change.revision}"><c:out value="${change.name}" /> r<c:out value="${change.revision}"/></a>
                    (<a href="<c:url value="${change.name}"/>?revision=${change.revision}&amp;ctype=raw">source</a>)
	              </c:otherwise>
	            </c:choose>
	          </td>
	          <td class="history">
	            <c:out value="${change.user}"/>
	          </td>
	          <td class="history">
	            <c:out value="${change.description}"/>
	          </td>
	          <td class="history">
	          	<c:choose>
	          	  <c:when test="${status.index == 1 || status.index == 0 && fn:length(changes) == 1}">
	          	    <input type="radio" name="diff" value="${change.name}.${change.revision}" checked="checked"/>
	          	  </c:when>
	          	  <c:otherwise>
	          	    <input type="radio" name="diff" value="${change.name}.${change.revision}"/>
	          	  </c:otherwise>
	          	</c:choose>
	          </td>
	          <td class="history">
	          <c:choose>
	            <c:when test="${status.index == 0}">
	              <input type="radio" name="revision" value="${change.name}.${change.revision}" checked="checked"/>
	            </c:when>
	          	<c:otherwise>
	          	  <input type="radio" name="revision" value="${change.name}.${change.revision}"/>
	          	</c:otherwise>
	          </c:choose>
	          </td>
	        </tr>
	      </c:forEach>
	    </table>
    </form>
  </tiles:putAttribute>
</tiles:insertTemplate>
