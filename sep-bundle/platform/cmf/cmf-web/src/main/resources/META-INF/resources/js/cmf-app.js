/**
 * Create SF global namespace.
 */
var SF = SF || {};
// configuration properties
SF.config = SF.config || {};
SF.tagsDecorator = null;
SF.AjaxLoaderHandler = null;
SF.defaultTooltipPosition = 'top';

/** Init function executed on document.ready */
SF.init = function() {};

/** Create CMF global namespace. */
var CMF = CMF || {};

/** Create CMF under EMF namespace. */
EMF.CMF = EMF.CMF || {};


/**
 * CMF configuration properties. This configuration will be merged with these from EMF
 */
CMF.config = {
	dateFieldSelector			: '.cmf-date-field',
	dateTimeFieldSelector		: '.cmf-datetime-field',
	dateRangeStartFieldSelector	: '.cmf-date-range-start',
	dateRangeEndFieldSelector	: '.cmf-date-range-end',
	userLink					: '.logout-link.user-link',
	logoutLink					: '.logout-link.cmf-user-logout',
	// flag that prevent multiple action invocation
	lockBindingActions 			: false
};

/**
 * CMF main click handler. Dispatches invocation according to the
 * event source target.
 *
 * @param evt The event being fired.
 */
CMF.clickHandlerDispatcher = function(evt) {

	var srcTarget = $(evt.target);
	// Clicking on facet header causes the facet body to be expanded/collapsed
	if(srcTarget.is('.facet-header, .facet-header .facet-title')) {
		CMF.clickHandlers.facetToggle(srcTarget);
	}
	// clicking on region label should expand/collapse the region
	else if(srcTarget.is('.dynamic-form-panel .group-label')) {
		CMF.clickHandlers.generatedFormRegionToggle(srcTarget);
	}
	// Open/close the sorting options menu
	else if(srcTarget.is('#sortingOptionsMenu, #sortingOptionsMenu img, #sortingOptionsMenu span')) {
		CMF.clickHandlers.menuToggle(srcTarget);
	}
	// when is clicked on the custom dropdown menu
	else if(srcTarget.is('.selector-activator-link, .selector-activator-link span, .selector-activator-link img')) {
		CMF.clickHandlers.toggleActivatorMenus(evt);
	}
	// Open popup menu with action links for operations
	else if(srcTarget.is('.more-actions-link, .more-actions-link span, #more-actions-link img')) {
		CMF.clickHandlers.showHiddenActions(srcTarget);
	}
	else if(srcTarget.is('.dropdown-toggle.cmf-sep-actions,.dropdown-toggle.cmf-sep-actions .caret')){
		CMF.clickHandlers.toggleAllowedActions(srcTarget, evt);
	}
	else if(srcTarget.is('.facet-content-input-text')) {
		CMF.clickHandlers.selectInputText(srcTarget);
	}
	// handle clicks on case section collapse/expand icon
	else if(srcTarget.is('.section-togler')) {
		srcTarget.parent('.section-row').toggleClass('expanded')
			.next('.section-items-list').slideToggle();
	}
	else if(srcTarget.is('.main-menu .menu-item, .main-menu .menu-item-trigger, .main-menu .menu-item .icon, .main-menu .caret')) {
		var submenu = srcTarget.closest('.menu-item').find('.submenu');
		if(submenu.length > 0) {
			CMF.clickHandlers.mainMenuToggle(submenu);
		}
	}
	else if(srcTarget.is('.workflow-tab-toggler')) {
		srcTarget.closest('.group-header').find('.render-workflow-link').click();
	}
	else {
		// TODO close opened menus if any
		CMF.clickHandlers.closeOpenMenus();
	}
};

/**
 * Comments
 */
CMF.comments = {
	init : function(config) {
		$.comments.EMF = EMF || {};
		$.comments.rootId = EMF.documentContext.currentInstanceId;
		$.comments.rootType = EMF.documentContext.currentInstanceType;
		$.comments.parentId = '';
		$.comments.rootSource = '.idoc-main-editor-placeholder';
		$.comments.imagePathURL = EMF.applicationPath + '/images';
		$.comments.loadService = EMF.servicePath + '/comment';
		$.comments.saveService = EMF.servicePath + '/comment';
		$.comments.editService = EMF.servicePath + '/comment';
		$.comments.removeService = EMF.servicePath + '/comment/remove';
		$.comments.source = '.comments-actions';
		$.comments.target = '.comments-panel';
		$.comments.viewSubmitButton = false;
		$.comments.commentSelectors = '#null';

		$.extend($.comments, config);

		$.comments.init();
		$.comments.refresh();

		tinymce.init({
			selector: ".comment-editor",
			plugins: [
				"lists link image anchor internal_thumbnail_link"
			],
			toolbar: "bold italic | bullist numlist | link image | internal_link internal_thumbnail_link",
			menubar: false,
			statusbar: false
		});

		$.ajax({
			type : 'GET',
			url : EMF.servicePath + '/user',
			data : {},
			async : false,
			complete : function(res) {
			    var users = $.parseJSON(res.responseText);
			    for ( var i = 0; i < users.length; i++) {
				$("#author").append(
					"<option value=\"" + users[i].displayName + "\">"
						+ users[i].displayName + "</option>");
			    }
			}
		});
	},

	/**
	 * Open dialog with a form for new comment.
	 */
	showCommentBox: function() {
		$("#annotation_layouts").select2({
            createSearchChoice: function(term, data) {
				if ($(data).filter(function() { return this.text.localeCompare(term)===0; }).length===0) {return {id:term, text:term};} },
            multiple: true,
            data: [
				{id: 0, text: 'story'},
				{id: 1, text: 'bug'},
				{id: 2, text: 'task'}
			]
        });
		$.comments.showCommentBox(EMF.documentContext.currentInstanceId, null, true);
	},

	/**
	 * Reloads comments from the server.
	 */
	reload : function() {
		var rootId = $.comments.loadFilter?$.comments.loadFilter.instanceId:'';
		$.comments.getData(rootId, true);
		buildTree();
	},

	/**
	 * Apply number of records for topic in the comment dashlet's headers. Method is invoke
	 * on event <b>commentTopicSizeChange</b>.
	 *
	 * @param selector class name for counter container.
	 */
	applyCommentTopicSize : function(selector){
		config = {
			selector : {
				commentCount   : '.comments-counter'
			},
			text 	 : {
				results 	   : ' result(s) found.',
				errorContainer : 'Not available comment counter container !'
			}
		};
		// check for available selector and use the default if is not available
		var topicCounterSelector = selector ? selector : config.selector.commentCount;
		var topicCounterHolder = $(topicCounterSelector);
		if(topicCounterHolder){
			var topicNumber = $.comments.topicSize + config.text.results;
			topicCounterHolder.html(topicNumber);
		}else{
			console.error(config.text.errorContainer);
		}

	},

	/**
	 * Change filter button(from comment panels) styles and text.
	 *
	 * @param fromFilterButton boolean value that indicate the
	 *        invoking location.
	 */
	changeCommentFilterButtonStyle : function(fromFilterButton){
		var filterNewStyleClass = "comment-modify-filter";
		var filterButtonSelector = ".dashboard-panel .filterBtn";

		var filterLabel = _emfLabels['cmf.comment.filter.menu'];
		var filterModifyLabel = _emfLabels['cmf.comment.filter.modify.menu'];

		var filterButton = $(filterButtonSelector);

		filterButton.removeClass(filterNewStyleClass);
		filterButton.eq(0).html(filterLabel);

		if(fromFilterButton){
			filterButton.addClass(filterNewStyleClass);
			filterButton.eq(0).html(filterModifyLabel);
		}
	}
};

/**
 * Click handler functions.
 */
