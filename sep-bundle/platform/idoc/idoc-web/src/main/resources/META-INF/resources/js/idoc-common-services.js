(function() {
	'use strict'

	var module = angular.module('idoc.common.services', [ ]);

	module.factory('ObjectPropertiesService', ['$http', function($http) {
		return {
			loadLiterals: function(type, id, definitionId) {
				var conf = { 
					params: { 
						forType: type,
						id: id,
						definitionId: definitionId
					} 
				};
				return $http.get(EMF.servicePath + '/definition/literals', conf);
			},
			
			loadSemanticProperties: function(types) {
				var conf = { params: { } };
				if (types) {
					conf.params.forType = types.join(',');
				}
				
				return $http.get(EMF.servicePath + '/definition/properties', conf);
			},
			
			loadDefinitionProperties: function(type, id) {
				var promise = $http.get(
					idoc.joinServicePath('instance', id, 'properties'), 
					{ params: { type: type } }
				);
				return promise;
			},
			
			/**
			 * Load the properties for a set of objects. 
			 */
			bulkLoadDefinitionProperties: function(objects) {
				var requestConfig = {
					params: {
						objects: JSON.stringify(objects)
					}
				}
				return $http.get(idoc.joinServicePath('properties'), requestConfig);
			}
		}
	}]);
	
	module.factory('CodelistService', ['$http', function($http) {
		return {
			loadCodelist: function(codelistNumber) {
				return $http.get(EMF.servicePath + '/codelist/' + codelistNumber);
			},
			
			loadAllAvailableCodelists: function() {
				return $http.get(EMF.servicePath + '/codelist/codelists');
			}
		}
	}]);
	
}())