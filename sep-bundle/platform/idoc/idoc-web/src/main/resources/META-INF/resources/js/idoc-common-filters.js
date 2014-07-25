(function() {
	'use strict'

	var module = angular.module('idoc.common.filters', [ ]);

	/**
	 * Filter that enables binding an arbitrary string as html using ng-bind-html
	 * e.g. <div ng-bind-html="model.unsafeHtml | trustAsHtmlFilter"></div>
	 * Use this filter only when you're know that the html is safe to bind directly
	 * w/o additional sanitization.
	 */
	module.filter('trustAsHtmlFilter', ['$sce', function($sce) {
		return function(unsafeHtml) {
			return $sce.trustAsHtml(unsafeHtml);
		}
	}]);

}())