CMF.clickHandlers = {

	// Handler for the expand/collapse functionality of the facets.
	facetToggle : function(elem) {
		var facetHeader = elem.closest('.facet-header');
		facetHeader.toggleClass('expanded').parent().find('.facet-content').slideToggle(function() {
			var expanded = facetHeader.siblings(".facet-content").is(':visible');
			if (expanded) {
				facetHeader.children("a").css('display','inline');
			} else {
				facetHeader.children("a").css('display','none');
			}
		});
	},

	// Handler for open/close of the sorting menu
	mainMenuToggle : function(submenu) {
		if (submenu.is(':visible')) {
			submenu.hide();
		} else {
			$('.menu-item .submenu').hide();
			submenu.toggle();
		}
	},

	// generated form region toggle
	generatedFormRegionToggle : function(regionLabel) {
		regionLabel.toggleClass('collapsed').siblings('.region-body, .radio-button-panel, .tasktree-panel').slideToggle();
	},

	// Handler for open/close of the sorting menu
	menuToggle : function(elem) {
		elem.closest('div').find('ul').toggle();
	},

	// Handler for showing/hiding hidden action buttons
	showHiddenActions: function(elem) {
		elem.closest('ul').siblings('.hidden-actions').show().on('mouseleave', function() {
				var actions = $(this);
				setTimeout(function() {
					actions.hide();
				}, 200);
			});
	},

	selectInputText : function(elem) {
		elem.select();
	},

	// toggles the panels in workflows tab
	workflowHistoryPanelToggle : function(elem) {
		var groupHeader = $(elem).closest('.group-header');
		var group = groupHeader.closest('.workflow-panel');
		var panel = group.find('.dynamic-form-panel');

		// once a panel is rendered, then don't allow submit anymore but just expand collapse the panel
		if(panel.children().length > 0) {
			$(elem).toggleClass('expanded');
			panel.closest('.workflow-history-content').slideToggle();
			EMF.config.pageBlockerOFF = true;
			return false;
		} else {
			return true;
		}
	},

	closeOpenMenus : function(){
		$('.items-list.rf-ulst, .submenu').hide();
		var cloneObjectActions = $('.cloned-allowed-actions');
		if(cloneObjectActions.length){
			cloneObjectActions.remove();
		}
	},

	//toggles single drop-down menu on user dash board
	toggleActivatorMenus : function(event) {
		var target = $(event.target);
		var targetMenu = target.closest('div').find('ul');
		var targetMenuVisible = targetMenu.is(':visible');
		CMF.clickHandlers.closeOpenMenus();
		targetMenu.mouseleave(function(){
			$(this).hide();
			return;
		});
		targetMenu.click(function(){
			$(this).hide();
		});
		if (targetMenuVisible){
			targetMenu.hide();
		} else {
			targetMenu.show();
		}
	},

	/**
	 * 	Method that manage action list dialog. Will be invoked when the client
	 *  click on the action button for additional actions visualization.
	 *  
	 *  Additional information:
	 *  1. Prevent action applied from bootstrap core.
	 *  2. Cross-browser fixes(offset).
	 */
	toggleAllowedActions : function(element, evt){
		evt.preventDefault();
		evt.stopPropagation();
		CMF.clickHandlers.closeOpenMenus();
		var contentHolder  		    = $('#content');
		var clonActionClassPointer  = "cloned-allowed-actions";
		var actionButton   	  	    = $(element);
		var parent  	   		    = actionButton.closest('div');
		var defaultStyles = {
			'width'	   : 'auto',
			'min-width': '200px',
			'max-width': '400px'
		};
		
		// calculate parent offset from top, needed for further 
		// action list positioning, also fix IE8 offset
		var scrollOffset			= CMF.clickHandlers.findOverflowedParent(actionButton);
		var actions = parent.children('ul:eq(0)').clone();
		var actionsOffset = CMF.clickHandlers.findTotalOffset(actionButton[0]);
		
		scrollOffset = (scrollOffset == null) ? 0 : scrollOffset.scrollTop();
		actions.css(defaultStyles);
		actions.addClass(clonActionClassPointer);
		contentHolder.after(actions);
		
		// action list position fixes, based on action button
		var topFix = 14;
		var leftFix = 18;
		if(actionButton.length && actionButton.prev().length){
			topFix = 20;
			leftFix = 23;
			scrollOffset = 0;
		}
		var positionStyles = {
			'top'      : (actionsOffset.top + topFix) - scrollOffset,
			'left'     : (actionsOffset.left + leftFix) - actions.width()
		}
		actions.css(positionStyles);
		actions.show().on('mouseleave',function(){
			$(this).remove();
		});
	},
	
	/**
	 * Help method for calculating specific element offset based on 
	 * their parent.
	 */
	findTotalOffset : function(object) {
		var offsetLeft = offsetTop = 0;
		if (object.offsetParent) {
			do {
				offsetLeft += object.offsetLeft;
				offsetTop += object.offsetTop;
		    }while (object = object.offsetParent);
		 }
		return {left : offsetLeft, top : offsetTop};
	},
	
	/**
	 * 	Check for parent that has <code>overflow</code>.
	 *  Needed for further offset calculation.
	 */
	findOverflowedParent : function(element){
		var parent = element.parent();
		 if(parent.is(document)){
			 return null;
		 }
		 if(parent.css('overflowY') == 'auto'){
			 return parent;
		 }
		 return CMF.clickHandlers.findOverflowedParent(parent);
	}
};

/**
 * Keyup handler functions.
 */
CMF.keyUpHandlers = {

	toggleSaveLinkButton : function(elem, button, hasSelectedCase) {
		var elemValue = CMF.trim($(elem).eq(0).val());
		// enable button
		if(elemValue && hasSelectedCase) {
			$(button).removeAttr('disabled');
		}
		// disable the button
		else {
			$(button).attr('disabled', 'disabled');
		}
	},

	toggleSaveButton : function(elem, button) {
		var elemValue = CMF.trim($(elem).eq(0).val());
		// enable button
		if(elemValue) {
			$(button).removeAttr('disabled');
		}
		// disable the button
		else {
			$(button).attr('disabled', 'disabled');
		}
	}
};

/**
 * Util for initializing specific components for search pages.
 */
CMF.searchPages = {
	
	init : function(){
		// Initialize select2 drop-downs
		CMF.searchPages.initSelectDropDown();
		
		// Initialize date ranges if any.
		CMF.utilityFunctions.initDateRange(CMF.config.dateRangeStartFieldSelector, CMF.config.dateRangeEndFieldSelector);

		// Initialize date fields if any.
		CMF.utilityFunctions.initDateFields();
	},
	
	initSelectDropDown : function(){
		// initialize dropdown menus inside search pages
		$(".search-page select").select2({
			width : function(){
				_width = "300px";
				if(this && this.element){
					var parent = this.element.parent();
					if(parent.hasClass('search-page-selector')) {
						_width = "170px";
					}
				}
				return _width;
			},
			allowClear: true,
			minimumResultsForSearch: EMF.config.comboAutocompleteMinimumItems,
			placeholder: _emfLabels['cmf.dropdown.placeholder']
		});
	}
},

/**
 *  Util for managing vertical splitter. At the beginning we check vertical splitter is
 *  supported by the concret page by identifier.
 */
CMF.verticalSplitter = {
	
	attachSplitterTo : function(containerClass){
		var splitAreaIdentifier = '#facets';
		splitAreaIdentifier = $(splitAreaIdentifier);
		var isPageSupportSplitter = splitAreaIdentifier.length > 0 ? true : false;
		if(isPageSupportSplitter){
			// if left container is empty - hide it and terminate scroller initialization
			if($.trim(splitAreaIdentifier.html()) == ''){
				splitAreaIdentifier.hide();
				return;
			}
		}
		
		if(!$.trim(containerClass).length){
			// some debug messages here
			return;
		}
		
		// if there is a facet panel on the page, then initialize the splitter plugin
		if(isPageSupportSplitter) {
			$(containerClass).splitter({
				type: "v",
				outline: true,
				minLeft: 250,
				sizeLeft: 250,
				// FIXME: debug for the reason of the 'too much recursion' error when this is set to be true
				// We actually need this enabled in order to allow auto resize from the cookie store when the page
				// is opened again or other page with splitter is opened.
				// http://stackoverflow.com/questions/10097458/splitter-js-wont-work-with-new-versions-of-jquery
				// http://stackoverflow.com/questions/5321284/fix-for-jquery-splitter-in-ie9/10505994#10505994
				// https://github.com/e1ven/jQuery-Splitter
				// Confluense uses same plugin too and they manage to fix this issue, but their version don't work for us too!
				resizeToWidth: false,
				cookie: "emf.vsplitter",
				cookiePath: '/emf',
				accessKey: 'I'
			});
		}
	}
},

/**
 * Functions used in file upload panels.
 */
