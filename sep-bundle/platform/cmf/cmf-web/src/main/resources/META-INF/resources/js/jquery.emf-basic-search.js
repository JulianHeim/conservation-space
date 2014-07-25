/**
 * EMF basic search jquery plugin.
 * Configuration:
 * 
 * 	contextPath: string
 * 		base path to use for rest calls and the retrieval of the template for the search UI
 * 
 * 	searchTemplateUrl: string
 * 		path to the template for the search UI (w/o the context path)
 * 
 * 	search: string
 * 		path to the REST service that performs the search
 * 
 * 	locationOptionsReload: string or function
 * 		path to the REST service that retrieves the available context objects, or a function.
 * 		The function must return an array of objects in the format of { name: 'object_key', title: 'Object Title' }
 * 
 * 	objectTypeOptionsLoader: string or function
 * 		path to the REST service that retrieves the available object types, or a function.
 * 		The function must return an array of objects in the format of { name: 'object_key', title: 'Object Title' }
 * 		
 * 	objectRelationOptionsLoader: string or function
 * 		path to the REST service that retrieves the available object relationships, or a function.
 * 		The function must return an array of objects in the format of { name: 'object_key', title: 'Object Title' }
 * 
 * 	orderByFieldsLoader: string or function
 * 		path to the REST service that retrieves the available 'order by' fields, or a function.
 * 		The function must return an array of objects in the format of { name: 'object_key', title: 'Object Title' }
 * 
 * 	searchUsersUrl: string
 * 		path to the REST service used to retrieve user names when searching by 'createdBy' field.
 * 
 * 	resultItemDecorators: array of functions
 * 		Each function is called after a search has been performed and a result must be rendered.
 * 		Each decorator receives the raw item data as well as the DOM element.
 * 		Each decorator must return the changed DOM element.
 * 
 * 	defaultsIfEmpty: boolean
 * 		If true, before the search is performed empty search args will be replaced with their default values
 * 
 * 	onSearchCriteriaChange: array of functions
 * 		Each function is called whenever a change in the search criteria occurs passing the changed search criteria object.
 * 
 * 	onAfterSearch: array of functions
 * 		Each function is called after a search has been performed. No arguments are passed to the functions.
 * 
 * 	onItemAfterSearch: array of functions 
 * 		Each function is called for each object in the result. The raw object data is passed to the function.
 * 
 * 	onBeforeSearch: array of functions
 * 		Each function is called just before the search url is invoked. An object containing the search criteria is passed to the function.
 * 			{ args: { ... } }
 * 
 * 	initialSearchArgs: object
 * 		Object containing the initial search arguments. Useful when there's a need to restore a previously saved search.
 * 	
 * 
 * 	Functions: 
 * 		usage - $('.selector').basicSearch('functionName', [arg1, arg2, ..., argN])
 * 		
 * 	getSearchArgs: 
 * 		returns the search arguments as an object
 * 
 * 	resetSearch:
 * 		replaces the search criteria with the initial
 * 	
 * 	
 * 
 * @param $ jQuery object to use.
 */
