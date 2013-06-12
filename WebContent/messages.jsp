<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<style>
	.unread {
		font-weight: bold;
	}
</style>
<script type="text/javascript">
<!--

var currentPage = 0;
var box = 'inbox';


function showInbox() {
	showInbox(0);
}

function showInbox(page) {
	$('#newMessageButton').html('New Message');
	if (page > 4) {
		page = 4;
	}
	box = 'inbox';
	currentPage = page;
	var messages = readInbox(-1, -1);
	fillMessageTable('#inboxTab', messages, -1, true);
}

function showOutbox() {
	showOutbox(0);
}

function showOutbox(page) {
	$('#newMessageButton').html('New Message');
	if (page > 4) {
		page = 4;
	}
	box = 'outbox';
	currentPage = page;
	var messages = readOutbox(-1, -1);
	fillMessageTable('#outboxTab', messages, -1, false);
}

function msgClearWarn() {
	$('#msgWarning').html("");
}


function showBothBox() {
	showInbox();
	showOutbox();
}

function guiDeleteMessage(element) {
	var id = $(element).attr('data-id');
	deleteMessage(id, showBothBox);
}

function guiDeleteMessage(element, inbox) {
	var id = $(element).attr('data-id');
	deleteMessage(id, (inbox ? showInbox : showOutbox));
}

function guiReplyMessage(element) {
	var data = $(element);
	$('#messageDialog a[href="#newMessageTab"]').tab('show');
	$('#newMessageButton').html('Send');
	$('#msgRecipient').val(data.attr('data-recipient'));
	$('#msgSubject').val( 'RE: ' + data.attr('data-subject'));
	$('#msgBody').val('');
	$('#msgWarning').html('');
}

function newMessage() {
	if ($('#newMessageTab').hasClass('active')) {
		var recipient = $('#msgRecipient').val();
		var subject = $('#msgSubject').val();
		var body = $('#msgBody').val();
		sendMessage(recipient, subject, body, function (data) {
			if (data) {
				$('#msgRecipient').val('');
				$('#msgSubject').val('');
				$('#msgBody').val('');
				$('#messageDialog').modal('hide');
				$('#msgWarning').html("");
			} else {
				$('#msgWarning').html("Error while sending message, check your input");
			}
		});
	}
	$('#newMessageButton').html('Send');
}


function showMessage(element, inbox) {
	var id = $(element).attr('data-id');
	readMessage(id, function (message) {
		var subject = message.subject;
		subject = subject == null ||subject == 'null' ? '<Unknown>' : subject;
		var title = message.subject + '&nbsp;';
		if (inbox) {
			title += 'from';
			title += '&nbsp' +  message.sender;
		} else {
			title += 'to';
			title += '&nbsp' +  message.recipient;
		}
		$('#showMessageHeader').html(title);
		$('#showMessageBody').html(message.body);
		$('#msgDeleteButton').attr('data-id',message.id);
		$('#msgReplyButton').attr('data-subject',subject);
		$('#msgReplyButton').attr('data-recipient', inbox ?  message.sender :  message.recpient);
		refresh();
	});
}

function fillMessageTable(target, messageresult, page, inbox) {
	var table = $(target + " table");
	table.empty();
	$.each(messageresult.messages, function(i, message) {
		var row = $('<tr>');
		table.append(row);
		var cell = $('<td>');
		row.append(cell);
		var link = $('<a>').addClass('messagelink');
		link.attr('href', '#showMessageDialog').attr('data-toggle', 'modal');
		link.attr('data-id', message.id).attr('onclick', 'showMessage(this,' + inbox + ')');
		cell.append(link);
		var icon = $('<i>');
		var unread = message.read == 'false';
		icon.addClass('icon-envelope');
		icon.toggleClass('icon-white', unread);
		link.append(icon);
		link.append('&nbsp;' + message.date + '&nbsp;' + message.subject + '&nbsp;(' + (inbox ? message.sender : message.recipient) + ')');
		link.toggleClass('unread', unread);
		cell = $('<td>');
		row.append(cell);
		link = $('<a>').attr('href', '#').attr('data-id', message.id).attr('onclick', 'guiDeleteMessage(this, ' + inbox + ');');
		cell.append(link);
		icon = $('<i>').addClass('icon-remove');
		link.append(icon);
		cell = $('<td>');
		row.append(cell);
		link = $('<a>').attr('href', '#').attr('data-recipient', inbox ? message.recipient : message.sender).attr('data-subject', message.subject).attr('onclick', 'guiReplyMessage(this);');
		cell.append(link);
		icon = $('<i>').addClass('icon-share-alt');
		link.append(icon);
	});
	
}

//-->
</script>

<div class="modal hide fade" id="messageDialog" data-focus-on="input:first">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3 id="messageDialogHeader">Messages</h3>
	</div>
	<div class="modal-body tab-content">
		<div class="tab-pane fade active in" id="inboxTab" style="height:180px; overflow:auto">
			<table class="table table-condensed table-hover" >
			</table>
		</div>
		<div class="tab-pane fade in" id="outboxTab" style="height:180px; overflow:auto">
			<table class="table table-condensed table-hover">
			</table>
		</div>
		<div class="tab-pane fade in" id="newMessageTab" data-focus-on="input:first">
			<input class="span4" type="text" id="msgRecipient" placeholder="Recipient" onchange="msgClearWarn()" />
			<input class="span4" type="text" id="msgSubject" placeholder="Subject" onchange="msgClearWarn()"  />
			<textarea class="span5" title="Body" rows="3" cols=""  onchange="msgClearWarn()"  id="msgBody" maxlength="4000" style="resize:none"></textarea>
			<br/>
			<span id="msgWarning" class="label label-warning"></span>
		</div>
	</div>
	<div class="modal-footer">
		<ul class="nav nav-pills" style="margin-bottom: 0">
			<li class="active"><a href="#inboxTab" data-toggle="tab" onclick="showInbox()">Inbox</a></li>
			<li><a href="#outboxTab" data-toggle="tab" onclick="showOutbox()">Outbox</a></li>
			<li><a href="#newMessageTab" id="newMessageButton" data-toggle="tab" onclick="newMessage()">New Message</a></li>
		</ul>
	</div>
</div>

<div class="modal hide" id="showMessageDialog">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3 id="showMessageHeader"></h3>
	</div>
	<div class="modal-body tab-content" id="showMessageBody"></div>
	<div class="modal-footer">
		<ul class="nav nav-pills" style="margin-bottom: 0">
			<li><a href="#showMessageDialog" id="msgDeleteButton" data-toggle="modal" onclick="guiDeleteMessage(this);">Delete</a></li>
			<li><a href="#showMessageDialog" id="msgReplyButton" data-toggle="modal" onclick="guiReplyMessage(this)">Reply</a></li>
		</ul>
	</div>
</div>
