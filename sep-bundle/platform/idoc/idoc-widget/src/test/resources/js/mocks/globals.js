EMF = {
	applicationPath: '/emf',
	ajaxGUIBlocker: {
		hideAjaxLoader: function() {}
	},
	util: {
		generateUUID: function() {
			return new Date().getTime();
		}
	}
}

_emfLabels = {
	'widget.checkbox.title': 'Checkbox'
}