(function($) {
	'use strict'
	
	/**
	 * Plugin instance constructor.
	 * @param options plugin configuration options
	 * @param element DOM element on which the search was called on
	 */
	function EmfBasicSearch(options, element) {
		var contextPath = options.contextPath || SF.config.contextPath;

		this.element = element;
		this.options = $.extend(true, {
			debugEnabled				: EMF.config.debugEnabled,
			// default settings
			contextPath					: contextPath,
			searchTemplateUrl			: contextPath + '/search/basic-search.tpl.html',
			
			fieldNameToSelectorMapping	: {
				'objectType'			: '.object-type',
				'objectRelationship'	: '.object-relationship',
				'location'				: '.location-select',
				'orderBy'				: '.order-by',
				'orderDirection'		: '.order-direction',
				'createdFromDate'		: '.created-from',
				'createdToDate'			: '.created-to',
				'createdBy'				: '.created-by',
				'metaText'				: '.search-meta'
			},
			
			initialSearchArgs: {
				pageNumber				: 1,
				pageSize				: 10,
				createdBy				: [ ],
				createdByValue			: [ ],
				orderBy					: 'emf:modifiedOn',
				orderDirection			: 'desc',
				location				: [ ],
				objectRelationship		: [ ],
				objectType				: [ ],
				createdFromDate			: '',
				createdToDate			: '',
				metaText				: ''
			},
			
			searchFieldsConfig			: { },
			
			dateFormatPattern			: SF.config.dateFormatPattern,
			firstDay					: SF.config.firstDay,
			monthNames 					: SF.config.monthNames,
			monthNamesShort				: SF.config.monthNamesShort,
			dayNames					: SF.config.dayNames,
			dayNamesMin 				: SF.config.dayNamesMin,
			
			search						: contextPath + '/service/search/basic',
			locationOptionsReload		: contextPath + '/service/search/basic',
			objectTypeOptionsLoader		: contextPath + '/service/definition/all-types',
			objectRelationOptionsLoader	: contextPath + '/service/definition/relationship-types',
			searchUsersUrl				: contextPath + '/service/search/users',
			orderByFieldsLoader			: function() {
				return [
				        { name: 'Title', value: 'dcterms:title'},
				        { name: 'Modified On', value: 'emf:modifiedOn'},
				        { name: 'Modified By', value: 'emf:modifiedBy'},
				        { name: 'Created On', value: 'emf:createdOn'},
				        { name: 'Created By', value: 'emf:createdBy'}
				];
			},
			
			resultItemDecorators		: [ ],
			
			onSearchCriteriaChange		: [ ],
			onAfterSearch				: [ ],
			onItemAfterSearch			: [ ],
			onBeforeSearch				: [ ],
			
			listeners					: { }
		}, options);
		
		this.searchArgs = $.extend(true, { }, this.options.initialSearchArgs);
		this.init();
	}
	
	EmfBasicSearch.prototype = {
		eventNames: {
			TEMPLATE_LOADED: 'basic-search:template-loaded',
			OPTIONS_LOADED: 'basic-search:options-loaded',
			AFTER_SEARCH: 'basic-search:after-search',
			AFTER_SEARCH_ACTION: 'basic-search:after-search-action'
		},
		
		_defaultFieldController: {
			disabled: function() {
				if (arguments.length) {
					if (arguments[0] === true) {
						this.field.prop('disabled', 'disabled');
					} else {
						this.field.removeProp('disabled');
					}
				} else {
					return this.field.prop('disabled') ? true : false;
				}
			},
			
			value: function() {
				if (arguments[0] || typeof arguments[0] === 'string') {
					this.field.val(arguments[0]);
					this.field.trigger('change');
				} else {
					return this.field.val();
				}
			}
		},
			
		_orderDirectionController: {
			value: function() {
				if (arguments[0] || typeof arguments[0] === 'string') {
					this.searchArgs.orderDirection = (arguments[0] === 'asc') ? 'asc' : 'desc';
				} else {
					return this.searchArgs.orderDirection;
				}
			}
		},
			
		_dateFieldController: {
			disabled: function() {
				if (arguments.length) {
					if (arguments[0] === true) {
						this.field.datepicker('disable');
					} else {
						this.field.datepicker('enable');
					}
				} else {
					return this.field.prop('disabled') ? true : false;
				}
			},
	
			value: function() {
				if (arguments[0] || typeof arguments[0] === 'string') {
					this.field.val(arguments[0]);
					this.field.trigger('change');
					this.field.datepicker('refresh')
				} else {
					return this.field.val();
				}
			}	
		},
			
		_autocompleteFieldController: {
			disabled: function() {
				if (arguments.length) {
					if (arguments[0] === true) {
						this.field.prop('disabled', 'disabled');
					} else {
						this.field.removeProp('disabled');
					}
				} else {
					return this.field.prop('disabled') ? true : false;
				}
			},
			
			value: function() {
				if (arguments[0]) {
					if (arguments[0].length > 0){
						arguments[0].push('');
					}
					this.field.val(arguments[0].join(', '));
					this.field.trigger('autocompletechange');
				} else {
					return this.field.val().replace(/,\s*$/, '').split(/\s*,\s*/);
				}
			}
		},
			
		_chosenSelectController: {
			disabled: function() {
				if (arguments.length) {
					if (arguments[0] === true) {
						this.field.prop('disabled', 'disabled');
					} else {
						this.field.removeProp('disabled');
					}
					this.field.trigger('chosen:updated');
				} else {
					return this.field.prop('disabled') ? true : false;
				}
			},
			
			value: function(value) {
				if (value) {
					this.field.children().removeProp('selected');
					
					if (typeof value === 'string') {
						this.field.children('[value="' + value + '"]').prop('selected', 'selected');
					} else {
						var _this = this;
						$.each(value, function() {
							var child = _this.field.children('[value="' + this + '"]');
							child.prop('selected', 'selected');
						});
					}
					
					this.field.trigger('change');
					this.field.trigger('chosen:updated');
				} else {
					var multiple = this.field.prop('multiple') ? true : false;
					var selected = this.field.prop('multiple') ? [ ] : '';
					
					var selectedSubTypes;
					var selectedSubTypeElements = this.field.find('.sub-type:selected');
					if (selectedSubTypeElements.length) {
						selectedSubTypes = multiple ? [ ] : '';
						selectedSubTypeElements.each(function() {
							var $this = $(this);
							if (selectedSubTypes.push) {
								selectedSubTypes.push($this.val());
							} else {
								selectedSubTypes = $this.val();
								return false;
							}
						});
					}
					
					this.field.find(':selected:not(.sub-type)').each(function() {
						var $this = $(this);
						if (selected.push) {
							selected.push($this.val());
						} else {
							selected = $this.val();
							return false;
						}
					});
					
					if (selectedSubTypes) {
						var result = {
							selected: selected,
							selectedSubTypes: selectedSubTypes
						}
						return result;
					}
					
					return selected;
				}
			}	
		},
		
		init: function() {
			var _this = this;
			
			_this.element.on(_this.eventNames.TEMPLATE_LOADED, this._onTeplateLoaded);
			_this.element.on(_this.eventNames.OPTIONS_LOADED, this._onOptionsLoaded);
			_this.loadTemplate();
			
			if (_this.options.listeners) {
				for (var key in _this.options.listeners) {
					var value = _this.options.listeners[key];
					
					if (typeof value === 'function') {
						_this.element.on(key, value);
					} else if (toString.call(value) === '[object Array]') {
						for (var index in value) {
							_this.element.on(key, value[index]);
						}
					}
				}
			}
			
			_this.element.on('perform-basic-search', function() {
    			_this.searchArgs.pageNumber = 1;
    			_this._doSearch();
			});
			
			_this.element.trigger('basic-search-initialized');

			_this.applySearchArguments();
		},
		
		/**
		 * Check if query string contains some search arguments and initialize the search fields 
		 * with provided search criteria. If there are search criteria passed trough the query string,
		 * then we execute the search after fields and arguments initialization.
		 */
		applySearchArguments: function() {
			var _this = this,
				// get available search criteria fields 
				fieldNames = _this.options.fieldNameToSelectorMapping,
				// get query string parameters
				queryParameters = $.deparam(location.search) || {},
				// flag to show if search arguments are updated trough values passed as query parameters
				hasUpdatedSearchArguments = false;
				
			for ( var parameter in queryParameters) {
				var parameterValue = queryParameters[parameter];
				if(parameter in fieldNames) {
					// for metaText field we should have plain text
					if(parameter === 'metaText') {
						_this.options.initialSearchArgs[parameter] = parameterValue;
						_this.searchArgs[parameter] = parameterValue;
					} else {
						_this.options.initialSearchArgs[parameter] = [parameterValue];
						_this.searchArgs[parameter] = [parameterValue];
					}
					hasUpdatedSearchArguments = true;
				}
			}
			if (hasUpdatedSearchArguments) {
				_this.element.trigger('perform-basic-search');
			}
		},
		
		getParameterByName: function(name) {
		    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
		    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
		        results = regex.exec(location.search);
		    var res = (results == null) ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
		    return res;
		},
		
		getSearchArgs: function() {
			return this.searchArgs;
		},
		
		/**
		 * Load the template and append it to the placeholder element.
		 * After the template has been loaded and inserted into the DOM
		 * an event is fired indicating that the template was loaded.
		 */
		loadTemplate: function() {
			var _this = this;
			$.ajax({
        		type: 'GET', 
        		url: _this.options.searchTemplateUrl, 
        		complete: function(data) {
        			_this.template = data.responseText;
        			var templateAsElement = $(_this.template);
        			
                	$(templateAsElement).appendTo(_this.element);
                	
        			_this.element.trigger(_this.eventNames.TEMPLATE_LOADED, {
            			template : templateAsElement,
            			settings : _this.options
            		});
        			
        			_this._initSearchFields();
        		}
    		});
		},

		/**
		 * Reset the search with the initial criteria.
		 */
		resetSearch: function() {
			var _this = this;
			$.each(this.options.fieldNameToSelectorMapping, function(name, selector) {
				var controller = _this._controllers(name);
				controller.value(_this.options.initialSearchArgs[name]);
			});
			
			_this.element
				.find('.basic-search-results-wrapper .basic-search-results')
					.children()
					.remove();
			
			_this.element.find('.basic-search-pagination').hide();
		},
		
		_controllers: function(controllerName) {
			var selector = this.options.fieldNameToSelectorMapping[controllerName];
			return $(selector, this.element).data('controller');
		},
		
		/**
		 * Builds the actual search url that will be used for the search (w/ the search arguments).
		 * Useful when there's a need to invoke the search without the UI.
		 */
		buildSearchUrl: function() {
			// FIXME: use this function in _doSearch()
			var url = this.options.search;
			var searchData = {
				args: this.searchArgs
			}
			this._onBeforeSearch(searchData);
			url += '?' + $.param(searchData.args);
			return url;
		},
		
		_doSearch: function(resultHandler) {
			var _this = this;
			
			var searchData = {
				args: this.searchArgs
			}
				
			// do some magic before the search, like replacing the context constants with real object URIs
			this._onBeforeSearch(searchData);
			$.each(this.options.onBeforeSearch, function() {
				this.call(_this, searchData);
			});
			EMF.blockUI.showAjaxLoader();	
			$.ajax({ dataType: "json", url: this.options.search, data: searchData.args, async: true,
				complete: function(response) {
					var parsed = $.parseJSON(response.responseText);
					if (resultHandler) {
						resultHandler.call(_this, parsed);
					}
					_this.element.trigger(_this.eventNames.AFTER_SEARCH, parsed);
					// the next event is for external use
					_this.element.trigger(_this.eventNames.AFTER_SEARCH_ACTION, parsed);
					$.each(_this.options.onAfterSearch, function() {
						this.call(_this);
					});
					EMF.shorteningPlugin.shortElementText();
		    	}
			});
			
		},
		
		_afterSearchHandler: function(event, data) {
			var _this = this;
			_this._renderItems.call(_this, data);
			var paginationPlaceholder = $('.basic-search-pagination', _this.element);
    		
			if(data.values && data.values.length > 0) {
    			paginationPlaceholder.pagination({
    				items: data.resultSize,
    				currentPage: _this.searchArgs.pageNumber,
    				itemsOnPage: _this.searchArgs.pageSize,
    				cssStyle: 'compact-theme',
    				onPageClick: function(number, event) {
    					_this.searchArgs.pageNumber = number;
    					_this._doSearch();
    				}
    			}).show();
			} else {
				paginationPlaceholder.hide();
			}
		},
		
		_defaultItemRenderer: function(resultItem) {
    		var imagePath = EMF.util.objectIconPath(resultItem.type, 64);
    		var content = 
    			'<div class="tree-header default_header"> \
    				<div class="instance-header ' + resultItem.type + '"> \
    					<span class="icon-cell"> \
    						<img class="header-icon" src="' + resultItem.icon + '" width="64"> \
    					</span> \
    					<span class="data-cell">' + resultItem.default_header + '</span> \
    				</div> \
    			 </div>';
    			
			return $(content);
		},
		
		_renderItems: function(data) {
			var _this = this;
			var viewSearchResults = $('.basic-search-results-wrapper > .basic-search-results', this.element);
			viewSearchResults.children().remove();

			var resultsCountLabel = _emfLabels['basicSearch.xFound'];
    		var resultSize = data.resultSize;
    		var resultCountMessage = EMF.util.formatString(resultsCountLabel, 0);
    		if (resultSize >= 1000) {
    			resultCountMessage = _emfLabels['basicSearch.tooManyResults'];
    		} else if (resultSize > 0) {
    			resultCountMessage = EMF.util.formatString(resultsCountLabel, resultSize);
    		}
    		viewSearchResults.prepend($('<div class="no-content-message" />').html(resultCountMessage));

			if(data.values && data.values.length > 0) {
				var itemRenderer = _this.options.resultItemRenderer || _this._defaultItemRenderer;
				$.each(data.values, function(index) {
					var item = data.values[index];
					var itemElement = itemRenderer.call(_this, item);
					
					$.each(_this.options.resultItemDecorators, function() {
						itemElement = this.call(_this, itemElement, item);
					});

					$.each(_this.options.onItemAfterSearch, function() {
						this.call(_this, item);
					});
					
					itemElement.appendTo(viewSearchResults);
				});
			}
			EMF.blockUI.hideAjaxBlocker();
		},
		
		_onBeforeSearch: function(searchData) {
			if (!EMF.currentPath) {
				return;
			}

			if (searchData.args.location) {
				var newArgs = $.extend(true, { }, searchData.args);
				
				var replacedItems = [ ];
				var contextPath = EMF.currentPath;
				
				$.each(newArgs.location, function(index, value) {
					switch (value) {
					case 'emf-search-context-current-project':
						if (contextPath.length && contextPath[0].type === 'projectinstance') {
							replacedItems.push(contextPath[0].id);
						}
						break;
					case 'emf-search-context-current-case':
						if (contextPath.length >= 1 && contextPath[1].type === 'caseinstance') {
							replacedItems.push(contextPath[1].id);
						}
						break;
					case 'emf-search-context-current-object':
						if (contextPath.length) {
							replacedItems.push(contextPath[contextPath.length - 1].id);
						}
						break;
					default:
						replacedItems.push(value);
						break;
					}
				});
				newArgs.location = replacedItems;
				searchData.args = newArgs;
			}
			
			if (this.options.defaultsIfEmpty) {
				var objectTypes = searchData.args.objectType;
				if (!objectTypes || !objectTypes.length) {
					searchData.args.objectType = this.options.initialSearchArgs.objectType;
				}
			}
		},
		
		/**
		 * Init elements with data and event handlers.
		 */
		_initSearchFields: function() {
			var _this = this;
			
			var locationElement = $(this.options.fieldNameToSelectorMapping.location, this.element);
			locationElement.data('controller', $.extend(true, { field: locationElement }, _this._chosenSelectController));
			
    		var objectTypeElement = $(this.options.fieldNameToSelectorMapping.objectType, this.element);
    		objectTypeElement.data('controller', $.extend(true, { field: objectTypeElement }, _this._chosenSelectController));
			
    		var objectRelationshipElement = $(this.options.fieldNameToSelectorMapping.objectRelationship, this.element);
    		objectRelationshipElement.data('controller', $.extend(true, { field: objectRelationshipElement }, _this._chosenSelectController));
    		
    		var orderByElement = $(this.options.fieldNameToSelectorMapping.orderBy, this.element);
    		orderByElement.data('controller', $.extend(true, { field: orderByElement }, _this._chosenSelectController));
    		
    		var orderDirectionElement = $(this.options.fieldNameToSelectorMapping.orderDirection, this.element);
    		orderDirectionElement.data('controller', $.extend(true, { field: orderDirectionElement, searchArgs: _this.searchArgs }, _this._orderDirectionController));
    		
    		var createdFromElement = $(this.options.fieldNameToSelectorMapping.createdFromDate, this.element);
    		createdFromElement.data('controller', $.extend(true, { field: createdFromElement }, _this._dateFieldController));

    		var createdToElement = $(this.options.fieldNameToSelectorMapping.createdToDate, this.element);
    		createdToElement.data('controller', $.extend(true, { field: createdToElement }, _this._dateFieldController));
    		
    		var createdByElement = $(this.options.fieldNameToSelectorMapping.createdBy, this.element);
    		createdByElement.data('controller', $.extend(true, { field: createdByElement }, _this._autocompleteFieldController));

    		var searchMetaFieldElement = $(this.options.fieldNameToSelectorMapping.metaText, this.element);
    		searchMetaFieldElement.data('controller', $.extend(true, { field: searchMetaFieldElement }, _this._defaultFieldController));
    		
    		var resetSearchElement = $('.reset-btn', this.element);
    		
    		var changeHandler = function(event, data) {
    			var controller = $(event.target).data('controller');
    			_this._onSearchCriteriaChange(event, controller.value());
    		};
    		
    		var inputValueChangeHandler = function(event) {
    			_this._onSearchCriteriaChange(event, $(event.target).val());
    		}
    		
    		locationElement.on('change', changeHandler);
    		objectTypeElement.on('change', changeHandler);
    		objectRelationshipElement.on('change', changeHandler);
    		orderByElement.on('change', changeHandler);
    		orderDirectionElement.on('click', function(event) {
    			_this.searchArgs.pageNumber = 1;
    			orderDirectionElement.find('span').toggleClass('hide');
    			_this._onSearchCriteriaChange(event, _this.searchArgs.orderDirection === 'desc' ? 'asc' : 'desc');
    			_this._doSearch();
    		});
    		
    		var chosenConfigMultiSelect = { 
    	    	width: '200px'
    	    };
    		var chosenConfigSingleSelect = {
    			width: '130px',
        		disable_search: true
        	};
			
    		var predefinedContexts = [ ];
    		if (EMF.currentPath) {
				var contextPath = EMF.currentPath;
				if (contextPath.length && contextPath[0].type === 'projectinstance') {
					predefinedContexts.push({ 'name' : 'emf-search-context-current-project', 'title': 'Current Project' });
				}
				
				if (contextPath.length > 1 && contextPath[1].type === 'caseinstance') {
					predefinedContexts.push({ 'name' : 'emf-search-context-current-case', 'title': 'Current Case' });			
				}
				
				if (contextPath.length) {
					predefinedContexts.push({ 'name' : 'emf-search-context-current-object', 'title': 'Current Object' });
				}
			}
    		
    		var rootInstance = sessionStorage.getItem('emf.rootInstance');
			// Check if rootInstance exists in sessionStorage and consult backend if the instance
			// is not in deleted state. If not, then use it as context instance in basic search
    		// !!! Identical code is used in basic-search.xhtml
    		if (rootInstance) {
    			var rootInstanceObject = JSON.parse(rootInstance);
				$.ajax({
					url 		: SF.config.contextPath + "/service/instances/status",
					type		: 'GET',
					async		: false,// 
					data		: {
						instanceId: rootInstanceObject.id,
						instanceType: rootInstanceObject.type
					}
				}).done(function(data) {
					if(data) {
						if(data.status !== 'DELETED') {
							predefinedContexts.push({ 'name' : rootInstanceObject.id, 'title': rootInstanceObject.title });
						}
					}
				}).fail(function(data, textStatus, jqXHR) {
					console.error(arguments);
	            });
    		}
    		
    		$.each(predefinedContexts, function() {
    			var option = $('<option value="' + this.name + '">' + this.title + '</option>');
    			
    			locationElement.append(option);
    			if (_this.searchArgs.location && $.inArray(this.name, _this.searchArgs.location) !== -1) {
    				option.attr('selected', 'selected');
	    			var eventData = { selected: option.value };
	    			locationElement.trigger('change', eventData);
	    		}
    		});
    		_this._loadOptions(_this.options.locationOptionsReload, locationElement, chosenConfigMultiSelect, predefinedContexts);
    		_this._loadOptions(_this.options.objectRelationOptionsLoader, objectRelationshipElement, chosenConfigMultiSelect);
    		_this._loadOptions(_this.options.objectTypeOptionsLoader, objectTypeElement, chosenConfigMultiSelect);
    		_this._loadOptions(_this.options.orderByFieldsLoader, orderByElement, chosenConfigSingleSelect);
    		
    		var datePickerSettings = {
    			changeMonth		: true,
        		dateFormat		: _this.options.dateFormatPattern,
        		firstDay 		: _this.options.firstDay,
        		monthNames 		: _this.options.monthNames,
        		monthNamesShort	: _this.options.monthNamesShort,
        		dayNames		: _this.options.dayNames,
        		dayNamesMin 	: _this.options.dayNamesMin,
        		numberOfMonths	: 1
    		};
    		
    		createdFromElement.datepicker($.extend(datePickerSettings, {
    			onClose: function(selectedDate) {
    				createdToElement.datepicker("option", "minDate", selectedDate);
    			}
    		}));
    		// FIXME: use $( ".selector" ).datepicker( "getDate" ); in datepicker change handlers
    		createdFromElement.on('change', inputValueChangeHandler);
    		createdFromElement.val(_this.options.initialSearchArgs.createdFromDate);
    		
    		createdToElement.datepicker($.extend(datePickerSettings, {
    			onClose: function(selectedDate) {
    		    	createdFromElement.datepicker("option", "maxDate", selectedDate);
    		    }
    		}));
    		createdToElement.on('change', inputValueChangeHandler);
    		createdToElement.val(_this.options.initialSearchArgs.createdToDate);
    		
    		createdByElement.bind('keydown', function(event) {
		    	if (event.keyCode === $.ui.keyCode.TAB && $(this).data('ui-autocomplete').menu.active) {
		    		event.preventDefault();
		        }
		    	this.label = this.value;
		    }).autocomplete({
		    	source: function(request, response) {
		            $.getJSON(_this.options.searchUsersUrl, { term: _this._extractLast(request.term) }, response);
		        },
		        search: function() {
		        	// custom minLength
		        	var term = _this._extractLast( this.value );
		        	if ( term.length < 2 ) {
		        		return false;
		        	}
		        },
		        select: function(event, ui) {
		        	var terms = _this._split(this.value);
		        	// remove the current input
		        	terms.pop();
		        	// add the selected item
		        	terms.push(ui.item.label);
		        	// add placeholder to get the comma-and-space at the end
		        	terms.push( "" );
		        	this.value = terms.join( ", " );
		        	
		        	if(this.label) {
		        		var terms = _this._split(this.label);
		        	} else {
		        		var terms = [];
		        	}
		        	// remove the current input
		        	terms.pop();
		        	terms.push(ui.item.value);
		        	terms.push( "" );
		        	this.label = terms.join( ", " );

		        	return false;
		        },
		        focus: function() {
		        	// prevent value inserted on focus
		        	return false;
		        }
		    });
    		createdByElement.on('autocompletechange', function(event, ui) {
    			var _value = $(this).val();
    			var _label = this.label;
    			var array = {};
    			array.value = [];
    			array.label = [];

    			if (_value) {
    				array.value = _value.replace(/,\s*$/, '').split(/\s*,\s*/);
    				if(_label) {
    					array.label = _label.replace(/,\s*$/, '').split(/\s*,\s*/);
    				} else {
    					array.label = _value.replace(/,\s*$/, '').split(/\s*,\s*/);
    				}
    			}
    			_this._onSearchCriteriaChange(event, array);
	        });
    		createdByElement.val((_this.options.initialSearchArgs.createdByValue || []).join(', '));
    		
    		searchMetaFieldElement.on('change', inputValueChangeHandler);
    		searchMetaFieldElement.val(_this.options.initialSearchArgs.metaText);
    		
    		_this.element.on(_this.eventNames.AFTER_SEARCH, function(event, data) {
    			_this._afterSearchHandler(event, data);
    		});
    		
    		$('.search-btn', this.element).click(function() {
    			_this.element.trigger('perform-basic-search');
    		});
    		
    		$('.switch', this.element).on('click',function() {
    			_this.searchArgs = { };
    		    $('#basic-search, #advanced-search', this.element).toggle();
    		});
    		
    		resetSearchElement.click(function() {
    			_this.resetSearch();
    		});

    		// TODO: this could be move to the controllers as a configure function
    		// configure each field in the search e.g. set disabled property
    		$.each(_this.options.fieldNameToSelectorMapping, function(name, selector) {
    			if (_this.options.searchFieldsConfig[name]) {
    				_this._controllers(name).disabled(_this.options.searchFieldsConfig[name].disabled);
    			}
    		});
		},
		
		addSearchCriteriaListener: function(listener) {
			this.options.onSearchCriteriaChange.push(listener);
		},
		
		addItemAfterSearchListener: function(listener) {
			this.options.onItemAfterSearch.push(listener);
		},
		
		addBeforeSearchListener: function(listener) {
			this.options.onBeforeSearch.push(listener);
		},
		
		_loadOptions: function(urlOrFunction, element, chosenConfig, preloaded) {
			var _this = this;
			// this is needed because otherwise IE10 throws Invalid Calling Object error
			var preloaded = preloaded;
			// disable the dropdown until options are loaded
			element.prop('disabled', true);
	    	element.chosen(chosenConfig);
	    	
	    	var selectName = element.attr('data-name');
	    	var initialValue = _this.options.initialSearchArgs[selectName];
	    	if (typeof urlOrFunction === 'string') {
	    		$.ajax({
	    			dataType: "json",
	    			url: urlOrFunction,
	    			async: true,
	    			complete: function(response) {
		    			var result = $.parseJSON(response.responseText);
		    			if (result.values) {
		    				result.initialValue = initialValue;
		    				result.preloaded = preloaded;
		    				result.options = _this.options;
		    				element.trigger(_this.eventNames.OPTIONS_LOADED, result);
		    			} else {
		    				element.trigger(_this.eventNames.OPTIONS_LOADED, { 
		    					values: result, 
		    					initialValue: initialValue, 
		    					options: _this.options,
		    					preloaded: preloaded
		    				});
		    			}
		    		}
	    		});
	    	} else if (typeof urlOrFunction === 'function') {
	    		var values = urlOrFunction.call(_this);
	    		element.trigger(_this.eventNames.OPTIONS_LOADED, { 
	    			values: values, 
	    			initialValue: initialValue, 
					options: _this.options,
					preloaded: preloaded
	    		});
	    	}
		},
		
		_onOptionsLoaded: function(event, data) {
			var element 		= $(event.target),
				arraySearchFn 	= function(item) {
					return item.name === this.name;
				};
			
			// Create the select options, also check for default selected
       		$.each(data.values || [ ], function(index, option) {
       			if(option.name && option.title) {
       				if (data.preloaded && _.findIndex(data.preloaded, arraySearchFn, option) !== -1) {
       					return true;
       				}
       				var optionElement = null;
       				// We must separate objectType from subType !!
		    		if(option.subType) {
		    			optionElement = $('<option class="sub-type" />').attr('value', option.name).html(option.title);
		    		} else {
		    			optionElement = $('<option />').attr('value', option.name).html(option.title);
		    		}
		    		
		    		if(typeof option.objectType != 'undefined') {
			    		optionElement.addClass(option.objectType);
			    		optionElement.attr("type", option.objectType);
		    		}
		    		
		    		optionElement.appendTo(element);
		    		if (data.initialValue && $.inArray(option.name, data.initialValue) !== -1) {
		    			optionElement.attr('selected', 'selected');
		    			var eventData = { selected: option.name };
		    			element.trigger('change', eventData);
		    		}
		    		
	    		}
       		});
			
       		// after the element is ready for use - enable it once again
       		var name = element.attr('data-name');
       		if (!data.options.searchFieldsConfig[name] || data.options.searchFieldsConfig[name].disabled === false) {
       			element.prop('disabled', false);
       		}
       		element.trigger("chosen:updated");
		},
		
		_onSearchCriteriaChange: function(domEvent, data) {
			var _this = this;
			var element = $(domEvent.target);
			
			var elementName = element.attr('data-name');

			// Value is an object when we have selected a codelist sub-type, otherwise it's an array
			if (elementName === 'objectType') {
				
				// ({}).toString.call is needed insted toString.call because otherwise IE10 throws Invalid Calling Object error
				if ( ({}).toString.call(data) === '[object Array]') { 
					_this.searchArgs.subType = undefined;
					_this.searchArgs.objectType = data;
				} else {
					_this.searchArgs.objectType = data.selected;
					_this.searchArgs.subType = data.selectedSubTypes;
				}
			} else if(elementName === 'createdBy') {
				_this.searchArgs[elementName] = data.label;
				_this.searchArgs.createdByValue = data.value;
			} else {
				_this.searchArgs[elementName] = data;
			}
			
			$.each(_this.options.onSearchCriteriaChange, function() {
				this.call(_this, _this.searchArgs);
			});
		},
		
		_extractLast: function(term) {
    		return this._split(term).pop();
    	},
    	
    	_split: function(val) {
    		return val.split(/,\s*/);
    	}
	}
	
    $.fn.basicSearch = function(opt) {
		var args = Array.prototype.slice.call(arguments, 1);
    	var pluginInstance = this.data('EmfBasicSearch');
    	if (!pluginInstance) {
    		this.data('EmfBasicSearch', new EmfBasicSearch(opt, this));
    		return this;
    	} else {
    		if (pluginInstance[opt]) {
    			return pluginInstance[opt].apply(pluginInstance, args);
    		} else {
    			var controller = pluginInstance._controllers.call(pluginInstance, opt);
    			if (controller) {
    				return controller;
    			}
    		}
    	}
    }
}(jQuery));