CMF.fileUpload = {

	disableFields : function(fileUpload) {
		var $fileUpload = $(fileUpload);
		this.disableFileUploadButton($fileUpload);

		var panelContent = $fileUpload.closest('.panel-content');
		if(panelContent.length > 0) {
			var currentPanel = panelContent.eq(0);
			this.toggleFieldsDisabledStatus(currentPanel, true);
		}
	},

	enableFields : function(fileUpload) {
		var $fileUpload = $(fileUpload);

		var panelContent = $fileUpload.closest('.panel-content');
		if(panelContent.length > 0) {
			var currentPanel = panelContent.eq(0);
			this.toggleFieldsDisabledStatus(currentPanel, false);
		}
	},

	toggleFieldsDisabledStatus : function(currentPanel, disable) {
		if(disable) {
			currentPanel.find('.data-group').append('<span class="button-blocker" style="opacity:.0"></span>');
		} else {
			currentPanel.find('.data-group').find('.button-blocker').remove();
		}
	},

	toggleUploadButton : function(fields, fileUpload) {
		var enable = true;

		// check if any of provided fields has a value
		for ( var i = 0; i < fields.length; i++) {
			var field = $(fields[i]);
			var fieldValue = field.val() || field.find(':selected').val() || "";
			fieldValue = CMF.trim(fieldValue);
			if (!fieldValue) {
				enable = false;
			}
		}

		if(enable) {
			this.enableFileUploadButton(fileUpload);
		} else {
			this.disableFileUploadButton(fileUpload);
		}
	},

	// place an overlay in front of the buttons pane to prevent clicking
	disableFileUploadButton : function(fileUpload) {
		var $fileUpload = $(fileUpload);
		if($fileUpload.find('.button-blocker').length == 0) {
			$fileUpload.find('.rf-fu-btns-lft').append('<span class="button-blocker"></span>');
		}
	},

	enableFileUploadButton : function(fileUpload) {
		$(fileUpload).find('.button-blocker').remove();
	},

	resetUploadPanel : function(fileUpload) {
		this.disableFileUploadButton(fileUpload);

		var $fileUpload = $(fileUpload);

		$fileUpload.find('.rf-fu-btn-cnt-clr').click();

		var panelContent = $fileUpload.closest('.panel-content');

		this.selectVersionType(panelContent);

		if(panelContent.length > 0) {
			var currentPanel = panelContent.eq(0);
			currentPanel.find('input[type=text], textarea, select').each(function() {
				var currentField = $(this);
				if(currentField.is('input')) {
					currentField.val('');
				} else if(currentField.is('textarea')) {
					currentField.val('');
				} else if(currentField.is('select')) {
					currentField.find('option:selected').removeAttr('selected');
				}
			});
		}
	},

	selectVersionType : function(panelContent) {
		var radios = panelContent.find('.file-version-type :radio');
		if(radios.length > 0) {
			radios.eq(0).attr('checked', 'checked');
		}
	},

	onuploadcompleteHandler : function(fileUpload, popup) {
		document.getElementById(popup.id+"_container").style.zIndex = "100";
		popup.hide();
		this.resetUploadPanel();
		this.disableFileUploadButton(fileUpload);
		this.enableFields('.file-upload-new-version');
	},

	changeUploadPanelIndexes : function(popup){
		var bgShaderContainer = document.getElementById(popup+"_shade");
		if(!$.browser.msie){
			document.getElementById(popup+"_container").style.zIndex = "99";
		}
		bgShaderContainer.onclick = null;

	},

	//automatically selects item in a single-item drop-down list when file upload panel shows
	changeFileUploadActivatorLink : function(fileUpload) {
		var selector = $(fileUpload).closest('.panel-content').find('.selector-wrapper').find('#fileUpload_activatorLink');
		var items = selector.next('ul').find('li');
		if (selector.length == 1 && items.length == 1) {
			items.eq(0).find('a').click();
			CMF.timer1 = setInterval(removeBlocker, 300);
		}

		function removeBlocker() {
			var blocker = $('.file-upload').find('.button-blocker');
			if(blocker.length > 0) {
				blocker.remove();
				clearInterval(CMF.timer1);
			}
		}
	},

	fileTypeSelectedhandler : function(selectedItem, fileTypeField, selectedKey, selectedValue, fileUpload) {
		fileTypeField.value = selectedKey;
		this.enableFileUploadButton(fileUpload);
		CMF.clickHandlers.menuToggle($(selectedItem));
		$('.selector-activator-link span').text(selectedValue);
		return false;
	}

};


/**
 * CMF utility functions.
 */
