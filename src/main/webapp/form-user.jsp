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
			<h3 class="page-header">${action eq 'update' ? 'Editar Usuário' : 'Adicionar Usuário'}</h3>

			<form action="${pageContext.request.contextPath}/user/${action}" method="POST">
				<input type="hidden" value="${user.id}" name="userId">
				<div class="row">
					<div class="form-group col-md-4 ${not empty errors.name ? 'has-error' : ''}">
					<label for="name">Nome</label>
						<input type="text" class="form-control" id="name" name="name" 
							   autofocus="autofocus" placeholder="Nome do usuário" 
							   required oninvalid="this.setCustomValidity('Por favor, informe o nome do usuário.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${user.name}' />">
						<c:if test="${not empty errors.name}"><span class="help-block"><c:out value="${errors.name}" /></span></c:if>
					</div>

					<div class="form-group col-md-4 ${not empty errors.gender ? 'has-error' : ''}">
						<label for="gender">Sexo</label>
						<select id="gender" class="form-control selectpicker" name="gender" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o sexo')"
							    oninput="setCustomValidity('')">
					      <option value="" disabled ${user.gender == 'F' or user.gender == 'M' ? "" : "selected"}>Selecione o sexo</option>
						  <option value="F" ${user.gender == 'F' ? "selected" : ""}>Feminino</option>
						  <option value="M" ${user.gender == 'M' ? "selected" : ""}>Masculino</option>
						</select>
						<c:if test="${not empty errors.gender}"><span class="help-block"><c:out value="${errors.gender}" /></span></c:if>
					</div>
					
					<div class="form-group col-md-4 ${not empty errors.email ? 'has-error' : ''}">
					<label for="email">E-mail</label>
						<input type="email" class="form-control" id="email" name="email" 
							   placeholder="E-mail do usuário" 
							   required oninvalid="this.setCustomValidity('Por favor, informe o email do usuário.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${user.email}' />">
						<c:if test="${not empty errors.email}"><span class="help-block"><c:out value="${errors.email}" /></span></c:if>
					</div>
				</div>
				<div class="row">
					<div class="form-group col-md-4 ${not empty errors.password ? 'has-error' : ''}">
					<label for="password">Senha</label>
						<input type="password" class="form-control" id="password" name="password" 
							   autocomplete="new-password" 
							   placeholder="${action eq 'update' ? 'Deixe em branco para manter a senha atual' : 'Senha do usuário'}" 
							   ${action eq 'update' ? '' : 'required'}
							   oninvalid="this.setCustomValidity('Por favor, informe a senha.')"
							   oninput="setCustomValidity('')">
						<c:if test="${not empty errors.password}"><span class="help-block"><c:out value="${errors.password}" /></span></c:if>
					</div>
				</div>
				<hr />
				<div id="actions" class="row pull-right">
					<div class="col-md-12">
						<a href="${pageContext.request.contextPath}/users" class="btn btn-default">Cancelar</a>
						<button type="submit" class="btn btn-primary">${action eq 'update' ? "Alterar Usuário" : "Cadastrar Usuário"}</button>
					</div>
				</div>
			</form>
		</div>

		<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
		<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
	</body>
</html>
