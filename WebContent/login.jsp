<%@page import="de.yovi.chat.web.Configurator"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% 
	boolean keyRequired = Configurator.isInvitationRequired(); 
	session.setAttribute("user", null);
	session.setAttribute("refresh", false);
%>
<!DOCTYPE html>
<html>
<head>
	<title>Login</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link rel="icon" href="favicon.ico" type="image/x-icon">
	<link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
	<script type="text/javascript" src="js/jquery.min.js"></script>
	<script type="text/javascript" src="js/bootstrap.min.js"></script>
	<script type="text/javascript" src="js/sha256.js"></script>
	<script type="text/javascript" src="js/chat.js"></script>
	<script type="text/javascript">

	var keyRequired = <%=keyRequired%>;

	function loginKey(event) {
		if (event.which == 13) {
			login();
		}
	}
	
	function login() {
		if ($('#loginTab').hasClass('active')) {
			var form = $('#loginForm');
			var password = $.trim(form.find('input[name="password"]').val());
			password = sha256_digest(password);
			form.find('input[name="password"]').val('');
			var username = form.find('input[name="username"]').val();
			var sugar = getSugar(username);
			if (sugar != null && sugar != "") {
				form.find('input[name="passwordHash"]').val(sha256_digest(sugar + password));
			} else {
				form.find('input[name="passwordHash"]').val(sha256_digest(password));
			}
			form.submit();
		}
	}
	
	function register() {
		if ($('#registerTab').hasClass('active')) {
			var form = $('#registerForm');
			var username = $.trim(form.find('input[name="username"]').val());
			var password = $.trim(form.find('input[name="password"]').val());
			var password2 = $.trim(form.find('input[name="passwordRepeat"]').val());
			if (password == '') {
				
			} else	if (password != password2) {
				$("#registerLabel").html("Supplied passwords differ!");
			} else {
				var sugar = getSugar(username);
				if (keyRequired) {
					var key = form.find('input[name="key"]').val();
					form.find('input[name="keyHash"]').val(sha256_digest(sugar + key));
				}
				form.find('input[name="passwordHash"]').val(sha256_digest(password));
				form.submit();
			}
		}
	}

	$(document).ready(function() {
		if (sha256_self_test() == false) {
			$('#loginForm').html("<strong>Login not possible!</strong>");
			$('#registerForm').html("<strong>Registration not possible!</strong>");
		}
	});
	</script>
</head>

<body>
	<div class="container" style="margin-top:50px">
	  <div class="row">
	    <div class="tab-content span6 offset3  well">
			<div class="tab-pane fade active in" id="loginTab">
				<h3>Login</h3>
				<form id="loginForm" action="session" method="POST">
					<input type="text" name="username" placeholder="Username">
					<br /> 
					<input type="password" name="password" placeholder="Password" onkeydown="loginKey(event)"> 
					<input type="hidden" name="passwordHash" /> 
					<input type="hidden" name="action" value="login" />
				</form>
			</div>
			<div class="tab-pane fade in" id="registerTab">
				<h3>Register</h3>
				<form id="registerForm" action="session" method="POST">
					<input type="text" name="username" placeholder="Username">
					<br /> 
					<input type="password" name="password" placeholder="Password">
					<br /> 
					<input type="password" name="passwordRepeat" placeholder="Password (Again)">
					<input type="hidden" name="passwordHash" /> 
					<br />
					<%
						if (keyRequired) {
					%>
					<input type="text" name="key" placeholder="Invite-Key">
					<input type="hidden" name="keyHash" /> 
					<%
						}
					%>
					<input type="hidden" name="action" value="register" />
				</form>
				<br /> <span class="label label-important" id="registerLabel"></span>
			</div>
			<ul class="nav nav-pills" style="margin-bottom:0">
				<li class="active"><a href="#loginTab" data-toggle="tab" onclick="login()">Login</a></li>
				<li><a href="#registerTab" data-toggle="tab" onclick="register()">Register</a></li>
			</ul>
	    </div>
	  </div>
	</div>
</body>
</html>