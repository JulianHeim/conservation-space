"use strict";

window.app = angular.module('app', ['widgets', 'components', 'idocEmfCommentsIntegration', 'emfComments']);

app.config(['$httpProvider',function($httpProvider) {

	/**
	 * Add an http interceptor that fires events on http request, response and
	 * errors
	 */
	$httpProvider.interceptors.push(function ($q, $rootScope) {
		return {
			'request': function (config) {
				$rootScope.$broadcast('onRequestBegin', config);
				if (config.method == 'POST' || config.method == 'PUT' || config.method == 'DELETE') {
					EMF.ajaxGUIBlocker.showAjaxLoader('html');
				}
				return config;
			},
			'response': function (response) {
				EMF.ajaxGUIBlocker.hideAjaxLoader();

				// Will only be called for HTTP up to 300
				$rootScope.$broadcast('onRequestEnd', response);
				return response;
			},
			'responseError': function (rejection) {
				EMF.ajaxGUIBlocker.hideAjaxLoader();
				if (!rejection.data.messages) {
					$rootScope.$broadcast('onRequestError', rejection);
				}
				return $q.reject(rejection);
			}
		};
	});
}]);

app.run(["$rootScope", function($rootScope) {
	$rootScope.$on("onRequestError",function(event, response) {
		var message = response.data.message;
		
		if (!message) {
			message = "An error has occured. Please contact your system administrator!";
		}

		EMF.dialog.open({
			 "title": "Error",
			 "message": message,
			 notifyOnly: true
		});
	});

	$rootScope.setPreviewMode = function(mode) {
		$rootScope.previewMode = mode;
	}

	$rootScope.documentContext = {
		meta: { },
		documentState: { },
		templateConfig: { }
	};

	$rootScope.broadcastEvent = function(name, data) {
		if (angular.isDefined(data)) {
			$rootScope.$broadcast(name, data);
		} else {
			$rootScope.$broadcast(name);
		}
	}
}]);

