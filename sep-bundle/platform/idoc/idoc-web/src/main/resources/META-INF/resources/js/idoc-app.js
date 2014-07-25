function Idoc(EMF) {
    var idoc = {};

    idoc.servicePath = EMF.servicePath + '/intelligent-document';
    idoc.object = {};
    idoc.objectViewVersion = '';
    idoc.operation = '';
    idoc.globalEvents = {};

    idoc.update = function(object) {
    	idoc.object = object;
    }

    idoc.joinServicePath = function(/* pass some args to join */) {
    	var path = idoc.servicePath;
    	for ( var i = 0; i < arguments.length; i++) {
    		path += '/' + arguments[i];
    	}
    	return path;
    }

    idoc.isNewObject = function() {
    	// REVIEW: typeof is not needed here
    	return typeof idoc.object.id === 'undefined' || !idoc.object.id;
    }

    idoc.bindGlobalEvent = function(name, handler) {
	$(idoc.globalEvents).bind(name, handler);
    }

    idoc.triggerGlobalEvent = function(name, data) {
	$(idoc.globalEvents).trigger(name, data);
    }

    // REVIEW: this is separated in EMF.util functions
    idoc.formatDate = function(stringValue, appendHint) {
	if (!appendHint) {
	    return $.datepicker.formatDate(SF.config.dateFormatPattern,
		    new Date(stringValue));
	}
	var suffix = idoc.timeSince(new Date(stringValue));
	return $.datepicker.formatDate(SF.config.dateFormatPattern, new Date(
		stringValue))
		+ ' (' + suffix + ')';
    }
    // REVIEW: this is separated in EMF.util functions
    idoc.getISODateString = function(date) {
	function pad(n) {
	    return n < 10 ? '0' + n : n;
	}
	return date.getUTCFullYear() + '-' + pad(date.getUTCMonth() + 1) + '-'
		+ pad(date.getUTCDate()) + 'T' + pad(date.getUTCHours()) + ':'
		+ pad(date.getUTCMinutes()) + ':' + pad(date.getUTCSeconds())
		+ 'Z';
    }
    // REVIEW: this is separated in EMF.util functions
    idoc.timeSince = function(date) {
	var seconds = Math.floor((new Date() - date) / 1000);
	var interval = Math.floor(seconds / 31536000);
	if (interval >= 1) {
	    if (interval == 1) {
		return interval + " year ago";
	    }
	    return interval + " years ago";
	}
	interval = Math.floor(seconds / 2592000);
	if (interval >= 1) {
	    if (interval == 1) {
		return interval + " month ago";
	    }
	    return interval + " months ago";
	}
	interval = Math.floor(seconds / 86400);
	if (interval >= 1) {
	    if (interval == 1) {
		return interval + " day ago";
	    }
	    return interval + " days ago";
	}
	interval = Math.floor(seconds / 3600);
	if (interval >= 1) {
	    if (interval == 1) {
		return interval + " hour ago";
	    }
	    return interval + " hours ago";
	}
	interval = Math.floor(seconds / 60);
	if (interval >= 1) {
	    if (interval == 1) {
		return interval + " minute ago";
	    }
	    return interval + " minutes ago";
	}
	return "few seconds ago";
    }
    return idoc;
}