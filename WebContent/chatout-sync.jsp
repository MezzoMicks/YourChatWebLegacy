<%@page import="de.yovi.chat.api.User"%>
<%@page import="de.yovi.chat.web.SessionParameters"%>
<%
User chatoutuser = (User)session.getAttribute(SessionParameters.USER);
String chatframeURL;
if (chatoutuser == null) {
	chatframeURL = "about:_blank";
} else {
	chatframeURL = "listen;jsessionid=" + session.getId() + "?listenid=" + chatoutuser.getListenId() + "&type=" + (chatoutuser.hasAsyncMode() ? "async" : "sync");
}
%>

<script type="text/javascript">
$(document).ready(function() {
	$('#chatframe').attr('src', '<%=chatframeURL%>');
});
</script>

<iframe id="chatframe" style="border:0; margin:0; padding:0; width:100%; height:100%;" src="">
</iframe>