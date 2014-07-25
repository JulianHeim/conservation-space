/* OBJ namespace */
var OBJ = OBJ || {};

/**
 * OBJ configuration properties.
 */
OBJ.config = {

};

/**
 * Creates and opens picker dialog to select objects section for clone
 * object functionality.
 * 
 * @param action 
 * 			The action id that triggers this function.
 * @param objectId 
 * 			The object id
 * @param sectionId 
 * 			The section id to which current object is attached (owning instance id)
 * @param type 
 * 			owning instance type
 * @param contextInstanceId 
 * 			The context instance id (if object is attached to a case section, then the case instance is the context) 
 */
OBJ.objectsSectionPicker = function(action, objectId, sectionId, type, contextInstanceId) {
	if(EMF.config.debugEnabled) {
		console.log('OBJ.objectsSectionPicker arguments: ', arguments);
	}
	// Remove this element to reset the search
	$('.basic-search-wrapper').remove();

	var _placeholder = $('<div></div>');
	_placeholder.off('basic-search:on-search-action').on('basic-search:on-search-action', function() {
		EMF.ajaxloader.showLoading(_placeholder);
	})
	.off('basic-search:after-search-action').on('basic-search:after-search-action', function() {
		EMF.ajaxloader.hideLoading(_placeholder);
	});

	var config = EMF.search.config();
	config.pickerType = 'documentsSection';
	config.objectTypeOptionsLoader = function() {
		var data = [ { "name": "http://ittruse.ittbg.com/ontology/enterpriseManagementFramework#Case", "title": "Case" } ];
		return data;
	};
	config.initialSearchArgs.objectType = ['http://ittruse.ittbg.com/ontology/enterpriseManagementFramework#Case'];
	// FIXME: This assumes that the context instance is a CaseInstance that for objects can be wrong assumption.
	// An object can be attached to case section, but also to a project or to not have a context at all (object library).
	if (contextInstanceId) {
		// if we should set the actual context instance id, use this line
//		 config.initialSearchArgs.location = [contextInstanceId];
		config.initialSearchArgs.location = ['emf-search-context-current-case'];
	}
	
	var sectionsServicePath; 
	// if clone is not for object show all section that can contain documents, else show only sections
	// that can contain objects.
	if (type === "documentinstance" ) {
		sectionsServicePath = "/intelligent-document/documentsSections";
	} else {
		sectionsServicePath = "/object-rest/objectsSections";
	}
			
	config.resultItemDecorators = {};
	config.resultItemDecorators = [ function(element, item) {
		var table = $('<table class="max-width-style"></table>');
		var sectionRow = $('<tr class=""></tr>');
		var tdTogler = $('<td></td>');
		var escapedDbId = item.dbId ? item.dbId.replace(':', '') : '';
		var togler = $('<div class="standalone-togler" id="toggler_case_' + escapedDbId + '"></div>');
		togler.data('data', item);
		togler.click(function(event, _sectionId, _checked) {
			if (!togler.loaded) {
				EMF.ajaxloader.showLoading(_placeholder);
				$.ajax({
					url: EMF.servicePath + sectionsServicePath, 
					data: {'caseId' : item.dbId},
					complete: function(data) {
						var result = $.parseJSON(data.responseText);
						if (result.values) {
							var len = result.values.length;
							for (var i = 0; i < len; i++) {
								var resultItem = result.values[i];
								// Used to automatically select current section
								var checkedValue = _checked ? (resultItem.dbId === _sectionId ? 'checked' : '') : '';
								var imagePath = EMF.util.objectIconPath(resultItem.type, 24);
								var content = 
									'<tr class="case_' + escapedDbId + '"> \
										<td></td> \
										<td> \
											<div class="tree-header default_header" > \
												<div class="instance-header ' + resultItem.type + '"> \
													<span class="icon-cell"> \
														<input type="checkbox" class="clone-section-instance" value="' + resultItem.dbId + '" caseId="' + item.dbId + '" ' + checkedValue + '/> \
													</span> \
													<span class="icon-cell"> \
														<img class="header-icon" src="' + imagePath + '" \> \
													</span> \
													<span class="data-cell">' + resultItem.default_header + '</span> \
												</div> \
											</div> \
										</td> \
									</tr>';
								$(content).appendTo(table);
							}
						}
						
						$('input.clone-section-instance:checkbox').change(function() {
							// Deselect all checkboxes and select current one if it was checked
							if ($(this).is(':checked')) {
								$('input.clone-section-instance:checkbox:checked').attr("checked", false);
								$(this).attr("checked", true);
							}
						});
						EMF.ajaxloader.hideLoading(_placeholder);
					}
				});
				togler.loaded = true;
				togler.addClass('expanded');
			} else {
				// Already loaded. Just trigger hide/show
				var rowClass = '.case_' + escapedDbId;

				if (togler.hasClass('expanded')) {
					togler.removeClass('expanded');
					$(rowClass).hide();
				} else {
					togler.addClass('expanded');
					$(rowClass).show();
				}
			}
		});
		
		// Fix for CMF-6458
		if(item.type == "caseinstance") {
			togler.appendTo(tdTogler);
		}
		tdTogler.appendTo(sectionRow);

		var tdElement = $('<td></td>');
		element.appendTo(tdElement);
		tdElement.appendTo(sectionRow);
		
		table.append(sectionRow);

		return table;
	} ];
	_placeholder.basicSearch(config);
	
	$.ajax({
		type: 'GET', 
		url: EMF.servicePath + '/object-rest/caseAndSection', 
		data: {'sectionId' : sectionId},
		async: true,
		complete: function(data) {
			var result = $.parseJSON(data.responseText);
			if(result.caseData) {
				_placeholder.trigger('basic-search:after-search', {
					values		: [result.caseData],
					resultSize	: 1 
				});
				var escapedDbId = result.caseData.dbId ? result.caseData.dbId.replace(':', '') : '';
				$('#toggler_case_' + escapedDbId).trigger('click', [sectionId, true]);
			} else {
				_placeholder.trigger('basic-search:after-search', {
					resultSize	: 0 
				});
			}

		}
	});
	
	_placeholder.dialog({
		width: 850,
		height: 600,
		resizable: false,
		title : 'Select target section',
		dialogClass: 'notifyme',
		modal: true,
        open: function( event, ui ) {
            $(event.target).parent().removeClass('ui-corner-all').find('.ui-corner-all').removeClass('ui-corner-all');
        },
		close: function(event, ui)
        {
            $(this).dialog('destroy').remove();
        },
        buttons: [
        	{
        	    text     : "Clone",
        	    priority : 'primary',
        	    click    : function() {
                    var selectedSection = $('input.clone-section-instance:checkbox:checked');
                    window.location.href = '/emf/object/clone-object.jsf?id=' + objectId + '&type=' + type + 
                        ((selectedSection && selectedSection.length > 0)?('&sectionId=' + selectedSection.attr('value')):'');
                } 
        	},
        	{
        	    text     : window._emfLabels["cancel"],
        	    priority : 'secondary',
        	    click    : function() {
                    $(this).dialog('destroy').remove();
                } 
        	}
        ]
	});
};