CMF.utilityFunctions = {

	hideIframe: function(iframeSelector, hide) {
		if(!iframeSelector) {
			return;
		}
		var iframeElement = document.getElementById(iframeSelector);
		if (iframeElement) {
			if(hide) {
				iframeElement.style.display = 'none';
			} else {
				iframeElement.style.display = 'block';
			}
		}

		var newVersionDialogSelector = '[id*="NewVersion"].modal-panel';
		var newVersionDialog = $(newVersionDialogSelector);

		// apply static width for new version dialog
		// NOTE: remove this after change dialogs
		newVersionDialog.addClass('new-version-width-fix');

		var searchModelDialogSelector = ".search-modal-dialog:last";
		var searchModelDialog = $(searchModelDialogSelector);

		// apply static width for search modal dialog
		// NOTE: remove this after change dialogs
		searchModelDialog.addClass('search-modal-fix');
	},
	
	/**
	 * Method that will be used for all modal dialogs and will 
	 * center it based on the viewport.
	 */
	centerModalDialog : function(id){
		if(!id || $.trim(id) == ''){
			return;
		}
		var uiComponent = document.getElementById(id+'_container');
		uiComponent = $(uiComponent);
		var top = (($(window).height() - uiComponent.outerHeight()) / 2) + $(window).scrollTop();
		var left = (($(window).width() - uiComponent.outerWidth()) / 2) + $(window).scrollLeft();
		
		uiComponent.css({
			position : "absolute",
				 top : top + "px",
				left : left + "px"
		});
		return false;
	},

	/**
	 * Populate sessionStorage with rootInstance object data to be used in basic search as context.
	 */
	setRootInstance: function(id, type, title) {
		if (id) {
			var rootInstance = JSON.stringify({
				id		:	id,
				type	:	type,
				title	:	title
			});
			sessionStorage.setItem('emf.rootInstance', rootInstance);
		} else {
			sessionStorage.removeItem('emf.rootInstance');
		}
	},
	
	clearRootInstance: function() {
		sessionStorage.removeItem('emf.rootInstance');
	},

	checkForDeadInstanceLinks: function(container, serviceUrl) {
		if (!serviceUrl) {
			return;
		}

		var typeAndIdPairs = [ ];

		// find all links to instances within the specified container
		$(container).find('a.instance-link').each(function() {
			var link = $(this)
				instanceId = link.attr('data-instance-id'),
				instanceType = link.attr('data-instance-type');

			// add each link's instance-id and instance-type to a array.
			// this array will be sent to a rest service to check which ones are deleted
			if (instanceId && instanceType) {
				typeAndIdPairs.push({
					instanceId	: instanceId,
					instanceType: instanceType
				});
			}
		});

		var ajaxCompleteCallback = function(response) {
			var deleted = JSON.parse(response.responseText);
			if (deleted && deleted.values) {
				var deletedLinks = [ ];

				$.each(deleted.values, function() {
					var pair = this,
						link;
					link = container.find('a.instance-link[data-instance-id="' + pair.instanceId + '"]');
					deletedLinks.push(link);
				});

				$.each(deletedLinks, function() {
					var link = $(this);

					if (link.is('.internal-thumbnail')) {
						var overlay = link.find('.not-allowed-overlay');
						if (!overlay.length) {
							link.append('<span class="not-allowed-overlay"></span>');
						}
					} else {
						link.css({
							textDecoration: 'line-through'
						});
					}

					link.addClass('deleted-instance-link')
						.off('click')
						.on('click', function(event) {
							// prevent bubbling and executing of handlers
							event.preventDefault();
							event.stopImmediatePropagation();
						});
				});
			}
		}

		// If there are links to instance make a request to see which ones are deleted
		if (typeAndIdPairs.length) {
			$.ajax({
				type	: 'POST',
				url		: EMF.servicePath + serviceUrl,
				data	: JSON.stringify(typeAndIdPairs),
				complete: ajaxCompleteCallback
			});
		}
	},

	/**
	 *  Method that manage downloading file by proxy URL. The function will also
	 *  search for preview frame element and will reloaded it.
	 */
	downloadDocumentAfterEvent : function(elementURI){
		if (!elementURI) {
			return;
		}
		var previewFrame = $('#previewFrame');
		var previewURL = null;
		if(previewFrame && previewFrame.length){
			previewURL = previewFrame.attr('src');
		}
		var hiddenIFrameID = 'hiddenDownloader',
		iframe = document.getElementById(hiddenIFrameID);
	    if (iframe === null) {
	        iframe = document.createElement('iframe');
	        iframe.id = hiddenIFrameID;
	        iframe.style.display = 'none';
	        document.body.appendChild(iframe);
	    }
	    iframe.src = elementURI;
	    if(previewURL){
	    	previewFrame.attr('src', previewURL);
	    }
	},

	/**
	 * Escapes the colon ':' signs from provided string and ads a '#' prefix to
	 * make it usable as jQuery selector.
	 *
	 * @param id
	 *            A string that should be escaped and prefixed.
	 * @returns {String}
	 */
	escId : function( id ) {
		return '#' + id.replace(/(:|\.)/g, '\\$1');
	},

	/**
	 * Mimic disabled behaviour for a field.
	 * @param target The field to be disabled.
	 * @returns {Boolean}
	 */
	initDisabledField: function(target) {
    	target.on('focus.cmf', function() {
			$(this).blur();
			return false;
		});
		return false;
	},

	toggleDisabledAttribute : function(selector, shouldDisable) {
		var elem = $(selector);
		var isDisabled = elem.attr('disabled') !== undefined;
		if (isDisabled || shouldDisable == false) {
			elem.removeAttr('disabled');
		} else {
			elem.attr('disabled', 'disabled');
		}
	},

	toggleReadonly : function(selector, shouldDisable) {
		var elem = $(selector);
		var isDisabled = elem.attr('readonly') !== undefined;
		if (isDisabled || shouldDisable == false) {
			elem.removeAttr('readonly');
		} else {
			elem.attr('readonly', 'readonly');
		}
	},

    resetPanel : function(fields, buttons) {
        if(fields) {
            $(fields).val('');
        }
        if(buttons) {
            $(buttons).attr('disabled', 'disabled');
        }
    },

	resetReasonPanel : function(fieldSelector, submitButtonSelector) {
		$(fieldSelector).val('');
		$(submitButtonSelector).attr('disabled', 'disabled');
	},

	resetVersionRevertPanel : function() {
		$('.revert-version-type :radio').eq(0).attr('checked', 'checked');
		$('.revert-description').val('');
	},

	initDynamicFormDropdowns : function() {
		var dynamicFormSelectTags = '.dynamic-form-panel select';
		CMF.utilityFunctions.initDropDown(dynamicFormSelectTags, null, function() {
			// is this needed, after rnc init change locations ?
//			CMF.rnc.init();
		});
	},

	initDropdownsForSearchDialogs : function(){
		var documentMoveSelector = '.search-modal-dialog select';
		var options = {width:'307px'};
		CMF.utilityFunctions.initDropDown(documentMoveSelector, options);
	},

	initDropdownsForRequestHelp : function(){
		var requestHelpSelector = '.request-help-panel select';
		CMF.utilityFunctions.initDropDown(requestHelpSelector);
	},

	/**
	 * Method that will be used like core init for select2 plugin.
	 * Here we can add additional plugin options that will be merge
	 * with the default.
	 *
	 * Attribute <b> options </b> is not required.
	 */
	initDropDown : function(elements, options, callback){

	  var settings = {
			width : function(){
				_width = "110px";
				if(this && this.element){
					var parent = this.element.parent();
					if(parent.hasClass('rm-role-selector-wrapper')) {
						_width = "170px";
					}
				}
				return _width;
			},
			minimumResultsForSearch: EMF.config.comboAutocompleteMinimumItems,
			placeholder : _emfLabels['cmf.dropdown.placeholder'],
			allowClear	: true
		}

		// check for additional settings
		if(options){
			// merge them with the default
			settings = $.extend( {}, settings, options);
		}

		$(elements).select2(settings);
		if (callback) {
			callback();
		}
	},

	/**
     * Initializes date fields in the page if any. Any date range field should be skipped
	 * because they are initialized separately.
     */
	initDateFields : function() {
		var fields = $( CMF.config.dateFieldSelector + ', ' + CMF.config.dateTimeFieldSelector)
                        .filter(function(index) {
                            return !$(this).hasClass('date-range');
                        });
		fields.datetimepicker({
			changeMonth		: true,
			changeYear		: true,
			showButtonPanel : true,
			yearRange		: SF.config.yearRange,
			timeText 		: SF.config.timeText,
			hourText 		: SF.config.hourText,
			minuteText 		: SF.config.minuteText,
			currentText 	: SF.config.currentText,
			closeText 		: SF.config.closeText,
			firstDay 		: SF.config.firstDay,
			dateFormat		: SF.config.dateFormatPattern,
			monthNames 		: SF.config.monthNames,
			monthNamesShort	: SF.config.monthNamesShort,
			dayNames		: SF.config.dayNames,
			dayNamesMin 	: SF.config.dayNamesMin,
			separator 		: ', ',
			// shows timepicker if necessary
			beforeShow : function() {
		    	if ($(this).is(CMF.config.dateTimeFieldSelector)) {
		    		var cashedDateTimeContainer = $('#ui-datepicker-div');
		    		$(this ).datetimepicker( 'option', 'showTimepicker', true );
		    	} else {
		    		$(this ).datetimepicker('option', 'showTimepicker', false );
		    	}
		    },
		    onClose : function(dateText, inst) {
		    	// run the rnc check when datepicker is closed
		    	CMF.rnc.init();
		    }
		});
	},

	initDynamicForm : function(panelClass) {
		var selector = '.dynamic-form-panel';
		if (panelClass) {
			selector = '.' + panelClass;
		}
		if($(selector).children().length > 0) {
			CMF.oncompleteHandlers.initGeneratedForm();
		}

	},

	/**
	 * On search pages if there is a field for content search, then input in that
	 * field should be restricted:
	 * - it is not allowed to have only '*' in the field
	 * - allow search to be performed only if there are more then 2 symbols in the field
	 */
	filterSearchArgumentsData : function() {

		// restrict field search only for specific pages
		if($('.document-search-panel').length > 0) {

			var keywordField = $('.keyword-argument');

			// if there is keyword search field on the page
			if (keywordField.length > 0) {

				keywordField.on('keyup', function() {
					var field = $(this);
					var value = field.val();
					// not striped value length
					var len1 = value.length;
					// remove all '*' and '?' characters
					value = value.replace(/[\*\?]/g,'');
					// striped value length
					var len2 = value.length;
					// striped and not striped value lengths must be greater then 2 characters
					// not striped value can be empty (zero length)
					if ((len1 === 0) || ((len1 > 2) && (len2 > 2))) {
						enableButtons();
					} else {
						disableButtons();
					};
				});
			};
		}

		function disableButtons() {
//			$('.standard-button').attr('disabled', 'disabled');
			var btns = $('.search-page .btn');
			var blocker = $('.search-page .button-blocker');
			if(blocker && blocker.length === 0) {
				btns.each(function() {
					var currentBtn = $(this);
					var overlay = $('<div class="button-blocker"></div>').css({
						'width': currentBtn.outerWidth() + 'px',
						'height': currentBtn.outerHeight() + 'px',
		          		'top' : currentBtn.position().top + 'px',
		                'left' : currentBtn.position().left + 'px',
		                'background-color' : '#888888',
		                'position' : 'absolute',
		                'opacity' : '0.3'
					});
					currentBtn.after(overlay);
				});
			}
		}

		function enableButtons() {
//			$('.standard-button').removeAttr('disabled');
			$('.search-page .btn').next('.button-blocker').remove();
		};
	},

	/**
	 * Initializes a date range fields.
	 */
	initDateRange : function(startDateFieldSelector, endDateFieldSelector) {

		var startDateFields = $(startDateFieldSelector);
		var endDateFields = $(endDateFieldSelector);

		for(var i = 0; i < startDateFields.length; i++) {
			var startDateFieldId = CMF.utilityFunctions.escId(startDateFields[i].id);
			var endDateFieldId = CMF.utilityFunctions.escId(endDateFields[i].id);

			initStartDate(startDateFieldId, endDateFieldId);
			initEndDate(startDateFieldId, endDateFieldId);
		}

		function initStartDate(startDateField, endDateField) {
			$( startDateField ).datetimepicker({
				showTimepicker 		: false,
				showButtonPanel 	: true,
				timeText 			: SF.config.timeText,
				hourText 			: SF.config.hourText,
				minuteText 			: SF.config.minuteText,
				currentText 		: SF.config.currentText,
				closeText 			: SF.config.closeText,
				firstDay 			: SF.config.firstDay,
				changeMonth			: true,
				changeYear 			: true,
				yearRange			: SF.config.yearRange,
				dateFormat			: SF.config.dateFormatPattern,
				monthNames 			: SF.config.monthNames,
				monthNamesShort		: SF.config.monthNamesShort,
				dayNames			: SF.config.dayNames,
				dayNamesMin 		: SF.config.dayNamesMin,
				onSelect: function( selectedDate, pickerInst ) {
					$(endDateField).datetimepicker( 'option', 'minDate', selectedDate );
				},
                // we use on beforeShow to set up the range fields in case there are some initial values in the model
				beforeShow: function( input, pickerInst ) {
                    var startDateInitialValue = $(input).val();
                    if(startDateInitialValue) {
                        $(endDateField).datetimepicker( 'option', 'minDate', startDateInitialValue );
                    }
                    var endDateInitialValue = $(endDateField).val();
                    if(endDateInitialValue) {
                        $(startDateField).datetimepicker( 'option', 'maxDate', endDateInitialValue );
                    }
				},
                onClose : function(dateText, inst) {
                    // run the rnc check when datepicker is closed
                    CMF.rnc.init();
                }
			});
		};

		function initEndDate(startDateField, endDateField) {
			$( endDateField ).datetimepicker({
				showTimepicker 	: false,
				showButtonPanel : true,
				timeText 		: SF.config.timeText,
				hourText 		: SF.config.hourText,
				minuteText 		: SF.config.minuteText,
				firstDay 		: SF.config.firstDay,
				currentText 	: SF.config.currentText,
				closeText 		: SF.config.closeText,
				changeMonth		: true,
				changeYear 		: true,
				yearRange		: SF.config.yearRange,
				dateFormat		: SF.config.dateFormatPattern,
				monthNames 		: SF.config.monthNames,
				monthNamesShort	: SF.config.monthNamesShort,
				dayNames		: SF.config.dayNames,
				dayNamesMin 	: SF.config.dayNamesMin,
				onSelect: function( selectedDate, pickerInst ) {
					$(startDateField).datetimepicker( 'option', 'maxDate', selectedDate );
				},
                // we use on beforeShow to set up the range fields in case there are some initial values in the model
				beforeShow: function( input, pickerInst ) {
                    var endDateInitialValue = $(input).val();
                    if(endDateInitialValue) {
                        $(startDateField).datetimepicker( 'option', 'maxDate', endDateInitialValue );
                    }
                    var startDateInitialValue = $(startDateField).val();
                    if(startDateInitialValue) {
                        $(endDateField).datetimepicker( 'option', 'minDate', startDateInitialValue );
                    }
				},
                onClose : function(dateText, inst) {
                    // run the rnc check when datepicker is closed
                    CMF.rnc.init();
                }
			});
		};
	},

	// This uses richfaces modal panel!!!
	riseConfirmation : function(message, confirmationPanelObj) {
		if (message !== '') {
			confirmationPanelObj.show();
			return false;
		}
		return true;
	},

	/**
	 * Method that hide element or number of elements based of the children.
	 * Accept element or array of element.
	 */
	hideElementsByChildrenLen : function(elements){
		if(!elements){
			return;
		}
		var el = elements;
		var childPattern = ' > *';
		if($.isArray(el)){
			for(var i in el){
				var hasChildren = $(el[i]+childPattern).length;
				if(!hasChildren){
					$(el[i]).hide();
				}
			}
		}else{
			var hasChildren = $(el+childPattern).length;
			if(!hasChildren){
				$(el).hide();
			}
		}
	},

	colleaguesUtils : {

			avatarGenerator : function(colleagueData){

				var wrap = $(colleagueData.wrapper);
				var avatar = '/images/user-icon-32.png';
				var dmsProxy = '/service/dms/proxy/';

				if(colleagueData.avatar){
					avatar = dmsProxy+decodeURIComponent(colleagueData.avatar);
				}

				var path = colleagueData.context+"/"+avatar;
				var avatarIcon = CMF.utilityFunctions.colleaguesUtils.createIcon(path);

				wrap.html(avatarIcon);
			},

			createIcon : function(path, style){
				var defStyle = 'width:32px; height:32px;';
				if(style){
					defStyle = defStyle+style;
				}
				var icon = '<img src="'+path+'" class="header-icon" style="'+defStyle+'" />';
				return icon;
			}
		}

};

