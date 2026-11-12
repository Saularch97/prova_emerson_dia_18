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
			<h3 class="page-header">${action eq 'update' ? 'Editar Matrícula' : 'Adicionar Matrícula'}</h3>

			<form action="${pageContext.request.contextPath}/enrollment/${action}" method="POST">
				<input type="hidden" value="${enrollment.id}" name="enrollmentId">
				<div class="row">
					<div class="form-group col-md-4 ${not empty errors.student ? 'has-error' : ''}">
						<label for="student">Aluno</label>
						<select id="student" class="form-control selectpicker" name="student" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o aluno.')"
							    oninput="setCustomValidity('')">
						  <option value="" ${not empty enrollment.student ? "" : "selected"}>Selecione um aluno</option>
						  <c:forEach var="u" items="${users}">
						  	<option value="${u.id}" ${enrollment.student.id == u.id ? "selected" : ""}>
						  		<c:out value="${u.name}" />
						  	</option>
						  </c:forEach>
						</select>
						<c:if test="${not empty errors.student}"><span class="help-block"><c:out value="${errors.student}" /></span></c:if>
					</div>

					<div class="form-group col-md-5 ${not empty errors.course ? 'has-error' : ''}">
						<label for="course">Curso</label>
						<select id="course" class="form-control selectpicker" name="course" 
							    required oninvalid="this.setCustomValidity('Por favor, informe o curso.')"
							    oninput="setCustomValidity('')">
						  <option value="" ${not empty enrollment.course ? "" : "selected"}>Selecione um curso</option>
						  <c:forEach var="co" items="${courses}">
						  	<option value="${co.id}" ${enrollment.course.id == co.id ? "selected" : ""}>
						  		<c:out value="${co.title}" />
						  	</option>
						  </c:forEach>
						</select>
						<c:if test="${not empty errors.course}"><span class="help-block"><c:out value="${errors.course}" /></span></c:if>
					</div>

					<div class="form-group col-md-3 ${not empty errors.enrolledOn ? 'has-error' : ''}">
						<label for="enrolledOn">Data da matrícula</label>
						<input type="date" class="form-control" id="enrolledOn" name="enrolledOn" 
							   required oninvalid="this.setCustomValidity('Por favor, informe a data da matrícula.')"
							   oninput="setCustomValidity('')"
							   value="<c:out value='${enrollment.enrolledOn}' />">
						<c:if test="${not empty errors.enrolledOn}"><span class="help-block"><c:out value="${errors.enrolledOn}" /></span></c:if>
					</div>
				</div>

				<div class="row">
					<div class="form-group col-md-4 ${not empty errors.status ? 'has-error' : ''}">
						<label>Situação</label>
						<div>
							<c:forEach var="s" items="${statuses}">
								<label class="radio-inline">
									<input type="radio" name="status" value="${s.name()}" required ${enrollment.status == s ? "checked" : ""}> <c:out value="${s.label}" />
								</label>
							</c:forEach>
						</div>
						<c:if test="${not empty errors.status}"><span class="help-block"><c:out value="${errors.status}" /></span></c:if>
					</div>

					<div class="form-group col-md-5 ${not empty errors.progress ? 'has-error' : ''}">
						<label for="progress">Progresso: <output id="progress-value"><c:out value="${not empty progressInput ? progressInput : enrollment.progress}" /></output>%</label>
						<input type="range" min="0" max="100" step="1" id="progress" name="progress" 
							   style="width: 100%;"
							   oninput="document.getElementById('progress-value').textContent = this.value"
							   value="<c:out value='${not empty progressInput ? progressInput : enrollment.progress}' />">
						<c:if test="${not empty errors.progress}"><span class="help-block"><c:out value="${errors.progress}" /></span></c:if>
					</div>

					<div class="form-group col-md-3">
						<label for="paid">Pagamento</label>
						<div class="checkbox">
							<label>
								<input type="checkbox" id="paid" name="paid" value="true" ${enrollment.paid ? "checked" : ""}> Pago
							</label>
						</div>
					</div>
				</div>
				<hr />
				<div id="actions" class="row pull-right">
					<div class="col-md-12">
						<a href="${pageContext.request.contextPath}/enrollments" class="btn btn-default">Cancelar</a>
						<button type="submit" class="btn btn-primary">${action eq 'update' ? "Alterar Matrícula" : "Cadastrar Matrícula"}</button>
					</div>
				</div>
			</form>
		</div>

		<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
		<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
	</body>
</html>
