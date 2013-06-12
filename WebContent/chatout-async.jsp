<%@page import="de.yovi.chat.api.User"%>
<%@page import="de.yovi.chat.web.SessionParameters"%>
<%
User chatoutuser = (User)session.getAttribute(SessionParameters.USER);
String listenId;
if (chatoutuser == null) {
	listenId = "null";
} else {
	listenId = chatoutuser.getListenId();
}
%>
<script type="text/javascript">
var stopped = false;
var window_focus = true;

function stop() {
	stopped = true;
}

function chatListen() {
	listen("<%=listenId%>", function (data) {
		if (!window_focus && data.length > 0 && data.indexOf('<!--') == -1) {
			$("#favicon").remove();
			$('head').append('<link id="favicon" rel="icon" href="favicon_talky.gif" type="image/x-icon">');
		}
		$('#chatframe').append(data);
		if (!stopped) {
			window.setTimeout("chatListen()", 250);
		}
	});
	if (typeof scrolling == 'undefined' || scrolling) { 
		$('#chatframe').scrollTop(5000000); 
	}
}


	$(document).ready(function() {
		$(window).focus(function() {
			$("#favicon").remove();
			$('head').append('<link id="favicon" rel="icon" href="favicon.ico" type="image/x-icon">');
			window_focus = true;
		});
		$(window).blur(function() {
			window_focus = false;
		});
		chatListen();
	});
</script>

<div id="chatframe" style="border:0; margin:0 8px; padding:0; width:100%; height:100%; overflow-x:hidden">
</div>