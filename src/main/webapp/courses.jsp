<jsp:directive.page contentType="text/html; charset=UTF-8" />
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="pt-br">
	<head>
		<%@include file="base-head.jsp"%>
	</head>
	<body>
		<%@include file="modal.html"%>
		<%@include file="nav-menu.jsp"%>
			
		<div id="container" class="container-fluid">
			<div id="alert" style="${not empty message ? 'display: block;' : 'display: none;'}" class="alert alert-dismissable ${alertType eq 1 ? 'alert-success' : 'alert-danger'}">
			  <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
			  ${message}
			</div>
		
	 	 	<div id="top" class="row">
	 			<div class="col-md-3">
			        <h3>Cursos</h3>
			    </div>
			 
			    <div class="col-md-6">
			        <div class="input-group h2">
			            <input name="data[search]" class="form-control" id="search" type="text" placeholder="Pesquisar cursos">
			            <span class="input-group-btn">
			                <button class="btn btn-danger" type="submit">
			                    <span class="glyphicon glyphicon-search"></span>
			                </button>
			            </span>
			        </div>
			    </div>
			 
			    <div class="col-md-3">
			        <a href="${pageContext.request.contextPath}/course/form" class="btn btn-danger pull-right h2"><span class="glyphicon glyphicon-plus" /></span>&nbspAdicionar Curso</a>
			    </div>
	     	</div>
	 
	     	<hr />
	     	
	     	<div id="list" class="row">
	     		<div class="table-responsive col-md-12">
			        <table class="table table-striped table-hover" cellspacing="0" cellpadding="0">
			            <thead>
			                <tr>
			                    <th>Título</th>
			                    <th>Nível</th>
			                    <th>Preço</th>
			                    <th>Início</th>
			                    <th>Ativo</th>
			                    <th>Instrutor</th>
			                    <th>Editar</th>
			                    <th>Excluir</th>
			                 </tr>
			            </thead>
			            <tbody>
			            	<c:forEach var="course" items="${pageData.items}">
								<tr>
				                    <td><c:out value="${course.title}" /></td>
				                    <td><c:out value="${course.level.label}" /></td>
				                    <td><c:out value="${course.price}" /></td>
				                    <td><c:out value="${course.startDate}" /></td>
				                    <td>${course.active ? 'Sim' : 'Não'}</td>
				                    <td><c:out value="${course.instructor.name}" /></td>
				                    <td class="actions">
				                        <a class="btn btn-info btn-xs" 
				                           href="${pageContext.request.contextPath}/course/update?courseId=${course.id}" >
				                           <span class="glyphicon glyphicon-edit"></span>
				                        </a>
				                    </td>
				                    <td class="actions">
				                        <a class="btn btn-danger btn-xs modal-remove"
				                           course-id="${course.id}" 
				                           course-title="<c:out value='${course.title}' />" data-toggle="modal" 
				                           data-target="#delete-modal"  href="#"><span 
				                           class="glyphicon glyphicon-trash"></span></a>
				                    </td>
				                </tr>
							</c:forEach>
							<c:if test="${empty pageData.items}">
								<tr>
									<td colspan="8" class="text-center">Nenhum curso cadastrado.</td>
								</tr>
							</c:if>
			            </tbody>
			         </table>
			 
			     </div>
	     	</div>
	 
	     	<div id="bottom" class="row">
	     		<div class="col-md-12">
			        <%@include file="pagination.jsp"%>
			    </div>
	     	</div>
		</div>
		
		<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
		<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
		<script type="text/javascript">
			$(document).ready(function() {
			    setTimeout(function() {
			        $("#alert").slideUp(500);
			    }, 3000);
			    
			    $(".modal-remove").click(function () {
		            var courseTitle = $(this).attr('course-title');
		            var courseId = $(this).attr('course-id');
		            $(".modal-body #hiddenValue").text("o curso '"+courseTitle+"'");
		            $("#id").attr( "value", courseId);
		            $("#form").attr( "action","${pageContext.request.contextPath}/course/delete");
		            $("#modal-page").val(${pageData.page});
		        })
			});
		</script>
	</body>
</html>
