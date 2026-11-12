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
			<h3 class="page-header">${action eq 'update' ? 'Editar Empresa' : 'Adicionar Empresa'}</h3>

			<form action="${pageContext.request.contextPath}/company/${action}" method="POST">
				<input type="hidden" value="${company.id}" name="companyId">
				<div class="row">
					<div class="form-group col-md-6 ${not empty errors.name ? 'has-error' : ''}">
						<label for="name">Nome</label>
						<input type="text" class="form-control" id="name" name="name" 
							   autofocus="autofocus" placeholder="Nome da empresa" 
							   required oninvalid="this.setCustomValidity('Por favor, informe o nome da empresa.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${company.name}' />">
						<c:if test="${not empty errors.name}"><span class="help-block"><c:out value="${errors.name}" /></span></c:if>
					</div>

					<div class="form-group col-md-6 ${not empty errors.role ? 'has-error' : ''}">
						<label for="role">Cargo</label>
						<input type="text" class="form-control" id="role" name="role" 
							   placeholder="Cargo ocupado" 
							   required oninvalid="this.setCustomValidity('Por favor, informe o cargo.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${company.role}' />">
						<c:if test="${not empty errors.role}"><span class="help-block"><c:out value="${errors.role}" /></span></c:if>
					</div>
				</div>

				<div class="row">
					<div class="form-group col-md-4 ${not empty errors.start ? 'has-error' : ''}">
						<label for="start">Data início</label>
						<input type="date" class="form-control" id="start" name="start" 
							   required oninvalid="this.setCustomValidity('Por favor, informe a data de início.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${company.start}' />">
						<c:if test="${not empty errors.start}"><span class="help-block"><c:out value="${errors.start}" /></span></c:if>
					</div>

					<div class="form-group col-md-4 ${not empty errors.end ? 'has-error' : ''}">
						<label for="end">Data saída</label>
						<input type="date" class="form-control" id="end" name="end" 
							   value="<c:out value='${company.end}' />">
						<c:if test="${not empty errors.end}"><span class="help-block"><c:out value="${errors.end}" /></span></c:if>
					</div>

					<div class="form-group col-md-4 ${not empty errors.user ? 'has-error' : ''}">
						<label for="user">Usuário</label>
						<select id="user" class="form-control selectpicker" name="user" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o usuário.')"
							    oninput="setCustomValidity('')">
						  <option value="" disabled ${not empty company.user ? "" : "selected"}>Selecione um usuário</option>
						  <c:forEach var="u" items="${users}">
						  	<option value="${u.id}" ${company.user.id == u.id ? "selected" : ""}>
						  		<c:out value="${u.name}" />
						  	</option>
						  </c:forEach>
						</select>
						<c:if test="${not empty errors.user}"><span class="help-block"><c:out value="${errors.user}" /></span></c:if>
					</div>
				</div>
				<hr />
				<div id="actions" class="row pull-right">
					<div class="col-md-12">
						<a href="${pageContext.request.contextPath}/companies" class="btn btn-default">Cancelar</a>
						<button type="submit" class="btn btn-primary">${action eq 'update' ? "Alterar Empresa" : "Cadastrar Empresa"}</button>
					</div>
				</div>
			</form>
		</div>

		<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
		<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
	</body>
</html>