app.controller('DocumentCtrl', ['$scope', '$window', 'DocumentRestClient', 'DocumentTemplateSerivce', 'ContentEditorService', 'IdocPanel',
                        function($scope,   $window,   DocumentRestClient,   DocumentTemplateSerivce,   ContentEditorService,   IdocPanel) {
	// /emf/javax.faces.resource/objectinstance-icon-16.png.jsf?ln=images
	$scope.iconUrl =  EMF.util.objectIconPath(idoc.object.type, 32);

	$scope.instanceType = idoc.object.type;
	$scope.rdfType = idoc.object.rdfType;
	$scope.instanceId = idoc.object.id;
	$scope.definitionId = idoc.object.definitionId;

	$scope.initDocument = function() {
		if (idoc.isNewObject()) {
			// the document is opened for creation
			$scope.documentContext.documentState.dirty = true;
			$scope.setPreviewMode(false);

			ContentEditorService.init('div.idoc-editor', $scope, $scope.previewMode);
		} else {
			var currentOperation = idoc.object.currentOperation;

			if (currentOperation) {
				if (currentOperation === 'editDetails' || currentOperation === 'editOnline') {
					$scope.setPreviewMode(false);
					$scope.loadDocument(idoc.object.id, idoc.object.type, null, function() {
						//$scope.broadcastEvent('edit-document');
					});
				} else if (currentOperation == 'clone') {
					$scope.cloneDocument(idoc.object.id, idoc.object.type);
				}
			} else {
				$scope.setPreviewMode(true);
				$scope.loadDocument(idoc.object.id, idoc.object.type, null);
			}
		}

		// load widgets palette
		IdocPanel.init($scope.previewMode);
		IdocPanel.addPaletteTab();
		IdocPanel.updatePaletteTab($scope.previewMode);
	};

	$scope.reloadDocument = function() {
		$scope.loadDocument(idoc.object.id, idoc.object.type);
		idoc.objectViewVersion = '';
		$scope.broadcastEvent('preview-document');
	};

	$scope.loadDocument = function(id, type, version, callback) {
		var document;
		DocumentRestClient.load(id, type, version).then(function(promise) {

			// For some reason the section instance from 'promise' is not
			// correct. To ensure that section is correctly added
			// we retrieve it from the 'idoc.object'(receive from document context).
			// Consider this as workaround till objects received from semantics
			// has no sections.
			promise.data.owningInstanceId = idoc.object.owningInstanceId;

			idoc.update(promise.data);

			$scope.documentContext.id = idoc.object.id;
			$scope.documentContext.meta = idoc.object.properties;
			ContentEditorService.init('div.idoc-editor', $scope, $scope.previewMode, function() {
				ContentEditorService.setContent(idoc.object.content);
				if (callback && typeof(callback) == "function") {
					callback();
				}
			});

			$scope.documentContext.documentState.dirty = false;

			if (version) {
				$scope.documentContext.documentState.historicVersion = true;
				$scope.broadcastEvent('document-selected-version', version);
				ContentEditorService.blur();
			} else {
				$scope.documentContext.documentState.historicVersion = false;
			}
			IdocPanel.refreshNavigation($scope.previewMode);
			IdocPanel.updatePaletteTab($scope.previewMode);
		});
		return document;
	}

	$scope.loadCommentsById = function(id) {
	    var result = [];
	    if (!idoc.isNewObject()) {
	    	result = DocumentRestClient.loadComment(id, '');
	    }
		return result;
	}

	$scope.saveComment = function(objectId, parentId, content) {
		return DocumentRestClient.saveComment(objectId, parentId, content);
	}

	$scope.isNewObject = function() {
		return idoc.isNewObject();
	}

	$scope.getCurrentOperation = function() {
		return idoc.object.currentOperation;
	}
	
	$scope.getDefinitionId = function() {
		return idoc.object.definitionId;
	}

	$scope.showTemplateSelector = function() {
		return idoc.isNewObject() && idoc.object.currentOperation != 'clone';
	}

	$scope.iDocClonePickerInit = function(id, type) {
		var owningInstanceId = idoc.object.owningInstanceId;
		var owningInstanceType = idoc.object.owningInstanceType;
		OBJ.objectsSectionPicker('clone', idoc.object.id, owningInstanceId, idoc.object.type);
	}

	$scope.saveAsTemplate = function(id, type) {
		var content = ContentEditorService.getContent();

		EMF.dialog.open({
			title: _emfLabels['idoc.btn.document_idoc_save_as_template'],
			elementSelector: '<div id="template-popup-content">' +
				'<div class="form-group"><label>' + _emfLabels['name'] + '</label><div id="template-name-in-use" class="hide"></div><input id="document-template-name" type="text" class="form-control input-sm" /></div>' +
				'<div class="form-group"><div><label>' + _emfLabels['primary'] + '</label></div><input id="document-template-primary" type="checkbox" /></div>' +
				'</div>',
			width: 400,
			height: 250,
			confirm: function(dialogControl) {
				var _scope = $scope;
				var templateNameInput =  $("#document-template-name");
				var templateName = templateNameInput.val();
				var primary = $("#document-template-primary").prop("checked");

				if (templateName == "") {
					templateNameInput.parent().addClass("has-error");
					templateNameInput.focus();

					return false;
				}

				DocumentTemplateSerivce.save($scope.definitionId, templateName, primary, ContentEditorService.getContent()).then(function(promise) {
					if(promise.data === 'true') {
						$("#template-popup-content").empty();
						$("#template-popup-content").append('<br/><br/><br/><span id="template-name-in-use" class="hide"></span>');
						$("#template-name-in-use").removeClass("hide");
						var msg = '  Template <b>"' + templateName + '"</b> was created successfully!';
						$("#template-name-in-use").html(msg); 
						var popup = $('body').find('.ui-dialog');
						var buttons = popup.find('.ui-dialog-buttonset').find('button');
						buttons.remove();
						setTimeout(function() {
							popup.remove();
						}, 3000);
					} else {
						$("#template-name-in-use").removeClass("hide");
						//$("#template-name-in-use").text(_emfLabels['idoc.template.name.taken']);
						$("#template-name-in-use").text("A template with this name already exists. Please change the name of the template.");
						templateNameInput.parent().addClass("has-error");
						templateNameInput.focus();
					}
				});

				return false;
			}
		});
	}

	$scope.cloneDocument = function(id, type) {
		DocumentRestClient.clone(id, type).then(function(promise) {
			var owningInstanceId = idoc.object.owningInstanceId;
			var owningInstanceType = idoc.object.owningInstanceType;

			idoc.update(promise.data);

			idoc.object.id = undefined;
			idoc.object.owningInstanceId = owningInstanceId;
			idoc.object.owningInstanceType = owningInstanceType;

			$scope.documentContext.meta = idoc.object.properties;

			$scope.documentContext.documentState.dirty = true;
			$scope.setPreviewMode(false);

			ContentEditorService.init('div.idoc-editor', $scope, $scope.previewMode, function() {
				ContentEditorService.setContent(idoc.object.content);

				IdocPanel.init($scope.previewMode);
				IdocPanel.updatePaletteTab($scope.previewMode);
			});
		});
	}

	// this event is called when a field in ObjectData widget is edited
	$scope.$on('idoc-properties-updated', function(event, data) {
		if (data.id === idoc.object.id && data.type === idoc.object.type) {
			// REVIEW: if deep merge is needed here and if not, why
			$.extend($scope.documentContext.meta, data.object.properties);
		}
	})

	$scope.$on('document-version-changed', function(event, version) {
		$scope.documentContext.meta.version  = version;
	})
	
	$scope.$on('document-revert-to-version', function(event, version) {
		DocumentRestClient.revertToVersion(idoc.object.id, idoc.object.type, version).then(function(promise) {
			$window.location.reload();
		});
	})

	$scope.$on('document-preview-version', function(event, version) {
			$scope.setPreviewMode(true);
			idoc.objectViewVersion = version;
			$scope.objectVersionMessage = _emfLabels["cmf.version.historical"] + " " + version + " " + _emfLabels["cmf.version.current"];
			$scope.loadDocument(idoc.object.id, idoc.object.type, version);
	})
}]);

