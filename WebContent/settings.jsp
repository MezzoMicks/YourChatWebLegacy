<%@page import="de.yovi.chat.api.User"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	String userColor = "000000";
	String fontFamily = null;
	String favouriteRoom = "";
	String settingsUserName = "";
	boolean asyncmode = false;
	User settingsUser = (User) request.getSession().getAttribute("user");
	if (settingsUser != null) {
		settingsUserName = settingsUser.getUserName();
		if (settingsUser.getColor() != null) {
			userColor = settingsUser.getColor();
		}
		if (settingsUser.getFont() != null) {
			fontFamily = settingsUser.getFont();
		}
		if (settingsUser.getFavouriteRoom() != null) {
			favouriteRoom = settingsUser.getFavouriteRoom();
		}
		asyncmode = settingsUser.hasAsyncMode();
	}

%>
<script src="js/fontdetect.js" type="text/javascript"></script>
<script type="text/javascript">
<!--
function saveSettings() {
	var color = $('#userColor').val();
	var room = $('#favouriteRoom').val();
	var font = $('#fontSelector').find('option:selected').attr('data-font');
	var asyncMode = $('#asyncCheck').is(':checked')?true:false;
	if (font == 'null') {
		font = null;
	}
	if (settings(font, color, room, asyncMode)) {
		$('#settingsDialog').modal('hide');
	}
}

function font_init() {
	var fonts = [];
	var d = new Detector();
    fonts.push("cursive");
    fonts.push("monospace");
    fonts.push("serif");
    fonts.push("sans-serif");
    fonts.push("fantasy");
    fonts.push("default");
    fonts.push("Arial");
    fonts.push("Arial Black");
    fonts.push("Arial Narrow");
    fonts.push("Arial Rounded MT Bold");
    fonts.push("Bookman Old Style");
    fonts.push("Bradley Hand ITC");
    fonts.push("Century");
    fonts.push("Century Gothic");
    fonts.push("Comic Sans MS");
    fonts.push("Courier");
    fonts.push("Courier New");
    fonts.push("Georgia");
    fonts.push("Gentium");
    fonts.push("Impact");
    fonts.push("King");
    fonts.push("Lucida Console");
    fonts.push("Lalit");
    fonts.push("Modena");
    fonts.push("Monotype Corsiva");
    fonts.push("Papyrus");
    fonts.push("Tahoma");
    fonts.push("TeX");
    fonts.push("Times");
    fonts.push("Times New Roman");
    fonts.push("Trebuchet MS");
    fonts.push("Verdana");
    fonts.push("Verona");
    var currentFont = "<%=fontFamily%>";
    var options = '<option data-font="null"';
    if (currentFont == null) {
    	options += ' selected="selected"';
    }
    options += '>&lt;Standard&gt;</option>';
    for (var i = 0; i < fonts.length; i++) {
    	var font = fonts[i];
	    if (d.detect(font)) {
	    	options += '<option';
	    	if (currentFont == font) {
	    		options += ' selected="selected"';
	    	}
	    	options += ' data-font="' + font + '"><span style="font-family:' + font + '">'+ font + '</span></option>';
	    }
    }
    $('#fontSelector').html(options);
}

function userColorChange() {
	var userColor = $('#userColor');
	$('#userNameHelp').css('color', '#' + userColor.val());
}

function fontChange() {
	var font = $('#fontSelector').find('option:selected').attr('data-font');
	$('#fontHelp').css('font-family', font);
}
$(document).ready(function() {
    font_init();
    userColorChange();
    fontChange();
});
//-->
</script>

<div class="modal hide fade" id="settingsDialog">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Settings</h3>
	</div>
	<div class="modal-body tab-content">
   		<label>Nickcolor</label>
		<input id="userColor" value="<%=userColor%>" onchange="userColorChange()"/>
    	<span class="help-block">
    	You can specify a color, in which your name will be represented, refer to  <a href="help.html#colors" target="_blank">this link</a> for finding your colorcode
    	<span id="userNameHelp" style="font-weight:bold"><%=settingsUserName%></span>: that's my color</span>
   		<label>Entrance-Room</label>
		<input id="favouriteRoom" type="text" value="<%=favouriteRoom%>"/>
    	<span class="help-block">The room you wish to enter upon login</span>
   		<label>Chatfont</label>
		<select id="fontSelector" onchange="fontChange()" contenteditable="false"></select>
    	<span class="help-block"><span id="fontHelp">The chat will look like this to you</span></span>
   		<label class="checkbox">
   			<input type="checkbox"  id="asyncCheck" <%=asyncmode ? "checked='checked'" :"" %> /> Alternate Chatmode
		</label>
    	<span class="help-block">Enable this, if you got trouble, reading the chat</span>
	</div>
	<div class="modal-footer">
		<ul class="nav nav-pills" style="margin-bottom: 0">
			<li class="active"><a href="#" data-toggle="tab" onclick="saveSettings()">Save</a></li>
		</ul>
	</div>
</div>