/**
 * Oncomplete event handlers.
 */
CMF.oncompleteHandlers = {

	initGeneratedForm : function() {
		$(function() {
			CMF.utilityFunctions.initDateFields();
			CMF.utilityFunctions.initDynamicFormDropdowns();
			CMF.rnc.init();
		});
	},

	// triggered from ajax response from links inside every workflow panel header is activated by user click
	// this expands the panel that belongs to provided trigger element
	workflowHistoryPanelToggleStatus : function(elem, processDiagramPanel) {
		if(elem) {
			var content = $(elem).closest('.workflow-panel').find('.workflow-history-content');
			if (content.length > 0) {
				$(elem).toggleClass('expanded');
				content.slideToggle();
			}
		}

		CMF.oncompleteHandlers.bindClickHandlerToProcessDiagramPanel($(elem).closest('.workflow-panel'), processDiagramPanel);
	},

	//  when workflow tab is opened for the first time, then expand the panel that belongs to provided trigger element
	workflowHistoryPanelToggle: function(elemId, processDiagramPanel) {
		//formId:workflowHistory-*:workflowHistoryDataPanel
		var workflowPanelId = 'formId:workflowHistory-' + elemId + ':workflowHistoryDataPanel';
		workflowPanelId = CMF.utilityFunctions.escId(workflowPanelId);

		var workflowPanel = $(workflowPanelId).closest('.workflow-panel');

		if(workflowPanel.length > 0) {
			workflowPanel.find('.workflow-tab-toggler').toggleClass('expanded').closest('.group-header')
				.siblings('.workflow-history-content').slideToggle();

			CMF.oncompleteHandlers.bindClickHandlerToProcessDiagramPanel(workflowPanel, processDiagramPanel);
		}
	},

	bindClickHandlerToProcessDiagramPanel : function(processDiagram) {
		if(processDiagram && processDiagram.length) {
			processDiagram.on('click', function() {
				var modalPanel = $('<div class=".process-diagram-holder"></div>')
				$(this).clone().appendTo(modalPanel);
				EMF.dialog.popup({
					content	: modalPanel,
					title	: _emfLabels['cmf.view.process.header.label'],
					width	: 'auto',
					height	: 'auto'
				});
			});
		}
	}
};

/**
 * Onchange event handlers.
 */
CMF.onchangeHandlers = {

	selectChange : function(selectElem, elemToChange){
		var select = $(selectElem);
		if($.trim(select.val()) === '') {
			CMF.utilityFunctions.toggleDisabledAttribute(elemToChange, true);
		} else {
			CMF.utilityFunctions.toggleDisabledAttribute(elemToChange, false);
		}
	}

};


/**
 * Handler functions for document create panel.
 */
CMF.fileCreateEventHandlers = {

	/**
	 * Shows the manual title field if the selected document type is the free-style document (default
	 * document type - defaultDocumetType).
	 *
	 * Note: This function is not use for the moment. Waithing to be approved by the BA.
	 */
	showHideManualTitle: function(fileTypeField, manualTitleField, defaultDocumetType) {
		if (fileTypeField.value == "" || fileTypeField.value != defaultDocumetType) {
			$(manualTitleField).parent().hide();
		} else {
			$(manualTitleField).parent().show();
		}
	},

	initFileCreationDialog: function(fileTypeField, manualTitleField, fileNameField, createButton, defaultDocumetType) {
		this.enableOrDisableCreateButton(fileTypeField, fileNameField, createButton);
		$(fileNameField).keyup(function() {
			CMF.fileCreateEventHandlers.enableOrDisableCreateButton(fileTypeField, fileNameField, createButton);
		});
	},

	fileTypeSelectedhandler : function(selectedItem, fileTypeField, manualTitleField, fileNameField,
			createButton, selectedKey, selectedValue, defaultDocumetType) {

		fileTypeField.value = selectedKey;
		//update item selection with the selected value
		$('.selector-activator-link span').text(selectedValue);
		CMF.clickHandlers.menuToggle($(selectedItem));
		this.enableOrDisableCreateButton(fileTypeField, fileNameField, createButton);

		return false;
	},

	/**
	 * Enables/Disables the 'create' button if the required fields (document type and file name)
	 * are filled/not filled.
	 */
	enableOrDisableCreateButton: function(fileTypeField, fileNameField, createButton) {
		if (fileTypeField.value != "" && fileNameField.value != "") {
			$(createButton).removeAttr("disabled");
		} else {
			$(createButton).attr("disabled","disabled");
		}
	}

};

/**
 * Handler functions for task reassign panel.
 */
CMF.taskReassignEventHandlers = {

	openPicklist : function(evt) {
		$('#reassignTaskFieldsWrapper').trigger({
			type : 'openPicklist',
			itemsList : evt.data
		});
	},

	userSelectedHandler : function(selectedItemLink, selectedUserField, selectedKey, selectedValue) {
		selectedUserField.value = selectedKey;
		$('.update-task-button').removeAttr('disabled');
		CMF.clickHandlers.menuToggle($(selectedItemLink));
		$('.selector-activator-link span').text(selectedValue);
		return false;
	}

};
/**
 * Transition selection of user - id of the span should be provided
 */
CMF.taskAssigneesSelectionEventHandlers = {

		openPicklist : function(evt, id) {
			$('#'+id).trigger({
				type : 'openPicklist',
				itemsList : evt.data
			});
		},

		userSelectedHandler : function(selectedItemLink, selectedUserField, selectedKey, selectedValue) {
			selectedUserField.value = selectedKey;
			$('.update-task-button').removeAttr('disabled');
			CMF.clickHandlers.menuToggle($(selectedItemLink));
			$('.selector-activator-link span').text(selectedValue);
			return false;
		}

};

CMF.incomingDocuments = {

	selectDocument : function(triggerLink) {
		var row = $(triggerLink).closest('.pad-1').find('.item-row');
		if(row.hasClass('selected-row')) {
			row.removeClass('selected-row');
		} else {
			row.addClass('selected-row');
		}
	},

	resetPanel : function(listElement) {
		$(listElement).find('.selected-row').removeClass('selected-row');
	}

};

/**
 * Functions used by entity browser.
 */
CMF.entityBrowser = {

	selectSection : function(triggerLink, inCase) {
		var $triggerLink = $(triggerLink);
		if(inCase) {
			$(triggerLink).closest('.case-sections-list').find('.selected-row').removeClass('selected-row')
				.end().end().parent().addClass('selected-row');
		} else {
			var currentItem = $triggerLink.parent();
			var isCurrentSelected = currentItem.hasClass('selected-row');
			if(isCurrentSelected) {
				currentItem.removeClass('selected-row');
			} else {
				$triggerLink.closest('.case-list').find('.selected-row').removeClass('selected-row');
				currentItem.addClass('selected-row');
			}
		}
	},

	selectCase : function(triggerLink) {
		$(triggerLink).closest('.case-list').find('.selected-row').removeClass('selected-row')
			.end().end().parent().addClass('selected-row');
	}

};