app.controller('IdocActions', ['$scope', '$compile', '$window', 'DocumentRestClient', 'ContentEditorService', 'IdocPanel',
                       function($scope,   $compile,   $window,   DocumentRestClient,   ContentEditorService,   IdocPanel) {
	$scope.actions = [];
	
	$scope.isEditAllowed = function() {
		return !$scope.documentContext.meta.isLocked || $scope.documentContext.meta.isLockedByMe;
	}

	$scope.loadActions = function() {
		DocumentRestClient.loadActions(idoc.object.id, idoc.object.type).then(function(promise) {
			$scope.actions = promise.data;
		});
	}

	$scope.loadActions();

	$scope.saveDocument = function() {
		var content = ContentEditorService.getContent();
		idoc.object.content = content;
		
		// If title is empty show focus the field and stop document save action
		if($scope.documentContext.meta.title == "") {
			$("#idoc-title-input").parent().addClass("has-error");
			$("#idoc-title-input").focus();
			setTimeout(function() {
				$("#idoc-title-input").parent().removeClass("has-error");
			}, 3000);
			return; 
		}
		
		if (idoc.isNewObject()) {
			// REVIEW: why undefined comes as string here?
			if (typeof idoc.object.properties === 'undefined' || !idoc.object.properties) {
				idoc.object.properties = [];
			}

			idoc.object.properties = $.extend({}, $scope.documentContext.meta, idoc.object.properties);

			DocumentRestClient.create(idoc.object).then(function(promise) {
				
				if(!promise.data) {
					// if session is over redirect to the login page - CMF-7558
					$window.location.href = EMF.applicationPath;
				}
				
				idoc.update(promise.data);
				if(idoc.relations) {
					$.each(idoc.relations, function(index, value) {
						$.each(value.selectedItems, function(key, val) {
							val.targetId = promise.data.id
						});
						DocumentRestClient.addRelation(value);

					});
				}
				// After create reload the object, so JSF can initialize for new
				// object
				$scope.setPreviewMode(true);
				$window.location = EMF.bookmarks.buildLink(idoc.object.type, idoc.object.id);

			});
		} else {
			// EMF.documentId, content, $scope.documentContext.meta.title
			// put the title in the object or view properties
			DocumentRestClient.saveContent(idoc.object).then(
				// success
				function(response) {
					if(response.data) {
						idoc.object.overwrite = false;
						$scope.setPreviewMode(true);
						$scope.broadcastEvent('preview-document');
					} else {
						// if session is over redirect to the login page - CMF-7558
						window.onbeforeunload = null;
						$window.location.href = EMF.applicationPath;
					}
				},
				// error
				function(response) {
					var data = response.data,
						messages = data && data.messages;
					
					if (messages && messages.staleDataMessage) {
						EMF.dialog.confirm({
							okBtn: 'Overwrite',
							title: "Overwrite newer version?",
							message: messages.staleDataMessage,
							confirm: function() {
								idoc.object.overwrite = true;
								$scope.saveDocument(true);
							}
						});
					}
				}
			);
		}
	};

	$scope.editDocument = function() {
		$scope.broadcastEvent('edit-document');
	};

	$scope.cancelEditDocument = function() {
		if (idoc.isNewObject()) {
			var referrer 	= document.referrer,
				redirectURL = referrer,
				path 		= EMF.currentPath;

			if (path && path.length) {
				if (path.length == 1) {
					// if the instance is created at root context (without context) return user the user dashboard
					redirectURL = EMF.applicationPath;
				} else {
					// we step back 2 elements of the path because the last one is the not created
					// document/object and we cannot open non existent instance
					var redirectInstance = path[path.length - 2];
					redirectURL = EMF.bookmarks.buildLink(redirectInstance.type, redirectInstance.id);
				}
			}

			$window.location.href = redirectURL;
		} else {
			redirectURL = EMF.bookmarks.buildLink(idoc.object.type, idoc.object.id);
			$window.location.href = redirectURL;
		}
	};

	/**
	 * Create sub document from existing document when in edit mode
	 */
	$scope.createSubDocument = function() {
		DocumentRestClient.createSubDocument(idoc.object).success(function(result) {
			$scope.types = result;
			$scope.selectedValue = $scope.types[0].id;

			// create popup with all possible document types
			var _dialogView = '<div id="create-idoc-dialog"><label emf-label="cmf.document.create.type" />  <select id="chosenDropDown" chosen="idocTypes" ng-model="selectedValue" ng-options="item.id as item.description for item in types"  ng-change="updateDropdown()" data-placeholder="Select value"></select><div><label emf-label="cmf.document.create.description" /><br><textarea style="width: 560px; height: 320px;" ng-model="iDocDescription"/></div></div>';
			$("body").append($compile(_dialogView)($scope));

			// destroy new document popup on close or when sub-document is
			// created
			var _onclose = function() {
				$(this).dialog("destroy");
            	jQuery('#create-idoc-dialog').remove();
			};

			// fill dialog options
			var dialogOptions = {
					title: _emfLabels['cmf.document.create.popup.title'],
					modal: true,
					draggable: true,
					dialogClass: 'notifyme',
					width: '600',
					height: '500',
		        	buttons : [{
		            	text : _emfLabels['cmf.btn.create'],
		            	priority: 'primary',
		            	click : function() {
		            		parent.object = idoc.object;
		            		idoc.object = {};
		            		idoc.object.parentId = parent.object.id;
		            		idoc.object.definitionId = $scope.selectedValue;
		            		idoc.object.identifier = $scope.selectedValue;
		            		idoc.object.owningInstanceId = parent.object.owningInstanceId;
		            		idoc.object.owningInstanceType = parent.object.owningInstanceType;
		            		idoc.object.type = parent.object.type;
		            		
		            		idoc.object.properties = {};
		            		idoc.object.properties.description = $scope.iDocDescription;
		            		
		            		idoc.object.id = null;
		            		idoc.object.currentOperation = null;
		            		$scope.documentContext.id = null;

		            		_onclose();
		            		$scope.initDocument();
		            		$scope.broadcastEvent('edit-document');
		            	}
		        	},{
		        		text : window._emfLabels["cancel"],
		        		priority: 'secondary',
		                click : function() {
		                	_onclose();
		                }
		            }],
		            close : function() {
		            	_onclose();
		            }
		     };

			// REVIEW: EMF.dialog can be used here
			jQuery('#create-idoc-dialog').dialog(dialogOptions);
		});
	};

	$scope.printDocument = function() {
        $scope.broadcastEvent('print-document');
	}

	/**
	 * Prints the idoc/object document content using the printArea
	 * jQuery plugin.
	 */
	$scope.$on('print-document', function(event) {
		var instanceLink =	EMF.bookmarks.buildLink(idoc.object.type, idoc.object.id);
		IDocPrint.print.executePrint(instanceLink);
	})
	
	$scope.exportDocument = function() {
        $scope.broadcastEvent('export-document');
	}

	/**
	 * Exports a document as pdf
	 *
	 * Currently no styles included and images included as links are extracted
	 */
	$scope.$on('export-document', function(event) {
		var instanceLink =	EMF.bookmarks.buildLink(idoc.object.type, idoc.object.id)
		
        DocumentRestClient.exportIDoc(idoc.object.id + '.pdf', instanceLink).success(function(data) {
        	$("body").append("<iframe src='" + EMF.servicePath + '/export/' + data.fileName + "' style='display: none;' ></iframe>")
        });
	})

	/**
	 * Upload document to existing document or cultural object
	 */
	$scope.upload = function() {
		// create popup
		var _dialogView = '<div id="upload-to-idoc-dialog"><div class="upload-content-panel"></div></div>';
		$("body").append($compile(_dialogView)($scope));

		// destroy new document popup on close and remove blockUI
		var _onclose = function() {
			$(this).dialog("destroy");
        	jQuery('#upload-to-idoc-dialog').remove();
        	var blockUI = $('body').find('.sf-ajax-loader');
        	blockUI.removeClass('sf-ajax-loader', 'modal-loader');
		};

		// fill dialog options
		var dialogOptions = {
				title: _emfLabels['cmf.document.create.popup.title'],
				modal: true,
				draggable: true,
				width: 920,
				height: 700,
				resizable: true,
				dialogClass: "widget-config-dialog notifyme",
	        	buttons : [{
	        		text : window._emfLabels["cancel"],
	        		priority: 'secondary',
	                click : function() {
	                	_onclose();
	                }
	            }],
	            close : function() {
	            	_onclose();
	            }
	     };
		// REVIEW: EMF.dialog can be used
		jQuery('#upload-to-idoc-dialog').dialog(dialogOptions);

		var element = $('.upload-content-panel');
		var upload = element.upload({
    		allowSelection	: false,
    		showBrowser		: false,
    		isNewObject		: idoc.isNewObject()
    	});
		element.data('upload', element);
	};

	/**
	 * Delete document
	 */
	$scope.deleteDocument = function() {
		EMF.dialog.confirm({
			title: '',
			message: _emfLabels['cmf.msg.warning.confirmOperation'] + ' \'Delete document\'?',
			confirm: function() {
				DocumentRestClient.deleteDocument(idoc.object.id, idoc.object.type).then(function(promise) {
//					CMF-4757 referrer is no good here since it could be the same page as the object we are deleting (page refresh) or document context
//					may not be cleared on the referrer and it will show the deleted instance
//					$window.location.href = document.referrer;
					var path = EMF.currentPath;
					var redirectTo = path[path.length - 2];
					$window.location = EMF.bookmarks.buildLink(redirectTo.type, redirectTo.id);
				});
			}
		});
	};

	$scope.revertToVersion = function() {
		$scope.broadcastEvent('document-revert-to-version', EMF.documentVersion);
	}

	$scope.showAction = function(action) {
		$scope.versionMode = idoc.object.loadedViewVersion;
		var show = false;
		var actionId = action.actionId;
			switch (actionId) {
				case "document_idoc_save":
					if (!$scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_edit":
					if ($scope.previewMode) {
						show = true;
					}
				    break;
				case "document_idoc_cancel_changes":
					if (!$scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_create":
					if (!idoc.isNewObject() && 	idoc.object.type == "documentinstance" && $scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_upload":
					show = true;
					break;
				case "document_idoc_clone":
					if ($scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_delete":
					if (!idoc.isNewObject() && $scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_download":
					if (!idoc.isNewObject() && $scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_save_as_template":
					if (!idoc.isNewObject() && $scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_print":
					if (!idoc.isNewObject() && $scope.previewMode) {
						show = true;
					}
					break;
				case "document_idoc_export":
					if (!idoc.isNewObject() && $scope.previewMode) {
						show = true;
					}
					break;
				default:
					break;
			}
		return show;
	}

	$scope.$on('preview-document', function(event) {
		$scope.setPreviewMode(true);
		ContentEditorService.init('div.idoc-editor', $scope, $scope.previewMode);
		IdocPanel.updatePaletteTab($scope.previewMode);
		IdocPanel.refreshNavigation($scope.previewMode);
		$scope.loadActions();
	})

	$scope.$on('edit-document', function(event) {
		DocumentRestClient.edit(idoc.object.id, idoc.object.type).then(function(response) {
			if(response.data) {
				idoc.update(response.data);
				$scope.documentContext.meta = idoc.object.properties;
				if (idoc.object.properties.isLocked && !idoc.object.properties.isLockedByMe) {
					return;
				} else {
					$scope.setPreviewMode(false);
					ContentEditorService.init('div.idoc-editor', $scope, $scope.previewMode, function() {
						ContentEditorService.setContent(idoc.object.content);
					});
					$scope.documentContext.meta.lockedByMessage = null;
					$scope.documentContext.meta.isLockedByMe = true;
					// FIXME da se refreshne celiq left column (accordion) a ne edin
					// po edin elementite mu
					IdocPanel.updatePaletteTab($scope.previewMode);
					IdocPanel.refreshNavigation($scope.previewMode);
				}
			}
			$scope.loadActions();
		});
	})

	/**
	 * Show warning popup if the document is in preview mode.
	 */
	EMF.onbeforeload.handlers.idocHandler = function() {
		return (!$scope.previewMode || idoc.isNewObject())
	};

	window.onunload = function () {
		var accordion = $( "#accordion" ).addClass("fade");
		
		if ($scope.documentContext.meta.isLockedByMe) {
			//DocumentRestClient.unlock(idoc.object.id, idoc.object.type).then(function(){
				//$scope.documentContext.meta.isLockedByMe = false;
			//});
// 			TODO: how sync $http in ng
			$.ajax({
				type: 'POST',
				url: EMF.servicePath + '/intelligent-document' + '/' + idoc.object.id + '/unlock?type=' + idoc.object.type,
				async: false
			});
		}
	}
}]);

app.controller("DocumentTemplateController", ["$scope", "DocumentTemplateSerivce","ContentEditorService","IdocPanel", function($scope, DocumentTemplateSerivce, ContentEditorService, IdocPanel) {

	/** Loads a template content and applies it to the document */
	$scope.selectTemplate = function(template) {
		$scope.selectedTemplate = template;

		DocumentTemplateSerivce.getContent(template.id).then(function(result) {
			$scope.documentContext.meta.title = result.data.title;
			ContentEditorService.setContent(result.data.content);
			IdocPanel.init($scope.previewMode);
		});
	}

	if ($scope.isNewObject() && $scope.getCurrentOperation() != 'clone') {
		DocumentTemplateSerivce.getTemplates($scope.getDefinitionId()).then(function(result) {
			$scope.templates = result.data;
			if ($scope.templates.length > 0) {
				$scope.selectTemplate($scope.templates[0]);
			}
		});
	}
}]);

app.controller('InlineEditCtrl', ['$scope', function($scope) {
	$scope.inEditMode = false;
	$scope.toggle = function() {
		// TODO: pass this variable in somehow
		if ($scope.previewMode) {
			if ($scope.inEditMode) {
				$scope.inEditMode = false;
			}
		} else {
			$scope.inEditMode = !$scope.inEditMode;
		}

		var element = angular.element('#' + $scope.id);
		if ($scope.inputValue && $scope.inputValue !== $scope.defaultText) {
			element.removeClass('idoc-paceholder-text');
		} else {
			$scope.inputValue = $scope.defaultText;
			element.addClass('idoc-paceholder-text');
			if ($scope.inEditMode) {
				$scope.inputValue = "";
			}
		}
	};
}]);