/**
 * Extend picker with pick object functionality. Basically this is initializer
 * for object picker plugin.
 */
EMF.search.picker.object = function(placeholder) {
	var config = EMF.search.config();
	
	config.pickerType = 'object';
	
	var selectedItems = EMF.search.selectedItems = {};
	
	var _placeholder = $(placeholder);
	_placeholder.off('basic-search:on-search-action').on('basic-search:on-search-action', function() {
		EMF.ajaxloader.showLoading(_placeholder);
	})
	.off('basic-search:after-search-action').on('basic-search:after-search-action', function() {
		EMF.ajaxloader.hideLoading(_placeholder);
	});
		
	config.objectTypeOptionsLoader = function() {
		var data = {
				values : [ {"name":"http://ittruse.ittbg.com/ontology/enterpriseManagementFramework#DomainObject","title":"Cultural Object"} ]
			}
		return data;
	};
		
	config.defaultCheckedObjectTypes = ['http://ittruse.ittbg.com/ontology/enterpriseManagementFramework#DomainObject'];
	
	// decorator to add target attributes to links in object picker
	config.resultItemDecorators = { 
		'decorateLinks' : function(element, item) {
			element.find('a').attr('target', '_blank');
			return element;
		}
	}
	
	_placeholder.objectPicker(config);
};
	
/**
 * For object picker, we remove the multiple attribute of the objectType selector.
 * 
 * @param data Holds the basic search template and the plugin settings map.
 */
EMF.search.picker.updateSearchTemplate = function(data) {
	if(data.settings.pickerType === 'object') {
		$(data.template).find('.object-type').removeAttr('multiple');
	}
};


/**
 * Init function for objects module.
 */
OBJ.init = function(opts) {
	OBJ.config = $.extend(true, {}, OBJ.config, opts);
};

// Register PM module
EMF.modules.register('OBJ', OBJ, OBJ.init);