/**
 * Changes the disabled attribute of element.
 *
 * @param selector
 * 			The element selector.
 * @param shouldDisable
 * 			Whether to disable or enable the element.
 */
CMF.toggleDisabledAttribute = function(selector, shouldDisable) {
	var elem = $(selector);
	var isDisabled = elem.attr('disabled') !== undefined;
	if (isDisabled || shouldDisable == false) {
		elem.removeAttr('disabled');
	} else {
		elem.attr('disabled', 'disabled');
	}
};

/**
 * Functions that will manage discussion into cases
 * and projects
 */
CMF.discussionActions = {

	/**
	 * Scroll down the list with messages for current case.
	 */
	scrollBottomFromTop : function(){
		CMF.discussionActions.enableDisableMessageButton();
		var element = $('.discussion-body');
		element.animate({ scrollTop: element.find('ul').outerHeight() }, "fast");
	},

	/**
	 * Disable button for adding messages into case discussion zone.
	 */
	enableDisableMessageButton : function(){
		var messageField = $('.comment-field');
		var messageButton = $('.post-comment-button');
		// remove white spaces and validate
		if($.trim(messageField.val())==''){
			// disable button
			messageButton.attr('disabled','disabled');
		}else{
			// enable button
			messageButton.removeAttr('disabled');
		}
	}
};

/**
 * Confirmation handler.
 *
 * @param message
 * 			The message to show in the confirmation window.
 * @param actionName
 * 			The action name to confirm operation for.
 * @returns {Boolean}
 */
CMF.confirm = function(message, actionName) {
	if (message != '') {
		if(actionName) {
			message += '[' + actionName + ']';
		}
		if (! confirm(message)) {
			return false;
		}
	}
	return true;
};



/**
 * Checks if there are facets on the page and if found some, then make it
 * expanded or collapsed according to whether the marker 'expanded' class
 * is set on the header or not.
 */
CMF.initFacetsStatus = function() {
	var facetHeaders = $('.facet-header');
	if(facetHeaders) {
		facetHeaders.each(function() {
			var currentHeader = $(this);
			if(currentHeader.hasClass('expanded')) {
				currentHeader.siblings('.facet-content').show();
				currentHeader.children().show();
			} else {
				currentHeader.siblings('.facet-content').hide();
				currentHeader.children().hide();
			}
		});
	}
};

CMF.trim = function(str) {
    return str.replace(/^\s+|\s+$/g, "");
};

CMF.rnc = {
	init : function() {
		if($('.dynamic-form-panel').length > 0) {
			CMF.RNC = CMF.RNC || {};
			$('.dynamic-form-panel').RNC({
				rnc			: CMF.RNC,
				formId		: 'formId',
				saveButtonId: 'saveButton',
				debugEnabled: CMF.rncDebugMode || EMF.config.debugEnabled
			});
		}

	}
};

/**
 * Configure the object picker for documents attach operation.
 *
 * @param currentInstanceId
 * 			The target instance id to which the documents should be attached
 * @param currentInstanceType
 * 			The target instance type to which the documents should be attached
 */
EMF.search.picker.attachDocument = function(currentInstanceId, currentInstanceType) {
	var pickerConfig = {
			currentInstanceId	: currentInstanceId,
			currentInstanceType	: currentInstanceType,
			defaultObjectTypes	: ['ptop:Document'],
			instanceFilter		: 'documentinstance',
			service				: '/document/',
			pickerType			: 'document', // TODO: do we really need this?
			labels: {
				headerTitle: 'Document picker'
			}
	};
	EMF.search.picker.attach(pickerConfig);
};

/**
 * Configure the object picker for objects attach operation.
 *
 * @param currentInstanceId
 * 			The target instance id to which the objects should be attached
 * @param currentInstanceType
 * 			The target instance type to which the objects should be attached
 */
EMF.search.picker.attachObject = function(currentInstanceId, currentInstanceType) {
	var pickerConfig = {
			defaultsIfEmpty		: true,
			currentInstanceId	: currentInstanceId,
			currentInstanceType	: currentInstanceType,
			instanceFilter		: 'objectinstance',
			service				: '/object-rest/',
			pickerType			: 'object', // TODO: do we really need this?
			labels: {
				headerTitle: 'Object picker'
			}
	};
	
	var searchCfg = EMF.config.search;
	pickerConfig.defaultObjectTypes = searchCfg.defaultSelectedTypes;
	
	if (searchCfg) {
		var filter = searchCfg.availableTypesFilter;
		if (filter && filter.length) {
			var filterLength = filter.length,
				url = EMF.servicePath + '/definition/types?';
			while(filterLength--) {
				url += 'filter=' + filter[filterLength] + '&';
			}
			pickerConfig.objectTypeOptionsLoader = url;
		}
	}
	EMF.search.picker.attach(pickerConfig);
};

/**
 * Attach instance (document, object) action handler.
 * - configure the picker
 * - open picker to allow user to search for documents or objects for attach
 * - user selects document or object
 * - check if document or object already exists in the case
 * - notify the user if object already exists in case and don't proceed with the operation
 * - perform attach operation
 *
 * @param opts
 */
EMF.search.picker.attach = function(opts) {
    var selectedItem;
    var selectedItems 	= {};
    var placeholder 	= $('<div></div>');
    var config			= EMF.search.config();
    config.pickerType  	= opts.pickerType;
    config.popup       	= true;
    config.labels		= {};
    config.labels.popupTitle = opts.labels.headerTitle;
    config.initialSearchArgs = config.initialSearchArgs || {};
    
    config.defaultsIfEmpty = opts.defaultsIfEmpty;
    //default value: ptop:Document
    config.initialSearchArgs.objectType = opts.defaultObjectTypes;

    config.objectTypeOptionsLoader = opts.objectTypeOptionsLoader || { };
    config.browserConfig = {
		allowSelection: true,
		singleSelection: false,
		instanceFilter: opts.instanceFilter
    };
    config.panelVisibility = {
       	upload: false
    };
//    config.searchFieldsConfig = {
//		objectType: { disabled: true }
//	};
    config.dialogConfig = {};
    config.dialogConfig.okBtnClicked = function() {
    	EMF.blockUI.showAjaxLoader();
        var data = {};
        if (selectedItems) {
    		// stringifying whole selectedItems object gives error in Chrome
    		var requestData = buildRequestData(opts.currentInstanceId, opts.currentInstanceType, selectedItems);
			$.ajax({
				type: 'POST',
				url: EMF.servicePath + opts.service,
				contentType: "application/json",
				data: requestData
			})
			.done(function(data) {
				EMF.blockUI.hideAjaxBlocker();
                window.location.href = EMF.bookmarks.buildLink(opts.currentInstanceType, opts.currentInstanceId);
            })
            .fail(function(data, textStatus, jqXHR) {
                EMF.dialog.open({
                    title        : ' ' + jqXHR,
                    message      : data.responseText,
                    notifyOnly   : true,
                    customStyle  : 'warn-message',
                    close: function(event, ui) {
                    	EMF.blockUI.hideAjaxBlocker();
                    },
                    confirm: function() {
                    	EMF.blockUI.hideAjaxBlocker();
                    }
                });
            });
        }
    };

	// decorator to add target attributes to links in object picker
	config.resultItemDecorators = {
		'decorateLinks' : function(element, item) {
			element.find('a').attr('target', '_blank');
			return element;
		}
	};

	config.onBeforeSearch.push(function() {
		EMF.ajaxloader.showLoading(placeholder);
	});
	config.onAfterSearch.push(function() {
		EMF.ajaxloader.hideLoading(placeholder);
	});

    placeholder.on('object-picker.selection-changed', function(event, data) {
		selectedItems = data.selectedItems;
		var isEmpty = true;
		for(key in data.selectedItems) {
			isEmpty = false;
			break;
		}
    	if (isEmpty) {
    		EMF.search.picker.toggleButtons(true);
		} else {
			EMF.search.picker.toggleButtons(false);
		}
    }).on('object.tree.selection', function(data) {
    	selectedItems = data.selectedNodes;
    	if (selectedItems.length === 0) {
    		EMF.search.picker.toggleButtons(true);
		} else {
			EMF.search.picker.toggleButtons(false);
		}
    });

    placeholder.objectPicker(config);
    EMF.search.picker.toggleButtons(true);

	var buildRequestData = function(currentInstanceId, currentInstanceType, selectedItems) {
		var request = {};
		for(key in selectedItems) {
			var item = {};
			item.dbId = selectedItems[key].dbId;
			item.type = selectedItems[key].type;
			request[key] = item;
		}
		return JSON.stringify({
			'currentInstanceId' : opts.currentInstanceId,
			'currentInstanceType': opts.currentInstanceType,
			'selectedItems' : request
		});
	}
};

