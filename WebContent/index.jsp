<%@page import="de.yovi.chat.api.ActionHandlerRemote"%>
<%@page import="de.yovi.chat.web.SessionParameters"%>
<%@page import="de.yovi.chat.system.ActionHandler"%>
<%@page import="de.yovi.chat.api.User"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	String logoutKey = (String) session.getAttribute(SessionParameters.LOGOUT_KEY);
	User user = (User) session.getAttribute(SessionParameters.USER);
	ActionHandlerRemote ah = new ActionHandler();
	if (user == null || !ah.isActive(user)) {
		session.setAttribute(SessionParameters.USER, null);
		response.sendRedirect("login.jsp");
	} else if (Boolean.TRUE.equals(session.getAttribute("refresh"))) {
		ah.resetAndRejoin(user);
	} else {
		session.setAttribute("refresh", true);
	}
%>
<!DOCTYPE html>
<html>

<head>
	<link id="favicon" rel="icon" href="favicon.ico" type="image/x-icon">
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Hallo <%=user == null ? "unbekanntes Wesen" : user.getUserName()%></title>
	<link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
	<link href="css/bootstrap-responsive.min.css" rel="stylesheet" media="screen">
	<link href="css/bootstrap-fileupload.min.css" rel="stylesheet" media="screen">
	<style>
		body {
			height: 100%;
			width: 100%;
			position: absolute;
			background-color: #E6E6FA;
		}
		.username {
			font-weight:bold; 
		}
		.useralias {
			font-weight:bold; 
			font-style:italic; 
		}
		
		#chatnav {
			border: 1px solid #ddd;
			margin-bottom: 5px; 
		}
		
		#chatdiv {
			height: 80%;
			width: 100%;
			padding-left: 0px;
			padding-right: 0px;
			margin-bottom: 5px; 
			border: 1px solid #ddd;
		}
		
		/* #channelBox { */
		/* 	height: 100%; */
		/* 	/* 			background-color:blue; */ */
		/* 	border: 1px solid #ddd; */
		/* 	border-collapse: collapse; */
		/* } */
		#channelMedia li {
			border-bottom: 1px dashed #ddd;
			overflow:hidden;
			text-overflow:ellipsis;
			white-space:nowrap;
			margin-left: 5px;
		}
		#channelMedia li a {
			width:10em; 
			overflow:hidden; 
			text-overflow: ellipsis;
		}
		#channelUsers li {
			margin-left: 5px;
		}
		.stretch {
			height: 100%
		}
		
		#previewBox {
			position: absolute;
			width: 290px;
			bottom: 12px;
			right: 24px;
			margin:0;
			padding: 0;
			background-color:#ffffff;
			z-index: 100;
		}
		
		#previewImg {
			margin: auto auto;
		}
		#previewText {
			width: 270px;
			margin: 0px 10px;
			overflow:hidden;
			text-overflow:ellipsis;
			white-space:nowrap;
		}
		
	</style>
	<script type="text/javascript" src="js/jquery.min.js"></script>
	<script type="text/javascript" src="js/jquery.form.js"></script>
	<script type="text/javascript" src="js/bootstrap.min.js"></script>
	<script type="text/javascript" src="js/bootstrap-fileupload.min.js"></script>
	<script type="text/javascript" src="js/chat.js"></script>
	<script type="text/javascript">
		var showBgImage = true;
		var scrolling = true;
		var currentBgImage = null;
		
		function roomSelected() {
			var room = $("#roomSelect").find('option:selected').attr('data-room');
			if (room != null) {
				doJoin(room);
			}
		}
		
		function toolify(element) {
			$(element).attr('onmouseover', '');
			$(element).tooltip(); 
			$(element).tooltip('show');
		}
	
		function preview(element, clicked) {
			if ($(element).is("[data-preview]")) {
				$("#previewImg").attr("src", $(element).attr("data-preview"));
				var link = $(element).attr("href");
				var text = $(element).html();
				$("#previewLink").attr("href", link);
				$("#previewText").html(text);
				$(element).addClass("previewed");
			}
			return false;
		}
		
		function appendUser(userList, user, withAka) {
			var userLi = $('<li>');
			// if the user is a 'normal' user
			if (user.guest == 'false') {
				// add a hyperlink
				var userA = $('<a target="_blank">');
				userA.attr('href', 'profile.jsp?user=' + user.username);
				userList.append(userA);
				userA.append(userLi);
				// if the user has an avatar, show a tooltip for it
				if (user.avatar != null && user.avatar != 'null') {
					userA.attr('data-title', '<img src="data/db.image.pinky.' + user.avatar + '" style="width:64px;height:64px"/>');
					userA.attr('data-toggle', 'tooltip');
					userA.attr('data-html', 'true');
					userA.attr('data-placement', 'left');
					userA.tooltip();
				}
			} else {
				userList.append(userLi);
			}
			var userI = $('<i>');
			userLi.append(userI);
			// colorize the icons background in the users color
			userI.css('background-color', '#' + user.color);
			if (user.away == "true") {;
				userI.addClass('icon-remove-sign');
			} else {
				userI.addClass('icon-user');
			}
			// append their name
			userLi.append('&nbsp;' + user.username);
			if (withAka && user.alias != null && user.alias != 'null') {
				userLi.append('&nbsp;(' + user.alias + ')');
			}
		}
		
		function guiRefresh(data) {
			// ### Refresh UserList ###
			// get the selector for the userlist and clear it
			var userList = $('#channelUsers');
			userList.empty();
			// iterate over returned users
			$.each(data.users, function(i, user) {
				appendUser(userList, user, true);
			});	
			var otherList = $('#otherUsers');
			otherList.empty();
			// iterate over returned users
			$.each(data.others, function(i, user) {
				appendUser(otherList, user, false);
			});
			// ### Refresh MediaList ###
			// selector for medialist and clear it
			var mediaList = $('#channelMedia');
			mediaList.empty();
			if (data.medias != null) {
				$.each(data.medias, function(i, media) {
					var mediaLi = $('<li>');
					mediaList.append(mediaLi);
					var mediaA = $('<a>');
					mediaLi.append(mediaA);
					mediaA.attr('target', '_blank');
					mediaA.attr('href', media.link);
					if (media.preview != null && media.preview != 'null') {
						mediaA.attr('onmouseover', 'preview(this, false)');
						mediaA.attr('data-preview', media.preview);
					}
					if (media.pinky != null && media.pinky != 'null') {
						mediaA.attr('data-title', '<img src="' + media.pinky + '" style="width:64px;height:64px"/>');
						mediaA.attr('data-toggle', 'tooltip');
						mediaA.attr('data-html', 'true');
						mediaA.attr('data-placement', 'left');
						mediaA.tooltip();
					}
					var mediaI = $('<i>');
					mediaA.append(mediaI);
					if (media.type == "IMAGE") {
						mediaI.addClass('icon-picture');
					} else if (media.type == "VIDEO") {
						mediaI.addClass('icon-film');
					} else if (media.type == "WEBSITE") {
						mediaI.addClass('icon-globe');
					} else if (media.type == "PROTOKOLL") {
						mediaI.addClass('icon-list-alt');
					}
					mediaA.append('&nbsp;' + media.name);
					if (media.user != null && media.user != "null") {
						mediaLi.append('<br/><i class="icon-user"></i>&nbsp' + media.user);
					}
				});
				$('#tabMediaButton').css('visibility', 'visible');
			} else {
				$('#tabMediaButton').css('visibility', 'hidden');
				$('#channelBox [href="#tabUsers"]').tab('show');
			}
			// ### Refresh RoomList ###
			var roomList = $('#roomSelect');
			roomList.empty();
			// initial dummy option
			roomList.append("<option selected>switch rooms</option>");
			$.each(data.rooms, function(i, r) {
				var roomOption = $('<option>');
				roomList.append(roomOption);
				roomOption.attr('data-room', r.name);
				roomOption.append(r.name);
				// usercount in brackets!
				roomOption.append('&nbsp;(' + r.users + ')');
			});
			$('#channelName').html(data.room);
			// ### Styling of the chatframe ###
			var chatFrame = $('#chatdiv');
			chatFrame.css("background-color", "#" + data.background);
			chatFrame.css("color", "#" + data.foreground);
			if (data.backgroundimage != currentBgImage) {
				currentBgImage = data.backgroundimage;
				displayBgImage(chatFrame);
			}
	 		var chatFrameBody = chatFrame.find('iframe').contents().find('body');
	 		if (chatFrameBody.length == 0) {
	 			chatFrameBody = chatFrame; 
	 		} 
			if (data.font != null && data.font != 'null') {
				chatFrameBody.css("font-family", data.font);
			}
			// ### Toggle the AwayOption ###
			if (data.away == "true") {
				$('#awayIcon').toggleClass('icon-remove-circle', true);
				$('#awayIcon').toggleClass('icon-remove-sign', false);
				$('#awayAction').attr('onclick','away(false)');
				$('#awayText').html('Available');
			} else {
				$('#awayIcon').toggleClass('icon-remove-circle', false);
				$('#awayIcon').toggleClass('icon-remove-sign', true);
				$('#awayAction').attr('onclick','away(true)');
				$('#awayText').html('Away');
			}
			var count = countUnread();
			if (count > 0) {
				$('#navMessageButton').html("<b>Messages (" + count + ")</b>");
			} else {
				$('#navMessageButton').html("Messages");
			}
			didRefresh = true;
		}
		
		function refresh() {
			doRefresh(guiRefresh);
		}
		
		function toggleBgImage(state) {
			showBgImage = state;
			displayBgImage($('#chatdiv'));
		}
		function displayBgImage(chatFrame) {
			if (showBgImage && currentBgImage != null && currentBgImage != 'null') {
				chatFrame.css('background-image', 'url("' + currentBgImage + '")');
				chatFrame.css('background-size', 'cover');
				chatFrame.css('background-repeat', 'no-repeat');
				chatFrame.css('background-position', '0 50%');
			} else {
				chatFrame.css('background-image', '');
				chatFrame.css('background-size', '');
				chatFrame.css('background-repeat', '');
				chatFrame.css('background-position', '');
			}
		}
		
		function openProfile(user) {
			window.open("profile.jsp?user=" + user,'chat_profil');
		}

		function resize () {
			var bodyHeight = $("body").height();
			var height = bodyHeight - $('#chatnav').outerHeight(true) - $('#chatform').outerHeight(true);
		    $("#chatdiv").height((height - 40) + "px");
			var channelHeight = bodyHeight - 18;
			$('#channelcontent').height((channelHeight - $('#channelnav').outerHeight(true)) + "px");
		}
		
		
		
		$(document).ready(function() {
			resize();
			$(window).resize(resize);
			var talkFormOptions = { 
				beforeSubmit: function() {
					var talk = $('#talkinput');
					talk.attr("disabled", true);
					var form = $('#talkForm');
					var fileValue = form.find('input[type="file"]').val();
					if (fileValue != null && fileValue != '') {
						var pbar = $('#talkProgress');
						pbar.find('.bar').css('width', 0);
						pbar.css('visibility', 'visible');
						pbar.css('position', 'relative');
						pbar.css('width', form.css('width'));
						pbar.css('height', form.css('height'));
						form.css('visibility', 'hidden');
						form.css('position', 'absolute');
					}
				},
			    success:    function() { 
					var form = $('#talkForm');
					if (form.css('visibility') == 'hidden') {
						var pbar = $('#talkProgress');
						pbar.css('visibility', 'hidden');
						pbar.css('position', 'absolute');
						form.css('visibility', 'visible');
						form.css('position', 'relative');
			    	}
					var talk = $('#talkinput');
					talk.attr("disabled", false);
					talk.val('');
					form.find('.fileupload').fileupload('clear');
					talk.focus();
			    },
				uploadProgress: function(event, position, total, percentComplete) {
					var progress = $('#talkProgress');
					progress.find('.bar').css('width', percentComplete + "%");
				}
			}; 
			$('#talkForm').ajaxForm(talkFormOptions);
				refresh();
				$('#talkinput').focus();
		});
	</script>
