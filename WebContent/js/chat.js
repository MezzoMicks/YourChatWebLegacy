

function listen(listenid, callback) {
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "listen",
		data : {
			"listenid" : listenid,
			"output" : "async",
			timestamp : tS
		},
		async:true,
		success : callback
	});
}



function getSugar(user) {
	var tS = new Date().getTime();
	var sugar = null;
	$.ajax({
		dataType: "json",
		type : "POST",
		url : "session",
		data : {
			"action" : "sugar",
			"user" : user,
			timestamp : tS
		},
		async:false,
		success : function(result) {
			sugar = result.sugar;
		}
	});
	return sugar;
}


function talk(message) {
	var result = false;
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "input",
		data : {
			"action" : "talk",
			"message" : message,
			timestamp : tS
		},
		async: false,
		success : function() {
			result = true;
		}
	});
	return result;
}


function whisper(user, message) {
	var result = false;
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "input",
		data : {
			"action" : "whisper",
			"user" : user,
			"message" : message,
			timestamp : tS
		},
		async: false,
		success : function() {
			result = true;
		}
	});
	return result;
}


function doJoin(room) {
	var result = false;
	var tS = new Date().getTime();
	$.ajax({
		type : "GET",
		url : "input",
		data : {
			"action" : "join",
			"room" : room,
			timestamp : tS
		},
		async: false,
		success : function() {
			result = true;
		}
	});
	return result;
}

function away(state) {
	var result = false;
	var tS = new Date().getTime();
	$.ajax({
		type : "GET",
		url : "input",
		data : {
			"action" : "away",
			"state" : state,
			timestamp : tS
		},
		async: false,
		success : function() {
			result = true;
		}
	});
	return result;
}

function readOutbox(page, limit) {
	var tS = new Date().getTime();
	var messages = null;
	$.ajax({
		type : "GET",
		url : "message",
		data : {
			"action" : "outbox",
			"page" : page,
			"limit" : limit,
			timestamp : tS
		},
		async:false,
		success : function(result) {
			messages = result;
		}
	});
	return messages;
}


function readInbox(page, limit) {
	var tS = new Date().getTime();
	var messages = null;
	$.ajax({
		dataType: "json",
		type : "GET",
		url : "message",
		data : {
			"action" : "inbox",
			"page" : page,
			"limit" : limit,
			timestamp : tS
		},
		async:false,
		success : function(result) {
			messages = result;
		}
	});
	return messages;
}


function countUnread() {
	var tS = new Date().getTime();
	var count = 0;
	$.ajax({
		type : "GET",
		url : "message",
		data : {
			"action" : "inbox",
			"onlycount" : true,
			timestamp : tS
		},
		async:false,
		success : function(result) {
			count = result.unread;
		}
	});
	return count;
}

/**
 * Deletes a message
 */
function deleteMessage(id, callback) {
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "message",
		data : {
			"action" : "delete",
			"id" : id,
			timestamp : tS
		},
		success : function() {
			if (callback != null) {
			 callback();
			}
		}
	});
}

/**
 * Reads the messages body
 * @returns {String}
 */
function readMessage(id, callback) {
	var tS = new Date().getTime();
	$.ajax({
		type : "GET",
		url : "message",
		data : {
			"action" : "read",
			"id" : id,
			timestamp : tS
		},
		async : false,
		success : function(result) {
			callback(result);
		}
	});
}


function settings(font, color, room, asyncmode) {
	var result = false;
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "action",
		data : {
			"action" : "settings",
			"font" : font,
			"color" : color,
			"room" : room,
			"asyncmode" : asyncmode,
			timestamp : tS
		},
		async : false,
		success : function() {
			result = true;
		}
	});
	return result;
}

/**
 * @returns {boolean}
 */
function sendMessage(recipient, subject, body, callback) {
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "message",
		data : {
			"action" : "send",
			"recipient" : recipient,
			"subject" : subject,
			"body" : body,
			timestamp : tS
		},
		async : false,
		success : function(data) {
			var result = false;
			if (data != null && data.id != null) {
				result = true;
			} else {
				result = false;
			}
			callback(result);
		}
	});
}


/**
 * @returns {String}
 */
function invite(trial) {
	var result = null;
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "action",
		data : {
			"action" : "invite",
			"trial" : trial,
			timestamp : tS
		},
		async : false,
		success : function(data) {
			result = data.key;
		}
	});
	return result;
}

function doRefresh(callback) {
	var tS = new Date().getTime();
	var data = null;
	$.ajax({
		dataType: "json",
		type: "GET",
		url: "action",
		data : {
			"action" : "refresh",
			timestamp : tS
		},
		success: function(result) {
			callback(result);
		}
	});
	return data;
}


function readProfile(user, callback) {
	var tS = new Date().getTime();
	$.ajax({
		dataType: "json",
		type : "POST",
		url : "action",
		data : {
			"action" : "readprofile",
			"user" : user,
			timestamp : tS
		},
		async: false,
		success : function(data) {
			callback(data);
		}
	});
}



function doSaveProfile(field, value, callback) {
	var tS = new Date().getTime();
	$.ajax({
		type : "POST",
		url : "input",
		data : {
			"action" : 'profile',
			"field" : field,
			"value" : value,
			timestamp : tS
		},
		async: false,
		success : function() {
			if (callback != null) {
				callback();
			}
		}
	});
}