/**
 * Handler for add primary image operation.
 *
 * - we should check if selected object has primary image already
 * - if there is one, then we should notify the user because its not allowed
 *   to have more than one primary image for object
 * - if there is no primary image associated to the object, then we proceed and
 *   open object picker to allow the user to search for an image
 */
EMF.search.picker.image = function(objectId) {

	var service = EMF.servicePath + '/thumbnail/';

    // check if the object has primary image or not
    $.ajax({
        url: service + objectId,
     // fix IE-11 - caching ajax requests
		cache   : false
    })
    .done(function(data) {
        // proceed with add primary image
        addThumbnail(objectId);
    })
    .fail(function(data, textStatus, jqXHR) {
        // notify for fail and allow user to try again
        EMF.dialog.open({
            title       : '',
            message     : _emfLabels['cmf.imagepicker.thumbnail.change.warn'],
            okBtn       : _emfLabels['cmf.btn.confirm'],
            cancelBtn   : _emfLabels['cmf.btn.no'],
            customStyle : 'warn-message',
            confirm     : function() {
                addThumbnail(objectId);
            }
        });
    });

    // init the image picker
    function addThumbnail(objectId) {
        var selectedItem;
        var placeholder = $('<div></div>');
        var config = EMF.search.config();
        config.pickerType		= 'image';
        config.popup			= true;
        config.labels			= {};
        config.singleSelection	= true;
        config.labels.popupTitle= _emfLabels['cmf.imagepicker.window.title'];
        config.dialogConfig = {};
        config.dialogConfig.okBtnClicked = function() {
            if(selectedItem) {
                var documentId = selectedItem.dbId;
                if (documentId) {
                    EMF.blockUI.showAjaxLoader();
                    var data = {
                        	objectId	: objectId,
                        	documentId	: documentId
                        };
                    $.ajax({
                    	type: 'POST',
                        url	: service,
                        data: JSON.stringify(data)
                    })
                    .done(function(data) {
                        // primary image is added
                        // close picker
                        // reload page
                    	window.location.reload();
                    })
                    .fail(function(data, textStatus, jqXHR) {
                        // notify and proceed
                    	EMF.blockUI.hideAjaxBlocker();
                        EMF.dialog.open({
                            title        : ' ' + jqXHR,
                            message      : data.responseText,
                            notifyOnly   : true,
                            customStyle  : 'warn-message',
                            width		 : 'auto'
                        });
                    });
                }
            }
        };

        config.initialSearchArgs = config.initialSearchArgs || { };
        config.initialSearchArgs.mimetype = '^image/';
        config.searchFieldsConfig = {
        	objectType: { disabled: true }
        };
        config.browserConfig = {
       		allowSelection: true,
        	singleSelection: true,
        	mimetypeFilter: 'image'
        };
        config.panelVisibility = {
           	search: true,
           	browse: true,
           	upload: false
        };

        var selectedItems = [];
        placeholder.on('object-picker.selection-changed', function(event, data) {
            // check if there is only one selected item and enable the OK button
            selectedItems[0] = data.currentSelection;
            selectedItem = data.currentSelection;
            if(selectedItem) {
            	EMF.search.picker.toggleButtons(false);
            }
        }).on('object.tree.selection', function(data) {
        	selectedItems = data.selectedNodes;
        	if (selectedItems.length === 0) {
        		EMF.search.picker.toggleButtons(true);
			} else {
				selectedItem = data.selectedNodes[0];
				EMF.search.picker.toggleButtons(false);
			}
        });

        placeholder.objectPicker(config);

        EMF.search.picker.toggleButtons(true);
    }
};

CMF.resourceAllocation = {
	openPicklist : function(event) {
		$('#resourceAllocationSelectUsersWrapper').trigger({
			type : 'openPicklist',
			itemsList : event.data
		});
	}
};

/**
 * Upload documents in task.
 */
CMF.fileUploadInTask = {
	init: function(currentInstanceId, currentInstanceType, currentPath, event) {
		var picker;
		
		// get document type from outgoing documents table
		var row = (event.source.id).split(':')[2];
		var column = $('#formId\\:outgoingDocuments').find('.link-column');
		var filesFilter = $($(column)[row]).find('.default-type').html();
		
		if (!currentInstanceId && !currentInstanceType && !currentPath) {
			console.log('Missing required arguments for file upload init!');
			return;
		}
		// get the target instance data for objects browser
		// we seek CaseInstance
		var browserRootInstanceId,
			browserRootInstanceType;
		var currentPathArray = currentPath;
		for (var i = 0; i < currentPathArray.length; i++) {
			var current = currentPathArray[i];
			if (current.type === 'caseinstance') {
				browserRootInstanceId = current.id;
				browserRootInstanceType = current.type;
				break;
			}
		}
		if (!browserRootInstanceId && !browserRootInstanceType) {
			console.log('Missing required arguments for file upload init!');
			return;
		}

        var selectedItem;
        var selectedItems	= [];
        var placeholder		= $('<div></div>');
        var config			= EMF.search.config();
        config.popup       	= true;
        config.labels		= {};
        config.labels.popupTitle = _emfLabels['cmf.document.upload.new.document.title.label'];
        config.uploadConfig	= {
        	targetInstanceId	: browserRootInstanceId,
        	targetInstanceType	: browserRootInstanceType,
    		showBrowser			: true,
    		multipleFiles		: false,
    		caseOnly			: true,
    		filesFilter			: filesFilter,
    		browserConfig		: {
    			changeRoot			: false,
    			rootNodeText		: _emfLabels['cmf.root.node.text'], //Case
    			node				: browserRootInstanceId,
    			type				: browserRootInstanceType,
        		allowSelection		: true,
        		singleSelection		: true,
        		allowRootSelection 	: false,
        		instanceFilter 		: 'sectioninstance'
            }
    	}
        config.panelVisibility = {
        	search: false,
        	browse: false,
        	upload: true
        };
        config.defaultTab = 'upload';
        config.dialogConfig = {
        	hideButtons : true
        };

        placeholder.on('file-upload-done', function(evt) {
        	if (evt.uploadedFiles) {
        		var uploadedFiles = evt.uploadedFiles;
                var items = {};
                for(key in uploadedFiles) {
                    var currentItem = uploadedFiles[key];
                    var item = {};
                    item.targetId   = currentInstanceId;
                    item.targetType = currentInstanceType;
                    item.destId     = currentItem.dbId;
                    item.destType   = currentItem.type;
                    items[key] = item;
                }
                var data = {
                	relType         : 'emf:outgoingDocuments',
                	reverseRelType	: 'emf:outgoingDocuments',
                	system			: true,
                	selectedItems   : items
                };
			}

            $.ajax({
                contentType : "application/json",
                data        : JSON.stringify(data),
                type        : 'POST',
                url         : SF.config.contextPath + '/service/relations/create'
            }).done(function(data, textStatus, jqXHR) {
            	$('.update-model-button').click();
            	picker.dialog( "close" );
            }).fail(function(jqXHR, textStatus, errorThrown) {
		        EMF.dialog.notify({
		            title       : 'Error',
		            message     : "Can not link uploaded document to current task!",
		            customStyle : 'warn-message'
		        });
            });
        });

        picker = placeholder.objectPicker(config);
	}
};

/**
 * Initializes the file upload for case section upload.
 */
CMF.fileUploadNew = {

	/**
	 * @param targetInstanceId
	 * 			The identifier of the instance to which the uploaded file should be attached.
	 * @param targetInstanceType
	 * 			The type of the instance to which the uploaded file should be attached.
	 */
	init: function(targetInstanceId, targetInstanceType) {
		if(!targetInstanceId && !targetInstanceType) {
			console.error('Missing required attributes to initiate file upload!');
			return;
		}
        var selectedItem;
        var selectedItems	= [];
        var placeholder		= $('<div></div>');
        var config			= EMF.search.config();
        config.popup       	= true;
        config.labels		= {};
        config.labels.popupTitle = _emfLabels['cmf.document.upload.new.document.title.label'];
        config.uploadConfig	= {
        	targetInstanceId	: targetInstanceId,
        	targetInstanceType	: targetInstanceType,
    		showBrowser			: false,
    		browserConfig		: {
        		allowSelection		: true,
        		singleSelection		: true,
        		allowRootSelection 	: false,
        		instanceFilter 		: 'sectioninstance'
            }
    	}
        config.panelVisibility = {
        	search: false,
        	browse: false,
        	upload: true
        };
        config.defaultTab = 'upload';
        config.dialogConfig = {
        	hideButtons : true
        };

        placeholder.on('file-upload-done', function(evt) {
        	window.location.href =
            	EMF.bookmarks.buildLink(EMF.documentContext.currentInstanceType, EMF.documentContext.currentInstanceId, EMF.bookmarks.caseTab.documents);
        });

        var picker = placeholder.objectPicker(config);
	}
};

