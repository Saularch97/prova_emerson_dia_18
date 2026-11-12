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
			<h3 class="page-header">${action eq 'update' ? 'Editar Curso' : 'Adicionar Curso'}</h3>

			<form action="${pageContext.request.contextPath}/course/${action}" method="POST">
				<input type="hidden" value="${course.id}" name="courseId">
				<div class="row">
					<div class="form-group col-md-8 ${not empty errors.title ? 'has-error' : ''}">
						<label for="title">Título</label>
						<input type="text" class="form-control" id="title" name="title" 
							   autofocus="autofocus" placeholder="Título do curso" maxlength="150" 
							   required oninvalid="this.setCustomValidity('Por favor, informe o título do curso.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${course.title}' />">
						<c:if test="${not empty errors.title}"><span class="help-block"><c:out value="${errors.title}" /></span></c:if>
					</div>

					<div class="form-group col-md-4 ${not empty errors.level ? 'has-error' : ''}">
						<label for="level">Nível</label>
						<select id="level" class="form-control selectpicker" name="level" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o nível.')"
							    oninput="setCustomValidity('')">
						  <option value="" ${not empty course.level ? "" : "selected"}>Selecione o nível</option>
						  <c:forEach var="l" items="${levels}">
						  	<option value="${l.name()}" ${course.level == l ? "selected" : ""}>
						  		<c:out value="${l.label}" />
						  	</option>
						  </c:forEach>
						</select>
						<c:if test="${not empty errors.level}"><span class="help-block"><c:out value="${errors.level}" /></span></c:if>
					</div>
				</div>

				<div class="row">
					<div class="form-group col-md-12 ${not empty errors.description ? 'has-error' : ''}">
						<label for="description">Descrição</label>
						<textarea class="form-control" id="description" name="description" rows="4" 
							   placeholder="Descrição do curso" 
							   required oninvalid="this.setCustomValidity('Por favor, informe a descrição do curso.')"
							   oninput="setCustomValidity('')"><c:out value="${course.description}" /></textarea>
						<c:if test="${not empty errors.description}"><span class="help-block"><c:out value="${errors.description}" /></span></c:if>
					</div>
				</div>

				<div class="row">
					<div class="form-group col-md-3 ${not empty errors.price ? 'has-error' : ''}">
						<label for="price">Preço (R$)</label>
						<input type="number" step="0.01" min="0" class="form-control" id="price" name="price" 
							   placeholder="0,00" 
							   required oninvalid="this.setCustomValidity('Por favor, informe um preço válido.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${not empty priceInput ? priceInput : course.price}' />">
						<c:if test="${not empty errors.price}"><span class="help-block"><c:out value="${errors.price}" /></span></c:if>
					</div>

					<div class="form-group col-md-3 ${not empty errors.startDate ? 'has-error' : ''}">
						<label for="startDate">Data de início</label>
						<input type="date" class="form-control" id="startDate" name="startDate" 
							   required oninvalid="this.setCustomValidity('Por favor, informe a data de início.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${course.startDate}' />">
						<c:if test="${not empty errors.startDate}"><span class="help-block"><c:out value="${errors.startDate}" /></span></c:if>
					</div>

					<div class="form-group col-md-4 ${not empty errors.instructor ? 'has-error' : ''}">
						<label for="instructor">Instrutor</label>
						<select id="instructor" class="form-control selectpicker" name="instructor" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o instrutor.')"
							    oninput="setCustomValidity('')">
						  <option value="" ${not empty course.instructor ? "" : "selected"}>Selecione um instrutor</option>
						  <c:forEach var="u" items="${users}">
						  	<option value="${u.id}" ${course.instructor.id == u.id ? "selected" : ""}>
						  		<c:out value="${u.name}" />
						  	</option>
						  </c:forEach>
						</select>
						<c:if test="${not empty errors.instructor}"><span class="help-block"><c:out value="${errors.instructor}" /></span></c:if>
					</div>

					<div class="form-group col-md-2">
						<label for="active">Ativo</label>
						<div class="checkbox">
							<label>
								<input type="checkbox" id="active" name="active" value="true" ${empty course or course.active ? "checked" : ""}> Curso ativo
							</label>
						</div>
					</div>
				</div>
				<hr />
				<div id="actions" class="row pull-right">
					<div class="col-md-12">
						<a href="${pageContext.request.contextPath}/courses" class="btn btn-default">Cancelar</a>
						<button type="submit" class="btn btn-primary">${action eq 'update' ? "Alterar Curso" : "Cadastrar Curso"}</button>
					</div>
				</div>
			</form>
		</div>

		<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
		<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
	</body>
</html>