</head>


<body>
	
	<div id="previewBox" class="accordion well">
		<div class="accordion-group">
			<div class="accordion-heading" style="height:30px;">
				<a class="accordion-toggle btn btn-block" data-toggle="collapse"
					data-parent="#previewBox" style="padding: 4px 15px" href="#preview">Sneak'a'Peek&nbsp;<i class="icon-eye-open"></i></a>
			</div>
			<div id="preview" class="accordion-body collapse" style="border: 1px solid ddd">
				<div class="accordion-inner">
					<img id="previewImg" class="img-rounded" />
					<br/>
					<a id="previewLink" href="about:_blank" target="_blank">
					</a>
					<br/>
					<p id="previewText">No preview loaded</p>
				</div>
			</div>
		</div>
	</div>
	<div class="container-fluid">
		<div class="row-fluid">
			<div class="span9">
				<div id="chatnav" class="navbar navbar-static-top">
					<div class="navbar-inner">
						<a class="btn btn-navbar" data-toggle="collapse" data-target=".navbar-responsive-collapse">
		                    <span class="icon-bar"></span>
		                    <span class="icon-bar"></span>
		                    <span class="icon-bar"></span>
		                  </a>
						<div id="channelName" class="brand"></div>
						<div class="nav-collapse collapse navbar-responsive-collapse">
							<form class="navbar-search pull-left">
								<select id="roomSelect" contenteditable="false" onchange="roomSelected()">
								</select>
							</form>
							<ul class="nav pull-right">
								<li><a href="help.html" target="_blank">Help</a></li>
								<li class="divider-vertical"></li>
								<%
									if (user != null && !user.isGuest()) {
								%>
								<li><a href="profile.jsp?edit=true" target="chat_profil">My&nbsp;Profile</a></li>
								<li><a href="#messageDialog" id="navMessageButton"
									data-toggle="modal" onclick="showBothBox()">Messages</a></li>
								<%
									}
								%>
								<li class="dropdown"><a href="#" class="dropdown-toggle"
									data-toggle="dropdown">User<b class="caret"></b></a>
									<ul class="dropdown-menu">
										<%
											if (user != null && !user.isGuest()) {
										%>
										<%
											if (user.isTrusted()) {
										%>
										<li><a href="#inviteDialog" data-toggle="modal"><i
												class="icon-share"></i>&nbsp;Invite</a></li>
										<%
											}
										%>
										<li><a href="#" id="awayAction" onclick="away(true)"><i
												id="awayIcon" class="icon-remove-sign"></i>&nbsp;<span
												id="awayText">Set&nbsp;Away</span></a></li>
										<%
											}
										%>

										<li><a href="#settingsDialog" data-toggle="modal"><i class="icon-wrench"></i>&nbsp;Settings</a></li>
										<li class="divider"></li>
										<li><a href="session?action=logout&key=<%=logoutKey%>"><i class="icon-off"></i>&nbsp;Logout</a></li>
									</ul>
								</li>
							</ul>
						</div>
					</div>
				</div>
				<div id="chatdiv" class="well well-small" >
					<% if (user == null) { %>
						Irgendwas geht hier nicht! 
					<% } else if (user.hasAsyncMode()) {%>
						<%@include file="chatout-async.jsp" %>
					<% } else {%>
						<%@include file="chatout-sync.jsp" %>
					<% } %>
				</div>
				<div id="chatform" style="position:relative; height: 54px;">
					<form id="talkForm" action="input" method="post" style="float:left; height:100%; margin-bottom:0px" class="form-inline"> 
					    <input type="hidden" name="action" value="talk" /> 
					    <input id="talkinput" class="input-xlarge" style="margin-bottom:0" type="text" name="message" autocomplete="off" />
					    <div class="fileupload fileupload-new" data-provides="fileupload" style="display:inline">
							<span class="btn btn-file">
								<span class="fileupload-new"><i class="icon-file"></i></span>
								<span class="fileupload-exists"><i class="icon-refresh"></i></span>
								<span class="fileupload-preview"></span>
								<input type="file" name="talkfile" id="talkfile" />
							</span>
						</div>
						<br />
						<label class="checkbox">
					      Autoscroll&nbsp;<input type="checkbox" checked="checked" onclick="scrolling=this.checked;">
					    </label>
						<label class="checkbox">
					      Show&nbsp;Backgrounds&nbsp;<input type="checkbox" checked="checked" onclick="toggleBgImage(this.checked);">
					    </label>
					</form>
					<div id="talkProgress" class="progress progress-striped active" style="visibility:hidden; height:30px">
						<div class="bar" style="width: 0%;"></div>
					</div>
				</div>
			</div>
			<div id="channelBox" class="tabbable tabbable-bordered span3" style="padding-top:8px" >
				<!-- Only required for left/right tabs -->
				<ul id="channelnav" class="nav nav-tabs" style="border-bottom:0px; margin-bottom:0px">
					<li class="active"><a href="#tabUsers" data-toggle="tab">Users</a></li>
					<li><a href="#tabMedia" id="tabMediaButton" style="visibility:hidden"  data-toggle="tab">Media</a></li>
				</ul>
				<div id="channelcontent" class="tab-content well well-cmls" style="white-space:nowrap; padding:0px; border-bottom: 0px; margin-bottom:0px">
					<div class="tab-pane active" id="tabUsers"  style="margin:15px; overflow-x:hidden; text-overflow:ellipsis;">
						<ul id="channelUsers" class="unstyled">

						</ul>
						<hr/>
						<ul id="otherUsers" class="unstyled" style="font-style: italic">

						</ul>
					</div>
					<div class="tab-pane" id="tabMedia" style="margin:15px; overflow-x:hidden; text-overflow:ellipsis;">
						<ul id="channelMedia" class="unstyled">

						</ul>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- container-fluid -->
	<%@include file="invite.jsp" %>
	<%@include file="messages.jsp" %>
	<%@include file="settings.jsp" %>
	
</body>
</html>
