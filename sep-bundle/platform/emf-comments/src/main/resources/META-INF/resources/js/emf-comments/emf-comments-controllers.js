(function() {
	'use strict';
	
	var module = angular.module('emfCommentsControllers', []);
	
	module.controller('CommentsController', ['$scope', function($scope) {
		
		$scope.$on('emf-comments:ng:update-object', function(event, data) {
			var index = -1,
				nextIndex;
			
			if (!$scope.objects) {
				$scope.objects = [ ];
			}
			
			if (data.dirty) {
				index = _.findIndex($scope.objects, function(item) {
					return item.aboutSection === data.aboutSection;
				});
				
				// element is not added yet
				if (index === -1) {
					nextIndex = _.findIndex($scope.objects, function(item) {
						return item.aboutSection === data.next;
					});
					
					if (nextIndex === -1) {
						$scope.objects.push(data);
					} else {
						$scope.objects.splice(nextIndex, 0, data);
					}
				} else {
					$scope.objects[index] = data;
				}
			}
			
			if (data.removed) {
				index = _.findIndex($scope.objects, function(item) {
					return item.aboutSection === data.aboutSection;
				});
				
				if (index > -1) {
					$scope.objects.splice(index, 1);
				}
			}
		});
	}]);
	
	module.controller('CommentsFilterController', ['$scope', '$timeout', 'TagsService', function($scope, $timeout, TagsService) {
		$scope.filterCriteria = { sortBy: 'createdOn' };
		$scope.url = EMF.servicePath + '/users/search';
		$scope.datePickerOptions = {
			changeMonth		: true,
			changeYear 		: true,
		   	dateFormat		: SF.config.dateFormatPattern,
		   	firstDay 		: SF.config.firstDay,
		   	monthNames 		: SF.config.monthNames,
		   	monthNamesShort	: SF.config.monthNamesShort,
			dayNames		: SF.config.dayNames,
			dayNamesMin 	: SF.config.dayNamesMin,
			numberOfMonths	: 1
    	}
		
		$scope.categories = [
		    { value: '0', label: 'Note to Self' },
		    { value: '1', label: 'Editorial question/suggestion' },
		    { value: '2', label: 'Request assistance' }
		];

		$scope.statuses = [
		    { value: 'IN_PROGRESS', label: 'In Progress' },
		    { value: 'ON_HOLD', label: 'On Hold' }
		];
		
		$scope.showFilterPopout = false;
		$scope.showSortPopout = false;
		
		$scope.getAvailableTags = function() {
			return TagsService.getAll();
		}
		
		$scope.toggleFilter = function() {
			$scope.showFilterPopout = !$scope.showFilterPopout;
			$scope.showSortPopout = false;
		}

		$scope.toggleSort = function() {
			$scope.showSortPopout = !$scope.showSortPopout;
			$scope.showFilterPopout = false;
		}
		
		$scope.applyFilter = function() {
			var criteria = angular.copy($scope.filterCriteria),
				from,
				to;
			
			if (criteria.dateFrom) {
				from = $.datepicker.parseDate($scope.datePickerOptions.dateFormat, criteria.dateFrom);
				from.setHours(3, 0, 0, 0);
				criteria.dateFrom = from.toISOString();
			}

			if (criteria.dateTo) {
				to = $.datepicker.parseDate($scope.datePickerOptions.dateFormat, criteria.dateTo);
				to.setDate(to.getDate() + 1);
				to.setHours(2, 59, 59, 0);
				criteria.dateTo = to.toISOString();
			}
			$scope.$broadcast('emf-comments:ng:apply-filter', criteria);
			$scope.close();
		}
		
		$scope.resetFields = function() {
			$scope.filterCriteria = { sortBy: 'createdOn' };
		}
		
		$scope.close = function() {
			$scope.showSortPopout = false;
			$scope.showFilterPopout = false;
		}
	}]);
	
	module.controller('CommentsDashletController', ['$scope', 'CommentsService', function($scope, CommentsService) {
		$scope.topics = [ ];
		$scope.hideLoadMoreBtn = true;
		
		// FIXME: same thing in TopicController
		$scope.$on('emf-comments:ng:replace-topic', function(event, topic) {
			var index = _.findIndex($scope.topics, function(item) {
				return item.Id === topic.Id;
			});
			if (index > -1) {
				$scope.topics[index] = topic;
			}
		});
		
		// FIXME: same thing in TopicController
		$scope.$on('emf-comments:ng:remove-topic', function(event, topic) {
			var index = _.findIndex($scope.topics, function(item) {
				return item.Id === topic.Id;
			});
			if (index > -1) {
				$scope.topics.splice(index, 1);
			}
		});
		
		$scope.$on('emf-comment:ng:reload-topics', function() {
			$scope.topics = [];
			$scope.loadTopics(false, null);
		});
		
		$scope.getInstanceLink = function(topic) {
			if (!topic.about) {
				return '';
			}
		    var tab = null;
		    var instanceType = topic.about.instanceType;
		    
		    if (instanceType === 'projectinstance') {
		    	tab = EMF.bookmarks.projectTab.details;
		    } else if (instanceType === 'caseinstance') {
		    	tab = EMF.bookmarks.caseTab.details;
		    }
			return EMF.bookmarks.buildLink(instanceType, topic.about.instanceId, tab);
		}
		
		$scope.getInstanceIcon = function(topic) {
			if (!topic.about) {
				return '';
			}
			return EMF.util.objectIconPath(topic.about.instanceType, 32);
		}
		
		$scope.$on('emf-comments:ng:apply-filter', function(event, filter) {
			$scope.loadFilter = _.merge({ }, $scope.oldLoadFilter, filter);
			$scope.loadTopics(false);
		});
		
		$scope.loadTopics = function(append, extraParams) {
			var params = null;
			if (extraParams) {
				params = angular.extend({ }, extraParams, $scope.loadFilter);
			} else {
				params = $scope.loadFilter;
			}
			
			$scope.loadingMore = true;
			
			CommentsService.loadTopics(params)
				.then(
					function success(response) {
						$scope.loadingMore = false;
						var result = response.data.result || [ ];

						if (append) {
							_.each(result, function(topic) {
								$scope.topics.push(topic);
							});
							
							// FIXME: get from config when available
							if (response.data.result.length < 25) {
								$scope.hideLoadMoreBtn = true;
							} else {
								$scope.hideLoadMoreBtn = false;
							}
						} else {
							// if not append we are loading for the first time
							$scope.topics = result;
							
							// FIXME: get from config when available
							if ($scope.topics.length === 25) {
								$scope.hideLoadMoreBtn = false;
							} else {
								$scope.hideLoadMoreBtn = true;
							}
						}
					},
					function error(response) {
						console.error(response);
					}
				);
		}
		
		$scope.loadMore = function() {
			var date = $scope.topics[$scope.topics.length - 1].createdOn;
			$scope.loadTopics(true, {
				date: date
			});
		}
		
		$scope.loadTopics();
		$scope.oldLoadFilter = angular.copy($scope.loadFilter);
	}]);
	
	module.controller('ObjectController', ['$scope', '$timeout', 'CommentsService', 'CommentsConfig', function($scope, $timeout, CommentsService, CommentsConfig) {
		
		$scope.showCreateTopicBtn = CommentsConfig.showCreateTopicBtn;
		$scope.inline = true;
		$scope.newTopic = null;
		$scope.topics = [ ];
		$scope.hideLoadMoreBtn = true;
		
		$scope.$on('emf-comments:ng:mouse-move', function(event, y) {
			console.log(y)
		});
		
		$scope.getMinHeight = function() {
			var top = $scope.object.top,
			nextTop = $scope.object.nextTop;
			
			if ((top || top === 0) && nextTop) {
				return nextTop - top;
			}
			
			return null;
		}

		$scope.getMaxHeight = function() {
			var top = $scope.object.top,
				nextTop = $scope.object.nextTop;
			
			if (!$scope.expanded && (top || top === 0) && nextTop) {
				return nextTop - top;
			}
			
			return null;
		}
		
		$scope.togglePopupEditor = function() {
			$scope.inline = !$scope.inline;
		}
		
		$scope.createTopic = function(data) {
			var defaultData = { 
				aboutSection: $scope.object.aboutSection,
				about: {
					instanceId: $scope.object.instanceId,
					instanceType: $scope.object.instanceType
				}
			};
			
			if (data) {
				$scope.newTopic = _.merge({ }, defaultData, data);
			} else {
				$scope.newTopic = defaultData;
			}
		}
		
		$scope.saveNewTopic = function() {
			$scope.$emit('emf-comments:ng:before-save-topic', $scope.newTopic);
			CommentsService.saveTopic($scope.newTopic)
				.then(
					function success(response) {
						if (!$scope.topics) {
							$scope.topics = [ ];
						}
						$scope.topics.push(response.data);
						$scope.newTopic = null;
						$scope.$emit('emf-comments:ng:topic-created', response.data);
					},
					
					function error(response) {
						console.error(response);
					}
				);
		}
		
		$scope.$watchCollection('filteredTopics', function() {
			$scope.$emit('emf-comments:ng:topics-changed', {
				topics: $scope.filteredTopics,
				object: $scope.object 
			});
		});
		
		$scope.$on('emf-comments:ng:apply-filter', function(event, filter) {
			$scope.loadTopics(false, filter);
		});
		
		$scope.loadTopics = function(append, extraParams) {
			var params = { sectionId: $scope.object.aboutSection };
			if (extraParams) {
				params = angular.extend({ }, extraParams, params);
			}
			
			$scope.loadingMore = true;
			
			CommentsService.loadTopics(params)
				.then(
					function success(response) {
						$scope.loadingMore = false;
						$scope.hideLoadMoreBtn = true;
						var result = response.data.result || response.data[params.sectionId] || [ ];
						
						if (append) {
							_.each(result, function(topic) {
								$scope.topics.push(topic);
							});
							
							// FIXME: get from config when available
							if (result.length >= 25) {
								$scope.hideLoadMoreBtn = false;
							}
						} else {
							// if not append we are loading for the first time
							$scope.topics = result;
							
							// FIXME: get from config when available
							if ($scope.topics.length === 25) {
								$scope.hideLoadMoreBtn = false;
							}
						}
						
						$scope.$emit('emf-comments:ng:topics-loaded', {
							topics: $scope.topics,
							object: $scope.object 
						});
					},
					function error(response) {
						console.error(response);
					}
				);
		}
		
		$scope.loadMore = function() {
			var date = $scope.topics[$scope.topics.length - 1].createdOn;
			$scope.loadTopics(true, {
				date: date
			});
		}
		
		$scope.$on('emf-comments:ng:replace-topic', function(event, topic) {
			var index = _.findIndex($scope.topics, function(item) {
				return item.Id === topic.Id;
			});
			if (index > -1) {
				$scope.topics[index] = topic;
			}
		});
		
		$scope.$on('emf-comments:ng:remove-topic', function(event, topic) {
			var index = _.findIndex($scope.topics, function(item) {
				return item.Id === topic.Id;
			});
			if (index > -1) {
				$scope.topics.splice(index, 1);
			}
		});

		$scope.$on('emf-comments:ng:create-topic', function(event, data) {
			if (data.popupMode) {
				$scope.inline = false;
			}
			$scope.createTopic(data.content);
		});

		$scope.$on('emf-comment:ng:cancel-create-topic', function(event) {
			$scope.newTopic = null;
		});
		
		$scope.loadTopics();
	}]);
	
	module.controller('TopicController', ['$scope', 'CommentsService', 'TagsService', 'CommentsConfig', function($scope, CommentsService, TagsService, CommentsConfig) {
		
		$scope.inline = true;
		$scope.inlineReply = true;
		$scope.newReply = {
			replyTo: $scope.topic.Id
		};
		$scope.collapsed = true;
		$scope.repliesLoaded = false;
		$scope.editing = false;
		
		$scope.cancelEditTopic = function() {
			$scope.editing = false;
		}
		
		$scope.cancelNewReply = function() {
			$scope.createReply();
			$scope.inlineReply = true;
		}
		
		$scope.togglePopupEditor = function() {
			$scope.inline = false;
		}

		$scope.togglePopupReplyEditor = function() {
			$scope.inlineReply = !$scope.inlineReply;
		}
		
		$scope.toggleReplies = function() {
			$scope.collapsed = !$scope.collapsed;
			
			if (!$scope.repliesLoaded && !$scope.collapsed) {
				CommentsService.loadTopicReplies($scope.topic)
					.then(
						function success(response) {
							$scope.topic.children = response.data.comments;
							$scope.repliesLoaded = true;
						},
						function error(response) {
							console.error(response);
						}
					);
			}
		}
		
		$scope.createReply = function() {
			$scope.newReply = {
				replyTo: $scope.topic.Id
			};
		}
		
		$scope.saveNewReply = function() {
			CommentsService.saveReply($scope.newReply)
				.then(
					function success(response) {
						_.forEach(response.data.comments, function(comment) {
							if (!$scope.topic.children) {
								$scope.topic.children = [ ];
							}
							$scope.topic.children.push(comment);
						});
						
						$scope.createReply();
						$scope.inlineReply = true;
					},
					
					function error(response) {
						console.error(response);
					}
				);
		}
		
		$scope.$on('emf-comments:ng:cancel-editing', function() {
			$scope.editing = false;
		});
		
		$scope.$on('emf-comments:ng:update-topic', function(event, data) {
			if ($scope.topic.Id === data.Id) {
				$scope.updateTopic(data);
			}
		});
		
		$scope.$on('emf-comments:ng:toggle-collapsed', function(event, data) {
			if ($scope.topic.Id === data.Id) {
				$scope.collapsed = !$scope.collapsed;
			} else {
				$scope.collapsed = true;
			}
		});
		
		$scope.$watch('collapsed', function(value) {
			if (value != null && typeof value !== 'undefined') {
				$scope.$emit('emf-comments:ng:topic-toggle-collapsed',{
					topic: $scope.topic,
					collapsed: value
				});
			}
		});
		
		$scope.updateTopic = function(data) {
			var data = data || $scope.editModel;
			$scope.$emit('emf-comments:ng:before-save-topic', data);
			CommentsService.saveTopic(data)
				.then(
					function(response) {
						$scope.$emit('emf-comments:ng:replace-topic', response.data);
						$scope.editing = false;
					},
					function(response) {
						console.error(response);
					}
				);
		}
		
		$scope.changeStatus = function(status) {
			$scope.topic.status = status;
			$scope.updateTopic($scope.topic);
		}
		
		$scope.deleteTopic = function() {
			CommentsService.deleteTopic($scope.topic.Id)
			.then(
				function(response) {
					$scope.$emit('emf-comments:ng:remove-topic', $scope.topic);
				},
				function(response) {
					console.error(response);
				}
			);
		}
		
		$scope.editTopic = function() {
			$scope.editModel = angular.copy($scope.topic);
			$scope.editing = true;
		}
		
		$scope.$on('emf-comments:ng:toggle-expand', function(event, model) {
			if (model.Id === $scope.topic.Id) {
				event.preventDefault();
				$scope.toggleReplies();
			}
		});

		$scope.$on('emf-comments:ng:remove-reply', function(event, reply) {
			var index = _.findIndex($scope.topic.children, function(item) {
				return item.Id === reply.Id;
			});
			if (index > -1) {
				$scope.topic.children.splice(index, 1);
			}
		});
		
		$scope.$on('emf-comments:ng:replace-reply', function(event, reply) {
			var index = _.findIndex($scope.topic.children, function(item) {
				return item.Id === reply.Id;
			});
			if (index > -1) {
				$scope.topic.children[index] = reply;
			}
		});
		
		$scope.$on('emf-comments:ng:execute-action', function(event, action) {
			if (event.defaultPrevented) {
				return;
			}
			
			event.preventDefault();
			switch (action) {
				case 'createComment':
					$scope.createReply();
					break;
				case 'delete':
					EMF.dialog.confirm({
						message: CommentsConfig.topicDeleteMessage,
						confirm: $scope.deleteTopic
					});
					break;
				case 'suspend':
					$scope.changeStatus('ON_HOLD');
					break;
				case 'restart':
					$scope.changeStatus('IN_PROGRESS');
					break;
				case 'editDetails':
					$scope.editTopic();
					break;
				default:
					break;
			}
		});
		
		$scope.$on('emf-comments:ng:load-actions', function(event, model) {
			if (event.defaultPrevented) {
				return;
			}
			event.preventDefault();
			CommentsService.loadTopicActions($scope.topic)
				.then(
					function success(response) {
						$scope.topic.actions = response.data.actions;
						$scope.$broadcast('emf-comments:ng:actions-loaded', $scope.topic);
					},
					function error(response) {
						console.log(response);
					}
				);
		});
		
		TagsService.addAll($scope.topic.tags);
	}]);
	
	module.controller('ReplyController', ['$scope', 'CommentsService', 'CommentsConfig', function($scope, CommentsService, CommentsConfig) {
		
		$scope.editing = false;
		$scope.inline = true;
		
		$scope.updateReply = function() {
			CommentsService.saveReply($scope.reply)
				.then(
					function success(response) {
						$scope.$emit('emf-comments:ng:replace-reply', $scope.reply);
						$scope.editing = false;
					},
					function error(response) {
						console.log(response);
					}
				);
		}
		
		$scope.cancelEditReply = function() {
			$scope.editing = false;
		}
		
		$scope.editReply = function() {
			$scope.editing = true;
		}
		
		$scope.togglePopupEditor = function() {
			$scope.inline = false;
		}
		
		$scope.deleteReply = function() {
			CommentsService.deleteReply($scope.reply.replyTo, $scope.reply.Id)
				.then(
					function success(response) {
						$scope.$emit('emf-comments:ng:remove-reply', $scope.reply);
					},
					function error(response) {
						console.log(response);
					}
				);
		}
		
		$scope.$on('emf-comments:ng:cancel-editing', function() {
			$scope.editing = false;
		});
		
		$scope.$on('emf-comments:ng:execute-action', function(event, action) {
			if (event.defaultPrevented) {
				return;
			}
			
			event.preventDefault();
			switch (action) {
				case 'delete':
					EMF.dialog.confirm({
						message: CommentsConfig.replyDeleteMessage,
						confirm: $scope.deleteReply
					});
					break;
				case 'editDetails':
					$scope.editReply();
					break;
				default:
					break;
			}
		});
	}]);
	
	module.controller('InlineCommentController', ['$scope', function($scope) {
		$scope.loadingActions = false;
		$scope.topicActionsModel = null;
		
		$scope.defaultUserIconPath = function() {
			return EMF.applicationPath + '/images/user-icon-32.png';
		}
		
		$scope.loadActions = function() {
			var actions = $scope.model.actions;
			if (!actions || !actions.length) {
				$scope.loadingActions = true;
				$scope.$emit('emf-comments:ng:load-actions', $scope.model);
			}
		}
		
		$scope.executeAction = function(action) {
			$scope.$emit('emf-comments:ng:execute-action', action);
		}

		$scope.triggerToggleExpand = function(model) {
			$scope.$emit('emf-comments:ng:toggle-expand', model);
			$scope.loadActions();
		}
		
		$scope.$on('emf-comments:ng:actions-loaded', function(event, model) {
			if (model.Id === $scope.model.Id) {
				$scope.loadingActions = false;
				$scope.buildTopicActionsModel();
			}
		});
		
		$scope.buildTopicActionsModel = function() {
			var actions = $scope.model.actions,
				actionsLength = actions.length;
			
			$scope.topicActionsModel = { secondary: [ ] };
			
			for (var int = 0; int < actionsLength; int++) {
				var action = actions[int];
				if (action.action === 'createComment') {
					continue;
				} else {
					if (action.action === 'editDetails') {
						$scope.topicActionsModel.primary = action;
					} else {
						$scope.topicActionsModel.secondary.push(action);
					}
				}
			}
		}
	}]);
	
	module.controller('InlineCommentEditorController', ['$scope', function($scope) {
		
		$scope.defaultUserIconPath = function() {
			return EMF.applicationPath + '/images/user-icon-32.png';
		}
		
		$scope.toggleInlineMode = function() {
			$scope.$broadcast('emf-comments:ng:inline-editor-update-model');
			$scope.$eval($scope.toggleEditorMode);
		}
	}]);
	
}());