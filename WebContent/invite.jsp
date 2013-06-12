<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<script type="text/javascript">
<!--
	function createKey() {
		var selectedVal = "";
		var selected = $("#inviteForm input[type='radio']:checked");
		if (selected.length > 0) {
			selectedVal = selected.val();	
		}
		var key = invite(selectedVal);
		if (key != null && key != 'null') {
			var resultDiv = $('#inviteForm div');
			resultDiv.css('visibility', 'visible');
			$('#keyResult').html(key);
		}
	}
//-->
</script>
<div class="modal hide fade" id="inviteDialog">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Settings</h3>
	</div>
	<div class="modal-body tab-content">
		<form id="inviteForm">
			
			<label class="radio">
				<input type="radio" name="trial" value="true"/> 
				24 Hour-Key
			</label> 
			<span class="help-block">The invitee may enter the chat within the next 24 hours</span> 
			
			<label class="radio">
				<input type="radio" name="trial" value="false"/>
				Registration 
			</label>
			<span class="help-block">The invitee may register permanently</span>
			<div style="visibility:hidden">
				<p>Give this key to your invitee</p>
				<span id="keyResult" style="font-weight: bold"></span>
			</div>
		</form>
	</div>
	<div class="modal-footer">
		<ul class="nav nav-pills" style="margin-bottom: 0">
			<li class="active"><a href="#" onclick="createKey()">Invite</a></li>
		</ul>
	</div>
</div>