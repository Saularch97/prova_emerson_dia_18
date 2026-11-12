<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="pt-br">
	<head>
		<%@include file="base-head.jsp"%>
		<title>Entrar - CRUD Manager</title>
	</head>
	<body>
		<div id="container" class="container">
			<div class="row">
				<div class="col-md-4 col-md-offset-4">
					<h3 class="text-center">CRUD Manager</h3>
					<div id="alert" style="${not empty message ? 'display: block;' : 'display: none;'}" class="alert alert-dismissable ${alertType eq 1 ? 'alert-success' : 'alert-danger'}">
						${message}
					</div>
					<form id="login-form" action="${pageContext.request.contextPath}/login" method="POST">
						<div class="form-group">
							<label for="email">E-mail</label>
							<input type="email" class="form-control" id="email" name="email" placeholder="E-mail" autofocus>
						</div>
						<div class="form-group">
							<label for="password">Senha</label>
							<input type="password" class="form-control" id="password" name="password" placeholder="Senha">
						</div>
						<button type="submit" class="btn btn-primary btn-block">Entrar</button>
					</form>
				</div>
			</div>
		</div>
	</body>
</html>
