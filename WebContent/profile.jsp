<%@page import="de.yovi.chat.api.ActionHandlerRemote"%>
<%@page import="de.yovi.chat.system.ActionHandler"%>
<%@page import="de.yovi.chat.api.ProfileImage"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="de.yovi.chat.ChatUtils"%>
<%@page import="de.yovi.chat.web.ActionServlet"%>
<%@page import="de.yovi.chat.api.User"%>
<%@page import="de.yovi.chat.web.SessionParameters"%>
<%@page import="de.yovi.chat.user.ProfileHandler"%>
<%@page import="de.yovi.chat.user.ProfileHandlerRemote"%>
<%@page import="de.yovi.chat.api.Profile"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	boolean redirect = false;
	if (session == null) {
		redirect = true;
	}
	User user = (User) session.getAttribute(SessionParameters.USER);
	if (user == null) {
		redirect = true;
	}
	ActionHandlerRemote ah = new ActionHandler();
	if (!ah.isActive(user)) {
		redirect = true;
	}
	Profile profile = null;
	boolean editable;
	if (redirect) {
		response.sendRedirect("login.jsp");
		editable = false;
	} else {
		ProfileHandlerRemote phr = new ProfileHandler();
		String profileUser = request.getParameter("user");
		if (profileUser == null) {
			profileUser = user.getUserName();
		}
		profile = phr.getProfile(user, profileUser);
		if (profile == null) {
			response.sendError(404);
		}
		if (user.getUserName().equalsIgnoreCase(profileUser)) {
			editable = Boolean.parseBoolean(request.getParameter("edit"));
		} else {
			editable = false;
		}
	}
	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	if (profile != null) {
%>
<!DOCTYPE html>
<head>
	<title><%=profile.getName() + "'s Profil"%></title>
	<link href="css/bootstrap.min.css" rel="stylesheet" />
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<% if (editable) {%>
	<link href="css/bootstrap-fileupload.min.css" rel="stylesheet" />
<% } %>
	<style type="text/css">
	
		.carousel-inner .item .left {
			position:relative;
			width:160px; 
			float:left;
			margin-left: 40px;
		}
		.carousel-inner .item .middle {
			position:relative;
			width:160px; 
			float:left;
			margin-left: 20px;
		}
		.carousel-inner .item .right {
			position:relative;
			width:160px; 
			float:left;
			margin-left: 20px;
		
		}
		
		.carousel-inner .item .delete {
			position:absolute; 
			top:10px; 
			right:10px; 
		}
		
		.carousel-indicators {
			position: relative;
			top: 0;
			right: 0;
		}
		
		.carousel-indicators li {
			background-color: #000;
		}
		
		.carousel-indicators .active {
			background-color: #0bb;
		}
	</style>
	<script src="js/jquery.min.js" type="text/javascript"></script>
	<script src="js/bootstrap.min.js" type="text/javascript"></script>
	<script src="js/chat.js" type="text/javascript"></script>
<% if (editable) {%>
	<script src="js/jquery.form.js" type="text/javascript"></script>
	<script src="js/bootstrap-fileupload.min.js" type="text/javascript"></script>
	<script src="js/edit.js" type="text/javascript"></script>
	<script type="text/javascript">
		inheritsize = false;

		function showProfile() {
			readProfile("Peter", fillProfile);
		}

		function saveLocation() {
			var locationDiv = $('#location');
			doSaveProfile('location', locationDiv.find('input').val());
			edit(locationDiv, false, true);
			return false;
		}

		function saveAbout() {
			var aboutDiv = $('#about');
			doSaveProfile('about', aboutDiv.find('textArea').val());
			edit(aboutDiv, false, true);
			return false;
		}

		function saveBirthday() {
			var birthdayDiv = $('#birthday');
			doSaveProfile('birthday', birthdayDiv.find('input').val());
			edit(birthdayDiv, false, true);
			return false;
		}

		function saveAvatar() {
			var avatarDiv = $('#avatar');
			avatarDiv.find('form').submit();
			edit(avatarDiv, false, false);
			return false;
		}

		function saveGallery() {
			var galleryDiv = $('#gallery');
			galleryDiv.find('form').submit();
			return false;
		}

		function calculateAge(date) {
			var year = date.substring(0, 4);
			var month = date.substring(5, 7);
			var day = date.substring(8, 10);
			var now = new Date();
			var thisYear = now.getFullYear();
			var thisMonth = now.getMonth() + 1;
			var thisDay = now.getDate();
			var age = thisYear - year;
			if (thisMonth < month) {
				age -= 1;
			} else if (thisMonth = month) {
				if (thisDay < day) {
					age -= 1;
				}
			}
			return age;
		}
		
		function addToCarousel(id, title, description) {
			var carousel =  $('#galleryCarousel');
			var inner = carousel.find('.carousel-inner');
			var indicators = carousel.find('.carousel-indicators');
			var lastIndicator = indicators.find('li:last');
			var lastItem = inner.find('.item:last');
			var lastIndex = (lastItem.length == 0 ? -1 : lastIndicator.attr('data-slide-to'));
			var subItem;
			if (lastItem.length == 0 || lastItem.find('.right').length) {
				lastItem = $('<div class="item">');
				inner.append(lastItem);
				subItem = $('<div class="left">');
				lastItem.append(subItem);
				lastIndicator = $('<li data-target="#galleryCarousel">');
				lastIndex = lastIndex + 1;
				lastIndicator.attr('data-slide-to', lastIndex);
				indicators.append(lastIndicator);
			} else if (lastItem.find('.middle').length) {
				subItem = $('<div class="right">');
				lastItem.append(subItem);
			// (lastDiv.find('.left').length)
			} else {
				subItem = $('<div class="middle">');
				lastItem.append(subItem);
			}
			// add caption to item
			var caption = $('<div class="caption">');
			caption.html('<h5>' + title + '</h5>');
			subItem.append(caption);
			var image = $('<img class="img-rounded">');
			image.attr('src', 'data/db.image.thumb.' + id);
			image.attr('alt', description);
			subItem.append(image);
			var deleteA= $('<a class="delete" href="#" onclick="deleteGallery(this)">');
			deleteA.attr('data-id', id);
			subItem.append(deleteA);
			carousel.removeData('carousel');
			carousel.carousel();
		}
		
		function deleteGallery(element) {
			var id = $(element).attr('data-id');
			doSaveProfile('delete', id, function() {
				var parent = $(element).parent();
				parent.find('h5').html("Deleted");
				var img = parent.find('img');
				img.attr('src','deleted.png');
				img.attr('onclick', '');
				$(element).remove();
			});
		}

		$(document).ready(function() {
			var avatarFormOptions = { 
				beforeSubmit: function() {
					var form = $('#avatarForm');
					var fileValue = form.find('input[type="file"]').val();
					if (fileValue != null && fileValue != '') {
						var pbar = $('#avatarProgress');
						pbar.find('.bar').css('width', 0);
						pbar.css('visibility', 'visible');
						pbar.css('position', 'relative');
						pbar.css('width', form.css('width'));
						pbar.css('height', form.css('height'));
						form.css('visibility', 'hidden');
						form.css('position', 'absolute');
					}
				},
			    success: function(data) { 
					var form = $('#avatarForm');
					if (form.css('visibility') == 'hidden') {
						var pbar = $('#avatarProgress');
						pbar.css('visibility', 'hidden');
						pbar.css('position', 'absolute');
						form.css('visibility', 'visible');
						form.css('position', 'relative');
			    	}
					var img = $('#avatar').find('.editable-view').find('img');
					var title = form.find('input[name="title"]').val();
					form.find('.fileupload').fileupload('clear');
					img.attr('src', 'data/db.image.preview.' + data);
					img.attr('data-id', data);
					img.attr('alt', title);
			    },
				uploadProgress: function(event, position, total, percentComplete) {
					var progress = $('#avatarProgress');
					progress.find('.bar').css('width', percentComplete + "%");
				}
			}; 
			$('#avatarForm').ajaxForm(avatarFormOptions);
			var galleryFormOptions = { 
					beforeSubmit: function() {
						var form = $('#galleryForm');
						var fileValue = form.find('input[type="file"]').val();
						if (fileValue != null && fileValue != '') {
							var pbar = $('#galleryProgress');
							pbar.find('.bar').css('width', 0);
							pbar.css('visibility', 'visible');
							pbar.css('position', 'relative');
							pbar.css('width', form.css('width'));
							pbar.css('height', form.css('height'));
							form.css('visibility', 'hidden');
							form.css('position', 'absolute');
						}
					},
				    success:    function(data) { 
						var form = $('#galleryForm');
						if (form.css('visibility') == 'hidden') {
							var pbar = $('#galleryProgress');
							pbar.css('visibility', 'hidden');
							pbar.css('position', 'absolute');
							form.css('visibility', 'visible');
							form.css('position', 'relative');
				    	}
						var title = $('#galleryForm').find('input[name="title"]').val();
						$('#galleryForm').find('.fileupload').fileupload('clear');
						$('#galleryForm').find('input[name="title"]').val('');
						addToCarousel(data, title, "");
				    },
					uploadProgress: function(event, position, total, percentComplete) {
						var progress = $('#galleryProgress');
						progress.find('.bar').css('width', percentComplete + "%");
					}
				}; 
			$('#galleryForm').ajaxForm(galleryFormOptions);
			$('#birthday').on('view', function(event, result) {
				var date = $(this).find('input').val();
				var age = calculateAge(date);
				result.value = age + "&nbsp;Jahre&nbsp;alt";
			});
		});
	</script>
<% } %>
	<script type="text/javascript">
	<!--
		function fillProfile(data) {
			var avatarget = $('#profileHead').find('.media-object');
			avatarget.attr('data-src', data.avatarurl);
			avatarget.attr('src', data.avatarurl);
			avatarget.attr('alt', data.avatartext);
			var bodytarget = $('#profileHead').find('.media-body');
			bodytarget.empty();
			bodytarget
					.append('<h4 class="media-heading">'
							+ data.name
							+ '&nbsp;<img src=\"img/Male-256.png\" alt=\"male\" style=\"width:16px; height:16px\"></h4>');
			bodytarget.append('<p>Letzte Anmeldung: ' + data.lastlogin + '</p>');
			bodytarget.append('<p>' + data.about + '</p>');
			var gallery = $('#profileGallery');
			var galleryIndicators = gallery.find('.carousel-indicators');
			galleryIndicators.empty();
			var galleryBody = gallery.find('.carousel-inner');
			galleryBody.empty();
			$.each(data.images, function(i, image) {
				var indicator = $('<li data-target="#profileGallery"></li>');
				indicator.attr('data-slide-to', i);
				if (i == 0) {
					indicator.addClass('active');
				}
				// galleryIndicators.append(indicator);
				var item = $('<div class="item">');
				var img = $('<img>');
				img.css('margin', '0px auto');
				img.css('display', 'block');
				img.attr('src', image.url);
				img.attr('alt', image.title);
				galleryBody.append(item);
				item.append(img);
				var caption = $('<div class="carousel-caption">');
				caption.append('<h4>' + image.title + '</h4>');
				item.append(caption);
			});
		}
		
		function showImage(element) {
			var source = $(element);
			var id = source.attr('data-id');
			if (id != 'null') {
				var dialog = $('#imageModal');	
				dialog.find('#imageTitle').html(source.attr('data-title'));
				dialog.find('#imageLink').attr('href', 'data/db.image.' + id);
				dialog.find('img').attr('src', 'data/db.image.' + id);
				dialog.modal('show');
			}
		}
		
	//-->
	</script>
</head>
<body>
<div class="well" style="margin: 50px auto; width: 610px;">
		<div class="media" id="profileHead">
			<% 
			ProfileImage avatar = profile.getImage();
			String imageURL =  avatar != null ? "data/db.image.preview." + avatar.getID() : "data/null"; 
			String imageTitle = avatar != null ? avatar.getTitle() : "";
			%>
			<div style="position: relative; margin-left: 10px; width: 240px; height: 240px; float: left">
				<% if (editable) { %>
				<div id="avatar" class="editable">
					<a class="editable-view pull-left" href="#"> <img
						class="img-rounded"
						style="vertical-align: top; width: 240px; height: 240px"
						src="<%=imageURL%>"
						alt="<%=imageTitle %>"
						onclick="showImage(this)"
						data-id="<%=avatar != null ? avatar.getID() : "null"%>"
						data-title="<%=avatar != null ? avatar.getTitle() : "null"%>"
						>
					</a>
					<div class="editable-editor">
						<form id="avatarForm" action="input" method="post">
							<div class="fileupload fileupload-new" data-provides="fileupload">
								<div class="fileupload-new" style="width: 240px; height: 240px;">
									<img src="<%=imageURL%>" />
								</div>
								<div class="fileupload-preview fileupload-exists thumbnail" style="width: 240px; height: 240px;">
								</div>
								<div style="position:absolute; bottom:0; left:0">
									<span class="btn btn-file" style="float:left"> 
										<span class="fileupload-new"><i class="icon-file"></i></span> 
										<span class="fileupload-exists"><i class="icon-refresh"></i></span> 
										<input type="file" name="avatarfile" />
									</span> 
									<input type="hidden" name="action" value="profile" /> 
									<input type="hidden" name="image" value="avatar" />
									<input class="fileupload-exists" type="text" name="title" value="" placeholder="Title" style="width: 180px; height: 25px;" />
								</div>
							</div>
						</form>
						<div id="avatarProgress" class="progress progress-striped active" style="visibility:hidden">
							<div class="bar" style="width: 0%;"></div>
						</div>
					</div>
					<div style="position: absolute; top: 5px; right: 40px;">
						<span class="editable-isviewing" style="position: absolute; width:16px; background-color: #fff; border: 1px dashed #ccc">
							<a href="#" onclick="edit($('#avatar'), true, true)"><i class="icon-pencil"></i></a>
						</span> 
						<span class="editable-isediting" style="position: absolute; width:32px; background-color: #fff; border: 1px dashed #ccc">
							<a href="#" onclick="saveAvatar();"><i class="icon-ok"></i></a> 
							<a href="#" onclick="edit($('#avatar'), false, false);"><i class="icon-remove"></i></a>
						</span>
					</div>
				</div>
				<% } else { %>
					<a class="pull-left" href="#"> <img
						style="vertical-align: top; width: 240px; height: 240px"
						class="img-rounded"
						src="<%=imageURL%>"
						alt="<%=imageTitle %>"
						onclick="showImage(this)"
						data-id="<%=avatar != null ? avatar.getID() : "null"%>"
						>
					</a>
				<% } %>
			</div>
			<div style="position: relative; padding: 20px; overflow: auto; height: 240px;">
				<h3><%=profile.getName()%></h3>
				<% if (editable) { %>
				<div class="editable" id="location" style="position: relative">
					<p class="editable-view" style="height:1em"><%=profile.getLocation() != null ? ChatUtils.escape(profile.getLocation()) : "&nbsp;"%></p>
					<input type="text" style="width: 100%; height:28px" class="editable-editor" placeholder="Additional Info">
					<div style="position: absolute; top: 0; right: 40px;">
						<span class="editable-isviewing" style="position: absolute; width:20px">
							<a href="#" onclick="edit($('#location'), true, true)"><i class="icon-pencil"></i></a>
						</span> 
						<span class="editable-isediting" style="position: absolute; width:40px">
							<a href="#" onclick="saveLocation();"><i class="icon-ok"></i></a> 
							<a href="#" onclick="edit($('#location'), false, false);"><i class="icon-remove"></i></a>
						</span>
					</div>
				</div>
				<% } else { %>
				<p><%=profile.getLocation() != null ? ChatUtils.escape(profile.getLocation()) : "&nbsp;"%></p>
				<% }%>
				<% if (editable) { %>
				<div class="editable" id="birthday" style="position: relative">
					<p class="editable-view"><%=profile.getDateOfBirth() == null ? "?" : ChatUtils.calculateAge(profile.getDateOfBirth())%>
						Jahre alt
					</p>
					<input type="date" class="editable-editor" style="height:28px" placeholder="YYYY-MM-DD">
					<div style="position: absolute; top: 5px; right: 40px;">
						<span class="editable-isviewing" style="position: absolute; width:20px">
							<a href="#" onclick="edit($('#birthday'), true, true)"><i class="icon-pencil"></i></a>
						</span> 
						<span class="editable-isediting" style="position: absolute; width:40px">
							<a href="#" onclick="saveBirthday();"><i class="icon-ok"></i></a> 
							<a href="#" onclick="edit($('#birthday'), false, false);"><i class="icon-remove"></i></a>
						</span>
					</div>
				</div>
				<% } else { %>
				<p><%=profile.getDateOfBirth() == null ? "?" : ChatUtils.calculateAge(profile.getDateOfBirth())%> Jahre alt
				</p>
				<% } %>
				<p>
					<i>Letzter Login am <%=sdf.format(profile.getLastLogin())%></i>
				</p>
			</div>
		</div>
		<ul class="nav nav-tabs">
			<li class="active"><a href="#about" data-toggle="tab">About</a></li>
			<li><a href="#gallery" data-toggle="tab">Gallery</a></li>
			<!-- 
			<li><a href="#friends" data-toggle="tab">Friends</a></li>
			 -->
		</ul>
		<div class="tab-content" style="padding: 0; height: 270px; overflow: auto">
			<div id="about" style="position: relative" class="tab-pane active">
			<% if (editable) { %>
				<div class="editable">
					<p class="editable-view"><%=profile.getAbout() != null ? ChatUtils.escape(profile.getAbout()) : ""%></p>
					<textArea class="editable-editor" style="width: 100%; height: 240px; overflow: auto; resize: none;">
					</textArea>
					<div style="position: absolute; top: 0; right: 40px;">
						<span class="editable-isviewing" style="position: absolute; width:20px">
							<a href="#" onclick="edit($('#about'), true, true)"><i class="icon-pencil"></i></a>
						</span> 
						<span class="editable-isediting" style="position: absolute; width:40px">
							<a href="#" onclick="saveAbout();"><i class="icon-ok"></i></a> 
							<a href="#" onclick="edit($('#about'), false, false);"><i class="icon-remove"></i></a>
						</span>
					</div>
				</div>
			<% } else { %>
					<p><%=profile.getAbout() != null ? ChatUtils.escape(profile.getAbout()) : ""%></p>
			<% } %>
			</div>
			<div id="gallery" class="tab-pane" style="position:relative">
				<div id="galleryCarousel" class="carousel slide">
					<% 
						ProfileImage[] images = profile.getCollage();
					%>
					<div class="carousel-inner" style="height: 215px;">
					<% 
						for (int i = 0; i < images.length; i++) {
							int mod = i % 3;
							if (mod == 0) {
								if (i == 0) {
									out.write("<div class=\"item active\">");
								} else {
									out.write("</div><div class=\"item\">");
								}
								out.write("<div class=\"left\">");
							} else if (mod == 1) {
								out.write("<div class=\"middle\">");
							} else {
								out.write("<div class=\"right\">");
							}
							out.write("<div class=\"caption\">");
							out.write("<h5>" + ChatUtils.escape(images[i].getTitle()) + "</h5>");
							out.write("</div>");
							out.write("<img class=\"img-rounded\" alt=\"" + images[i].getDescription() + "\" data-id=\"" + images[i].getID() + "\"  data-title=\"" + images[i].getTitle() + "\" onclick=\"showImage(this)\" src=\"data/db.image.thumb." + images[i].getID() + "\"  alt=\"" + images[i].getDescription() + "\">");
							if (editable) {
								out.write("<a href=\"#\" data-id=\"" + images[i].getID() + "\" onclick=\"deleteGallery(this);\" class=\"delete\"><i class=\"icon-trash\"></i></a>");
							}
							out.write("</div>");
						}
						if (images.length > 0) {
							out.write("</div>");
						}
					%>
					</div>
					<!-- Carousel nav -->
					<a class="carousel-control left" href="#galleryCarousel" data-slide="prev">&lsaquo;</a> 
					<a class="carousel-control right" href="#galleryCarousel" data-slide="next">&rsaquo;</a>
					<ol class="carousel-indicators">
					<% 
						int pages = (int) Math.ceil((double) images.length / 3);
						for (int i = 0; i < pages; i++) {
							if (i == 0) {
							%><li data-target="#galleryCarousel" data-slide-to="<%=i%>" class="active"></li><%
							} else {
							%><li data-target="#galleryCarousel" data-slide-to="<%=i%>"></li><%
							}
						}
					%>
						<!--
						<li data-target="#galleryCarousel" data-slide-to="0" class="active"></li>
						<li data-target="#galleryCarousel" data-slide-to="1"></li>
						<li data-target="#galleryCarousel" data-slide-to="2"></li>
						-->
					</ol>
				</div>
				<% if (editable) {%>
				<div>
						<form id="galleryForm" action="input" method="post" style="margin:0">
							<div class="fileupload fileupload-new" style="margin:0" data-provides="fileupload">
								<div>
									<span class="btn btn-file" style="float:left"> 
										<span class="fileupload-new"><i class="icon-file"></i>&nbsp;Upload&nbsp;Image</span> 
										<span class="fileupload-exists"><i class="icon-refresh"></i>&nbsp;Change</span> 
										<input type="file" name="avatarfile" />
									</span> 
									<input type="hidden" name="action" value="profile" /> 
									<input type="hidden" name="image" value="gallery" />
									&nbsp;
									<input class="fileupload-exists" type="text" name="title" value="" placeholder="Title" style="margin:0; width: 200px; height: 25px;" />
									&nbsp;
									<span class="fileupload-exists"><a href="#" class="btn" onclick="saveGallery();"><i class="icon-ok"></i></a></span> 
								</div>
							</div>
						</form>
						<div id="galleryProgress" class="progress progress-striped active" style="visibility:hidden">
							<div class="bar" style="width: 0%;"></div>
						</div>
					</div>
				</div>
				<% } %>
			</div>
			<!-- 
			<div class="tab-pane" id="friends">
				<div class="tabbable tabbable-bordered tabs-right">
					<ul class="nav nav-tabs">
						<li class="active"><a href="#flToll" data-toggle="tab">Toll</a></li>
						<li><a href="#flDoof" data-toggle="tab">Doof</a></li>
					</ul>
					<div class="tab-content" style="padding: 0">
						<div class="tab-pane" id="flToll">tollllll</div>
						<div class="tab-pane" id="flDoof">dooooof</div>
					</div>
				</div>
			</div>
			 -->
		</div>
	</div>
<!-- Modal -->
<div id="imageModal" class="modal hide fade" tabindex="-1" role="dialog" aria-hidden="true">
  <div class="modal-header" style="height:28px">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
    <h3 id="imageTitle"></h3>
  </div>
  <div class="modal-body" style="overflow: hidden; text-align: center;">
  	<a id="imageLink" href="#" target="_blank">
    	<img id="imageElement" src="data/null" style="max-height:370px;"/>
    </a>
  </div>
</div>
<%}%>