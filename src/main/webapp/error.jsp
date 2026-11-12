<%@ page contentType="text/html; charset=UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="pt-br">
	<head>
		<%@include file="base-head.jsp"%>
		<title>Erro</title>
	</head>
	<body>
		<div id="container" class="container">
			<div class="alert alert-danger" style="margin-top: 40px;">
				<h3>Algo deu errado</h3>
				<p>Não foi possível concluir a operação. Tente novamente em instantes.</p>
				<a class="btn btn-default" href="${pageContext.request.contextPath}/">Voltar para o início</a>
			</div>
		</div>
	</body>
</html>
