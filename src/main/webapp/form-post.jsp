<jsp:directive.page contentType="text/html; charset=UTF-8" />
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="pt-br">
	<head>
		<%@include file="base-head.jsp"%>
	</head>
	<body>
		<%@include file="nav-menu.jsp"%>
			
		<div id="container" class="container-fluid">
			<h3 class="page-header">${action eq 'update' ? 'Editar Post' : 'Adicionar Post'}</h3>

			<form action="${pageContext.request.contextPath}/post/${action}" method="POST">
				<input type="hidden" value="${post.id}" name="postId">
				<div class="row">
					<div class="form-group col-md-6 ${not empty errors.content ? 'has-error' : ''}">
					<label for="content">Conteúdo</label>
						<input type="text" class="form-control" id="content" name="content" 
							   autofocus="autofocus" placeholder="Conteúdo do post" 
							   required oninvalid="this.setCustomValidity('Por favor, informe o conteúdo do post.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${post.content}' />">
						<c:if test="${not empty errors.content}"><span class="help-block"><c:out value="${errors.content}" /></span></c:if>
					</div>

					<div class="form-group col-md-6 ${not empty errors.user ? 'has-error' : ''}">
						<label for="user">Usuário</label>
						<select id="user" class="form-control selectpicker" name="user" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o usuário.')"
							    oninput="setCustomValidity('')">
						  <option value="" disabled ${not empty post.user ? "" : "selected"}>Selecione um usuário</option>
						  <c:forEach var="u" items="${users}">
						  	<option value="${u.id}" ${post.user.id == u.id ? "selected" : ""}>
						  		<c:out value="${u.name}" />
						  	</option>	
						  </c:forEach>
						</select>
						<c:if test="${not empty errors.user}"><span class="help-block"><c:out value="${errors.user}" /></span></c:if>
					</div>
				</div>
				<c:if test="${not empty errors.postDate}">
					<div class="row"><div class="col-md-12 has-error"><span class="help-block"><c:out value="${errors.postDate}" /></span></div></div>
				</c:if>
				<hr />
				<div id="actions" class="row pull-right">
					<div class="col-md-12">
						<a href="${pageContext.request.contextPath}/posts" class="btn btn-default">Cancelar</a>
						<button type="submit" class="btn btn-primary">${action eq 'update' ? "Alterar Post" : "Criar Post"}</button>
					</div>
				</div>
			</form>
		</div>

		<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
		<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
	</body>
</html>