CMF.action = {

	confirmation: function(opts) {
		if(!opts.message || !opts.confirmAction) {
			return;
		}
		var title = opts.title || ' ';
		var customStyle = opts.customStyle || 'warn-message';
		var closeAction = opts.closeAction || function(event, ui) {
        	return false;
        };
        EMF.dialog.confirm({
            title       : title,
            message     : opts.message,
            customStyle	: customStyle,
            close		: closeAction,
            confirm		: opts.confirmAction
        });
	},

	operationFail: function(data, textStatus, jqXHR) {
		EMF.dialog.open({
			title        : ' ' + jqXHR,
			message      : data.responseText,
			notifyOnly   : true,
			customStyle  : 'warn-message',
			width		 : 'auto'
		});
	},
	
	/**
	 * Get/check if given task instance has subtasks.
	 */
	completeTask: function(evt, opts) {
		if(EMF.config.debugEnabled) {
			console.log('Get subtasks: ', opts);
		}
		if (!opts.instanceId || !opts.instanceType || !opts.checkonly) {
			console.log('Missing required arguments for get subtasks operation: ', arguments);
		}
		var actionTarget = $(evt.target);
		var service = EMF.servicePath + '/task/subtasks';
		var data = {
			instanceId	: opts.instanceId,
			instanceType: opts.instanceType,
			checkonly	: opts.checkonly
		};
		$.ajax({
			type		: 'GET',
			url			: service,
			data		: data
		})
		.done(function(data, textStatus, jqXHR) {
			if (data && (data.skip || !data.hasSubtasks)) {
				executeTransition();
			} else {
				CMF.action.confirmation({
					message      : opts.confirmationMessage || 'This task has sub-tasks, which are not completed. All uncompleted sub-tasks will be automatically stopped. Are you sure? Y/ N',
					confirmAction: executeTransition
				});
			}
		})
		.fail(CMF.action.operationFail);
		
		function executeTransition() {
			EMF.blockUI.showAjaxLoader();
			actionTarget.next('input').click();
		}
	},
		
	/**
	 * Detach document.
	 */
	detachDocument: function(evt, opts) {
		if (!opts.attachedInstanceId || !opts.attachedInstanceType || !opts.targetId || !opts.targetType) {
	        console.error('Missing required attributes for document detach operation:', arguments);
		}
		// because this function is called onclick, we should prevent the default action and handle submit manually
		if(evt) {
			evt.preventDefault();
		}

		var confirmTitle = _emfLabels['cmf.document.section.remove.document.operation'];
		confirmTitle = opts.confirmation+' '+confirmTitle + ' ?';
		
		CMF.action.confirmation({
			message      : confirmTitle,
			confirmAction: executeDetach
		});
		
		function executeDetach() {
			var service = EMF.servicePath + '/instances/detach';
			var data = {
					targetId	: opts.targetId,
					targetType	: opts.targetType,
					linked: [{
						instanceId	: opts.attachedInstanceId,
						instanceType: opts.attachedInstanceType
					}]
			};

			$.ajax({
				type		: 'POST',
				contentType : "application/json",
				url			: service,
				data		: JSON.stringify(data)
			})
			.done(function(data) {
				window.location.href = EMF.bookmarks.buildLink(opts.targetType, opts.targetId, null);
			})
			.fail(CMF.action.operationFail);
			return true;
		}
		return false;
	},

	deleteDocument: function(evt, opts) {

	},

	/**
	 * - Call service to fetch case with its sections
	 * - Build check table
	 * - Open dialog with the table for the user to pick a target instance section
	 * - On accept, send selected section and instance data to rest service which should move the instance
	 * - After rest call completes, build navigation link and refresh the page
	 *
	 * @param instanceId
	 * @param instanceType
	 * @param targetId
	 * @param targetType
	 */
	moveInstance: function(opts, event) {
		if(EMF.config.debugEnabled) {
			console.log('Move instance arguments: ', opts);
		}
		if (!opts.instanceId || !opts.instanceType || !opts.sourceId || !opts.sourceType) {
			console.log('Missing required arguments for instance move operation: ', arguments);
			return;
		}
		if (opts.sourceType !== 'sectioninstance') {
			console.warn('We doesn\'t support object/document move in context different than section instance for now!');
			return;
		}
		if (opts.instanceType !== 'objectinstance' && opts.instanceType !== 'documentinstance') {
			console.warn('This function [moveInstance] doesn\'t support instances different than objectinstance and documentinstance!');
			return;
		}

		var selectedItem;
		// document sections have no purpose
		var sectionType = '';
		if (opts.instanceType === 'objectinstance') {
			sectionType = 'objectsSection';
		}
		var url = EMF.servicePath + '/caseInstance/sections?id=' + opts.sourceId
				+ '&instanceType=' + opts.sourceType
				+ '&purpose=' + sectionType;
	    $.ajax({
	        url: url
	    })
	    .done(loadSectionsHandler)
	    .fail(failLoadSections);

	    /**
	     * Build case sections html to be displayed inside a picker dialog.
	     */
	    function loadSectionsHandler(data) {
	    	var result = $.parseJSON(data.responseText);
	    	var caseHeader =
	    		'<div class="tree-header default_header"> \
						<div class="instance-header caseinstance"> \
						<span class="icon-cell"> \
							<img class="header-icon" src="' + data.icon + '" width="24"> \
						</span> \
						<span class="data-cell">' + data.header + '</span> \
					</div> \
				 </div>';
	    	var tableHtml = EMF.dom.buildCheckTable({
				singleSelection	: true,
				tableClass		: 'table table-hover section-picker',
				tableId			: 'caseSections',
				data			: data.sections
			});

	    	EMF.dialog.open({
	    		message: caseHeader + tableHtml,
	    		title: 'Section picker',
	    		resizable: true,
	    		width: 'auto',
	    		height: 'auto',
	    		confirm: function() {
	    			moveInstance();
	    		}
	    	});

	    	// disable ok button until user makes a selection
	    	EMF.dialog.toggleButton('okBtn', true);
	    	$('.section-picker input').on('change', function(evt) {
	    		selectedItem = $(evt.target)[0];
	    		if(selectedItem) {
	    			EMF.dialog.toggleButton('okBtn', false);
	    		}
	    	});
	    };

	    /**
	     * Handle instance move.
	     */
	    function moveInstance() {
	    	if (selectedItem.value) {
	    		EMF.blockUI.showAjaxLoader();
	    		var url = EMF.servicePath + '/instances/move';
	    		var data = {
	    			instanceId	: opts.instanceId,
	    			instanceType: opts.instanceType,
	    			sourceId	: opts.sourceId,
	    			sourceType	: opts.sourceType,
	    			targetId	: selectedItem.value,
	    			targetType	: 'sectioninstance'
	    		};
			    $.ajax({
					type		: 'POST',
					contentType : 'application/json',
					data		: JSON.stringify(data),
			        url			: url
			    })
			    .done(instanceMovedHandler)
			    .fail(failInstanceMoveHandler);
	    	}
	    }

	    /**
	     * Object is moved and the page should be reloaded.
	     */
	    function instanceMovedHandler(data) {
    		window.location.reload();
	    }

	    /**
	     * Notify user that selected instance can't be moved.
	     */
	    function failInstanceMoveHandler(data, textStatus, jqXHR) {
	        EMF.dialog.open({
	            title       : ' ' + jqXHR,
	            // TODO: message should be taken from the response
	            message     : "Can not move selected object!",
	            notifyOnly	: true,
	            customStyle : 'warn-message'
	        });
	    }

	    /**
	     * Handle cases where case or sections can not be loaded successfully.
	     */
	    function failLoadSections(data, textStatus, jqXHR) {
	        EMF.dialog.open({
	            title       : ' ' + jqXHR,
	            // TODO: message should be taken from the response
	            message     : "Can not load sections for current case!",
	            customStyle : 'warn-message',
	            notifyOnly	: true
	        });
	    }
	}
};

CMF.init = function(opts) {
	CMF.config = $.extend(true, {}, CMF.config, opts);

	$.proxy(SF.init(), this);

	if(!CMF.config.lockBindingActions){
		CMF.config.lockBindingActions = true;
		// Bind base click handler dispatcher
		$(CMF.config.mainContainerId).on('click', CMF.clickHandlerDispatcher);
	}
	// Bind browser window blocker to links and buttons
	$(CMF.config.mainContainerId).on('click', '.cmf-container a, .cmf-container input[type=submit]', EMF.blockUI.togglePageBlocker);

	// Initialize the tooltips plugin.
	// TODO are we need this plugin ?
	$(CMF.config.mainContainerId).tipTip({
		defaultPosition : 'top'
	});

	CMF.searchPages.init();
	
	CMF.initFacetsStatus();

	CMF.utilityFunctions.filterSearchArgumentsData();

	CMF.utilityFunctions.hideElementsByChildrenLen('.context-data-header .tree-header');

	CMF.verticalSplitter.attachSplitterTo('.content-row');

	Ext.Date.defaultFormat = SF.config.dateExtJSFormatPattern;

};

// Register CMF module
EMF.modules.register('CMF', CMF, CMF.init);