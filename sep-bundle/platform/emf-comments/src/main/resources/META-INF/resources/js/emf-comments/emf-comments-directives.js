(function() {
	'use strict';
	
	var module = angular.module('emfCommentsDirectives', []);
	
	module.directive('datePicker', [function() {
		return {
			restrict: 'E',
			replace: true,
			template: '<input type="text" class=" form-control input-sm" data-ng-model="model"></input>',
			scope: {
				model: '=',
				options: '='
			},
			link: function(scope, element, attrs) {
				element.datepicker(scope.options);
			}
		}
	}]);
	
	module.directive('selectOneAjax', [function() {
		return {
			restrict: 'E',
			replace: true,
			template: '<input type="hidden" />',
			scope: {
				model: '=',
				url: '='
			},
			link: function(scope, element, attrs) {
				var conf = {
					minimumInputLength: 2,
					// this is not the id of the element, it's the id of the select item
					id: function(user) {
					   	return user.value; 
					},
					initSelection: $.noop,
					ajax: {
						url: scope.url,
						dataType: 'json',
						data: function (term, page) { return { term: term }; },
						results: function (data, page) {
							if (data && data.length) {
								return { results: data };
							}
						    return { results: [ ] };
						}
					},
					formatResult: function(user) {
						return user.label;
					},
					formatSelection: function(user) {
						return user.label;
					}
				};
				
				element
					.select2(conf)
					.on('change', function() {
						scope.$apply(function() {
							scope.model = element.val();
						});
					});
				
				scope.$watch('model', function(value) {
					element.select2('val', value || '');
				});
			}
		}
	}]);
	
	module.directive('selectOne', [function() {
		return {
			restrict: 'E',
			replace: true,
			template: '<select data-ng-model="model" data-ng-options="item.value as item.label for item in options"></select>',
			scope: {
				options: '=',
				model: '='
			},
			link: function(scope, element, attrs) {
				var conf = {
					minimumResultsForSearch: -1, 
					allowClear: true 
				};
				
				element
					.select2(conf)
					.on('change', function() {
						scope.$apply(function() {
							
						});
					});
				
				scope.$watch('model', function(value) {
					element.select2('val', value || '');
				});
			}
		}
	}]);
	
	module.directive('tagSelect', [function() {
		return {
			restrict: 'E',
			replace: true,
			template: '<input type="hidden" data-ng-model="model" />',
			scope: {
				tags: '&?',
				model: '='
			},
			link: function(scope, element, attrs) {
				var conf = {
					separator: '|',
					tags: function() {
						if (scope.tags) {
							var tags = scope.tags();
							return tags;
						} else {
							return [ ];
						}
					}
				};
				
				element
					.select2(conf)
					.on('change', function() {
						scope.$apply(function() {
							scope.model = element.val();
						});
					});
				
				scope.$watch('model', function(value) {
					if (value) {
						element.select2('val', value.split('|'));
					} else {
						element.select2('val', []);
					}
				});
			}
		}
	}]);
	
	module.directive('emfCommentsFilter', ['$compile', '$timeout', function($compile, $timeout) {
		return {
			restrict: 'EA',
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/emf-comments-filter.tpl.html',
			link: function(scope, element, attrs) {
				
				
			},
			controller: 'CommentsFilterController'
		}
	}]);
	
	module.directive('emfCommentsDashlet', [function() {
		return {
			restrict: 'EA',
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/emf-comments-dashlet.tpl.html',
			scope: {
				loadFilter: '=',
				orderByExpression: '@'
			},
			controller: 'CommentsDashletController',
			link: function(scope, element, attrs) {
				
			}
		}
	}]);
	
	module.directive('emfComments', ['$timeout', '$document', function($timeout, $document) {
		return {
			restrict: 'A',
			scope: {
				objects: '=?',
				clientSideFilter: '=?',
				orderByExpression: '@'
			},
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/emf-comments.tpl.html',
			controller: 'CommentsController',
			link: function(scope, element, attrs) {
				var top = element[0].offsetTop,
					cancelCreate = function() {
						$timeout(function() {
							scope.$broadcast('emf-comment:ng:cancel-create-topic');
						}, 0);
					};
				
				element.on('keydown', function(event) {
					event.stopPropagation();
				});
				
				$document
					.on('emf-comments:dom:create-topic', function(event, data) {
						$timeout(function() {
							scope.$broadcast('emf-comments:ng:create-topic', data);
						}, 0);
					});
				
				
				
				$document
					.on('keyup', function(event) {
						if (event.keyCode !== 27) {
							return;
						}
						cancelCreate();
					});
				
				$document
					.on('emf-comment:dom:cancel-create-topic', function(event) {
						cancelCreate();
					});
			}
		}
	}]);
	
	module.directive('ensureAlignment', ['$timeout', function($timeout) {
		return function(scope, element, attrs) {
			
			function ensureAlignment() {
				if (!scope.object.next) {
					return;
				}
				
				if (scope.expanded) {
					$timeout(function() {
						var objectWrapperHeight = element.height(),
							availableHeight = scope.object.nextTop - scope.object.top,
							nextMarginTop = 0,
							next = $('#' + scope.object.next.replace(':', '\\:')),
							nextOriginalMarginTop = next.data('emfCommentsData').topMargin;
						
						if (availableHeight < objectWrapperHeight) {
							scope.additionalMarginToNext = objectWrapperHeight - availableHeight;
							nextMarginTop = parseInt(next.css('marginTop'));
							next.css({
								marginTop: nextOriginalMarginTop + scope.additionalMarginToNext + 'px'
							});
						} else {
							next.css({
								marginTop: nextOriginalMarginTop + 'px'
							});
						}
					});
				} else {
					var next = $('#' + scope.object.next.replace(':', '\\:'));
					var nextMarginTop = parseInt(next.css('marginTop'));
					next.css({
						marginTop: next.data('emfCommentsData').topMargin + 'px'
					});
					scope.additionalMarginToNext = 0;
				}
			}
			
			element.on('click', function(event) {
				if (!scope.object.next) {
					return;
				}
				
				var expanded = false;
				if (!event.ctrlKey) {
					expanded = true;
				}
				
				scope.$apply(function() {
					scope.expanded = expanded;
				});
			});
			
			scope.$watch(
				function() {
					return element.height();
				},
				ensureAlignment
			);
		}
	}]);
	
	module.directive('inlineComment', [function() {
		return {
			restrict: 'E',
			replace: true,
			transclude: true,
			scope: {
				model: '=',
				collapsed: '=',
				forTopic: '=?'
			},
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/inline-comment.tpl.html',
			controller: 'InlineCommentController'
		}
	}]);
	
	module.directive('topic', [function() {
		return {
			restrict: 'E',
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/topic.tpl.html',
			controller: 'TopicController'
		}
	}]);
	
	module.directive('reply', [function() {
		return {
			restrict: 'E',
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/reply.tpl.html',
			controller: 'ReplyController'
		}
	}]);
	
	module.directive('inlineCommentEditor', ['$timeout', function($timeout) {
		return {
			replace: true,
			restrict: 'E',
			scope: {
				model: '=',
				onSave: '&',
				toggleEditorMode: '&'
			},
			controller: 'InlineCommentEditorController',
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/inline-comment-editor.tpl.html',
			link: function(scope, element, attrs) {
				var tinymceConfig = $.extend(true, { }, module.defaultCommentsEditorSettings);
				
				scope.editorId = EMF.util.generateUUID();
				tinymceConfig.selector = '#' + scope.editorId;
				tinymceConfig._initialContent = scope.model.content || '';
				tinymceConfig.toolbar = false;
				
				element.on('keyup', function(event) {
					// cancel editing on ECS
					if (event.keyCode === 27) {
						$timeout(function() {
							scope.$emit('emf-comments:ng:cancel-editing');
						}, 0);
					}
					
				});
				
				element.on('emf-comments:dom:tinymce-save', function() {
					scope.$apply(function() {
						var editor = tinymce.get(scope.editorId),
							content = editor.getContent();
						
						editor.setContent('');
						scope.model.content = content;
						scope.$eval(scope.onSave);
					});
				});
				
				scope.$on('emf-comments:ng:inline-editor-update-model', function() {
					var editor = tinymce.get(scope.editorId);
					scope.model.content = editor.getContent();
					editor.setContent('');
				});
				
				$timeout(function() {
					tinymce.init(tinymceConfig);
				}, 0);
			}
		}
	}]);
	
	module.directive('popupCommentEditor', ['$timeout', 'TagsService', function($timeout, TagsService) {
		return {
			restrict: 'E',
			templateUrl: EMF.applicationPath + '/js/emf-comments/templates/popup-comment-editor.tpl.html',
			scope: {
				model: '=',
				onSave: '&',
				onCancel: '&?',
				forTopic: '=?'
			},
			link: function(scope, element, attrs) {
				element.addClass('popup-comment-editor-popup');
				
				// shortcuts in IAT stop some keys
				element.on('keydown', function(event) {
					event.stopPropagation();
				});
				
				scope.categories = [ {
				    value : '0',
				    text : 'Note to self'
				}, {
				    value : '1',
				    text : 'Editorial question/suggestion'
				}, {
				    value : '2',
				    text : 'Request assistance'
				} ];
				
				var tinymceConfig = $.extend(true, { }, module.defaultCommentsEditorSettings);
				
				scope.editorId = EMF.util.generateUUID();
				tinymceConfig.selector = '#' + scope.editorId;
				tinymceConfig._initialContent = scope.model.content || '';
				tinymceConfig.toolbar = tinymceConfig._toolbar;
				tinymceConfig._saveOnEnter = false;
				
				var tagsElement = angular.element('.popup-comment-editor input.tags');
				tagsElement
					.val(scope.model.tags || '')
					.select2({
						separator: '|',
						width: '100%',
				        multiple: true,
				        placeholder: 'Tags',
						tags: function() {
							return TagsService.getAll();
						}
					})
					.on('change', function(event) {
						var val = event.val,
							added = event.added,
							removed = event.removed;
						
						TagsService.add(added);
						TagsService.remove(removed);
						
						scope.model.tags = tagsElement.val();
					});
				
				var categoryElement = angular.element('.popup-comment-editor select.category');
				categoryElement
					.select2({ width: '100%', minimumResultsForSearch: -1, placeholder: 'Category' })
					.on('change', function() {
						scope.model.type = $(this).val();
					});
				
				$timeout(function() {
					EMF.dialog.init({
						modal: false,
						content: element,
						width: 550,
						okBtn: 'Post',
						confirm: function() {
							scope.$apply(function() {
								var editor = tinymce.get(scope.editorId),
								content = editor.getContent();
								
								scope.model.content = content;
								scope.$eval(scope.onSave);
							});
						},
						cancel: function() {
							$(document).trigger('emf-comment:dom:cancel-create-topic');
							if (scope.onCancel) {
								scope.onCancel();
							}
						}
					});

					tinymce.init(tinymceConfig);
					
					angular.element('.popup-comment-editor select.category').trigger('change');
				}, 0);
			}
		}
	}]);
	
	module.defaultCommentsEditorSettings = {
		inline: true,
		menubar: false,
		toolbar: false,
		resize: false,
		fixed_toolbar_container: ".toolbar-placeholder",
		forced_root_block: 'div',
		schema: 'html5',
		plugins: [
			'lists link image anchor internal_thumbnail_link'
		],
		_toolbar: 'bold italic | bullist numlist | link image | internal_link internal_thumbnail_link',
		_saveOnEnter: true,
		setup: function(editor) {
				
			var enterKeyCode = tinymce.util.VK.ENTER,
				saveOnEnter  = editor.settings._saveOnEnter;
				
			editor.on('Init', function(event) {
				editor.focus(false);
				if (editor.settings._initialContent) {
					editor.setContent(editor.settings._initialContent);
				}
			});

			editor.on('KeyDown', function(event) {
				var content;

				if (saveOnEnter && enterKeyCode === event.keyCode && !event.ctrlKey) {
					event.preventDefault();
					event.stopPropagation();
					angular.element(editor.dom.getRoot()).trigger('emf-comments:dom:tinymce-save');
				}
			});
		}
	}
}());