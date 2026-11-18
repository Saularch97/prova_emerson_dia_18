<jsp:directive.page contentType="text/html; charset=UTF-8" />
<%@ taglib prefix="pg" uri="http://java.sun.com/jsp/jstl/core" %>
<pg:if test="${pageData.totalPages > 1}">
	<nav id="pagination-nav" aria-label="Paginação">
		<ul class="pagination">
			<pg:choose>
				<pg:when test="${pageData.hasPrevious()}">
					<li><a href="${pageContext.request.contextPath}${pageBaseUrl}?page=${pageData.page - 1}">Anterior</a></li>
				</pg:when>
				<pg:otherwise>
					<li class="disabled"><span>Anterior</span></li>
				</pg:otherwise>
			</pg:choose>
			<pg:forEach begin="1" end="${pageData.totalPages}" var="n">
				<li class="${n == pageData.page ? 'active' : ''}"><a href="${pageContext.request.contextPath}${pageBaseUrl}?page=${n}">${n}</a></li>
			</pg:forEach>
			<pg:choose>
				<pg:when test="${pageData.hasNext()}">
					<li><a href="${pageContext.request.contextPath}${pageBaseUrl}?page=${pageData.page + 1}">Próximo</a></li>
				</pg:when>
				<pg:otherwise>
					<li class="disabled"><span>Próximo</span></li>
				</pg:otherwise>
			</pg:choose>
		</ul>
	</nav>
</pg:if>
