
;(function ( $, window, document, undefined ) {

  var pluginName = 'picklist';

  // defaults
  var defaults = {
    pklWidth          : 800,            // default modal panel width
    pklHeight         : 400,            // default modal panel height
    showFooterButtons : true,           // whether to show ok|cancel button in panel footer
    maskClick         : true,          	// whether to allow overlay mask click to close the panel
    pklMode           : 'single',       // [multy|single|singleImmediate] plugin working mode
    itemType          : 'user',
    headerTitle       : 'Picklist',     // default header title text
    okButtonTitle     : 'Ok',           // default ok button text
    cancelButtonTitle : 'Cancel',       // default cancel button text
    triggerButtonTitle: 'Open picklist',// default trigger button text
    searchButtonTitle : 'Search',		// default search button text
    itemsList         : null,           // json feed that should be used to populate the panel
    clearOnClose      : true,           // whether to reset the picklist when panel is closed
    updatePreviewField: true,			// preview field may not be updated if picker is placed outside of generated form
    									// selected value will be visible only in the picker itself
    submitValueLink	  : null,			// if provided submit value link, then the value will be submitted trough it
    hiddenField		  : null,			// if [submitValueLink] is provided, then hiddenField should be provided too
    									// selected item value will be placed in this field's value attribute and a click will be performed
    									// on the [submitValueLink]
    applicationContext: null,
    imgResourceService: null
  };

  /**
   * Plugin constructor.
   */
  function Picklist( container, options ) {

    this.plugin = container;

    // merge the default options with provided once
    this.options = $.extend( {}, defaults, options) ;

    this._defaults = defaults;

    this._name = pluginName;

    // call init to build the plugin view and handlers
    this.init();
  }

  /**
   * Initialize the plugin.
   */
  Picklist.prototype.init = function () {

    var opts = this.options;

    this.buildPluginContainer(opts);

    this.bindHandlers(opts);
  };

  /**
   * Creates the plugin container DOM.
   */
  Picklist.prototype.buildPluginContainer = function(opts) {

    var picklistHtml =
        '<div class="pkl-wrapper"> \
            <div class="pkl-overlay"></div> \
            <dl class="pkl-selected-items-preview"></dl> \
            <div class="pkl-popup hover-class"> \
                <div class="pkl-header"> \
                    <span class="pkl-header-title"></span> \
                    <span class="pkl-close-button"></span> \
                </div> \
                <div class="pkl-actions-subheader"> \
                    <input type="text" id="filterItems" name="filterItems" value="" class="pkl-filter-field" autocomplete="off" /> \
                    <input type="button" id="searchButton" value="Search" class="search-button standard-button" style="display:none;" /> \
                </div> \
                <div class="pkl-body"> \
                    <div class="pkl-body-panel pkl-left-panel"></div> \
                    <div class="pkl-collect-buttons"> \
                        <span class="add-all-button">&gt;</span> \
                        <span class="remove-all-button">&lt;</span> \
                    </div> \
                    <div class="pkl-body-panel pkl-right-panel"></div> \
                </div> \
                <div class="pkl-footer"> \
                    <div class="pkl-footer-buttons"> \
                        <input type="button" id="okButton" value="" class="pkl-ok-button btn btn-primary standard-button" /> \
                        <input type="button" id="cancelButton" value="" class="pkl-cancel-button btn btn-default standard-button" /> \
                    </div> \
                </div> \
            </div> \
        </div>';

    var picklist = $(picklistHtml).appendTo(this.plugin);

    var popup = opts.popup = picklist.find('.pkl-popup');

    var popupWidth = opts.pklWidth,
        popupHeght = opts.pklHeight,
        marginTop = (popupHeght/2) * -1;
        marginLeft = (popupWidth/2) * -1;

    popup.css({
      'width' : popupWidth + 'px',
      'height' : popupHeght + 'px',
      'marginTop' : marginTop + 'px',
      'marginLeft' : marginLeft + 'px'
    });

    // (header height) + (footer height) + (subheader height) + (body padding) = 100px
    var bodyHeight = (popupHeght - 140) + 'px';
    popup.find('.pkl-body').css({
      'height' : bodyHeight
    });

    if((opts.showFooterButtons === false) || (opts.showFooterButtons === 'false')) {
      popup.find('.pkl-footer-buttons').css({ 'visibility' : 'hidden' });
    }

    popup.find('.pkl-header-title').text(opts.headerTitle).end()
      .find('.pkl-ok-button').val(opts.okButtonTitle).end()
      .find('.pkl-cancel-button').val(opts.cancelButtonTitle);

    var overlay = opts.overlay = picklist.find('.pkl-overlay');
    if((opts.maskClick === true) || (opts.maskClick === 'true')) {
      overlay.addClass('pkl-clickable-mask');
    }

    if(opts.pklMode === 'single' || opts.pklMode === 'singleImmediate') {
    	popup.find('.pkl-collect-buttons').hide();
    }

    // if button value is not set serverside, the apply the default value
    var triggerButton = this.triggerButton = this.plugin.find('.open-picklist');
    if(!triggerButton.val()) {
    	triggerButton.val(opts.triggerButtonTitle);
    }

    /*var searchButton = this.searchButton = popup.find('.search-button');
    if(!searchButton.val()) {
    	searchButton.val(opts.searchButtonTitle);
    }*/

    // display current value if there is one in the hidden field
    var hiddenField = this.plugin.find(':hidden');
    //if(hiddenField.length > 0 && hiddenField.val()) {
    	// preview field is binded to the model and no need to be set here
    	//this.plugin.find('.pkl-selected-items-preview').empty().append(hiddenField.val()).show();
    //}
  };

  /**
   * Binds the event handlers.
   */
  Picklist.prototype.bindHandlers = function(opts) {
	var pluginContainer = this.plugin;
    var triggerButton = this.triggerButton;
    var container = opts.popup;
    var overlay = opts.overlay;
    var filterField = container.find('.pkl-filter-field');

    pluginContainer.on('openPicklist', openPicklist);

    triggerButton.on('click', openPicklist);

    if((opts.maskClick === true) || (opts.maskClick === 'true')) {
      overlay.on('click', closePicklist);
    }

    var getClosePanelTriggersRef = this.getClosePanelTriggers;

    container.on('click', clickEventsDispatcher);

    filterField.on('keyup', function(evt) {
    	var keyCode = $.ui.keyCode;
		switch(evt.keyCode) {
				case keyCode.ENTER:
				case keyCode.NUMPAD_ENTER:
					//console.log('enter');
				default:
					filterList();
					break;
   		}
    });

    // because the preview field is actually an input we should prevent
    var previewField = pluginContainer.find('.picklist-preview-field')[0];
    if (previewField){
	    previewField.onfocus = function() {
	    	this.blur();
	    	return false;
	    };
    }
    /**
     * Finds the event target and executes the appropriate handler function.
     * @param evt The event.
     */
    function clickEventsDispatcher(evt) {

      var target = $(evt.target);
      if(target.is('.item-select')) {
        addToSelected(evt);
      }
      else if(target.is('.item-remove')) {
        removeFromSelected(evt);
      }
      else if(target.is(getClosePanelTriggersRef(opts))) {
        closePicklist(evt);
      }
      else if(target.is('.pkl-ok-button')) {
    	  var selectedItems = getSelectedItems();
    	  updatePreviewField(selectedItems.labels);
    	  updateValueField(selectedItems.values);
    	  closePicklist();
      }
      else if(target.is('.add-all-button')) {
        addAllToSelection(evt);
      }
      else if(target.is('.remove-all-button')) {
        removeAllFromSelection(evt);
      }
    }

    // ---------------------------------
    // event handlers
    // ---------------------------------

    function filterList() {
    	var term = filterField.val().toLowerCase();
    	var filtered = filter(opts.normalizedItemsList, term);
    	populateItemsPanel(filtered);
    };

    function escapeRegex( value ) {
   		return value.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
    };

    function filter(array, term) {
   		return $.grep( array, function(value) {
   			return (value.itemLabel.toLowerCase().indexOf(term) !== -1) || (value.itemValue.toLowerCase().indexOf(term) !== -1);
   		});
    }

	function normalize(items) {
		// assume all items have the right format when the first item is complete
		if ( items.length && items[0].itemLabel && items[0].itemValue ) {
			return items;
		}
		return $.map( items, function(item) {
			if ( typeof item === "string" ) {
				return {
					label: item,
					value: item
				};
			}
			return $.extend({
				label: item.itemLabel || item.itemValue,
				value: item.itemValue || item.itemLabel
			}, item );
		});
	};

    /**
     * Called when an item is selected from the picklist panel. According to
     * plugin mode configuration is performed selection.
     *
     * @param evt The event.
     */
    function addToSelected(evt) {

    	var selectedItemsPanel = container.find('.pkl-right-panel .selected-items');
    	var itemsListPanel = container.find('.pkl-left-panel .pkl-items-list');
    	var selectedItem = $(evt.target).closest('dt');

    	if(opts.pklMode === 'multy') {
    		selectedItem.find('.item-handler').removeClass('item-select').addClass('item-remove');
    		selectedItemsPanel.append(selectedItem);
    	}
    	else if(opts.pklMode === 'single') {
    		selectedItem.find('.item-handler').removeClass('item-select').addClass('item-remove');
    		itemsListPanel.find('.item-handler').removeClass('item-select');
    		selectedItemsPanel.append(selectedItem);
    		filterField.attr('disabled', 'disabled');
    	}
    	else if(opts.pklMode === 'singleImmediate') {
    		// TODO: implement
    	}
    };

    /**
     * Called when an item is selected for to be removed from selected items.
     *
     * @param evt The event.
     */
    function removeFromSelected(evt) {

    	var itemsListPanel = container.find('.pkl-left-panel .pkl-items-list');
    	var deselectedItem = $(evt.target).closest('dt');

    	if(opts.pklMode === 'multy') {
    		deselectedItem.find('.item-handler').removeClass('item-remove').addClass('item-select');
    		itemsListPanel.append(deselectedItem);
    	}
    	else if(opts.pklMode === 'single') {
    		deselectedItem.find('.item-handler').removeClass('item-remove').addClass('item-select');
    		itemsListPanel.append(deselectedItem);
    		itemsListPanel.find('.item-handler').addClass('item-select');
    		filterField.removeAttr('disabled').focus();
    	}
    	else if(opts.pklMode === 'singleImmediate') {
    		// TODO: implement
    	}
    };

    /**
     *
     * @param evt
     */
    function addAllToSelection(evt) {
    	var itemsListPanel = container.find('.pkl-left-panel .pkl-items-list');
    	var selectedItemsPanel = container.find('.pkl-right-panel .selected-items');
    	var allItems = itemsListPanel.find('dt');
    	allItems.appendTo(selectedItemsPanel).find('.item-handler')
        	.removeClass('item-select').addClass('item-remove');
    };

    /**
     *
     * @param evt
     */
    function removeAllFromSelection(evt) {
    	var itemsListPanel = container.find('.pkl-left-panel .pkl-items-list');
    	var selectedItemsPanel = container.find('.pkl-right-panel .selected-items');
    	var allSelectedItems = selectedItemsPanel.find('dt');
    	allSelectedItems.appendTo(itemsListPanel).find('.item-handler')
    		.removeClass('item-remove').addClass('item-select');
    };

    /**
     * Called when the picklist should be  opened.
     *
     * @param evt The event.
     *
     * @returns {Boolean}
     */
    function openPicklist(evt) {
    	// populate the panel with list data
    	var items = opts.itemsList || evt.itemsList;
    	if (typeof items === 'string') {
    		items = $.parseJSON(items);
		}
    	opts.itemsList = items;
    	opts.normalizedItemsList = normalize(items || []);

    	// multiselect mode
    	if(opts.pklMode === 'multy') {
    		populateItemsPanel(items);
    	}
    	// single select immediate operation
    	else if(opts.pklMode === 'singleImmediate') {
    		container.find('.pkl-right-panel').remove();
    		populateItemsPanel(items).addClass('single-list');
    	}
    	// single item can be selected but the tow panels are visible
    	else if(opts.pklMode === 'single') {
    		populateItemsPanel(items);
    	}

    	var selectedItemsListRoot = '<dl class="pkl-items-list selected-items"></dl>';
    	if(container.find('.selected-items').length === 0) {
    		container.find('.pkl-right-panel').append(selectedItemsListRoot);
    	}

    	overlay.show();
    	container.show();
    	filterField.val('').removeAttr('disabled').focus();
    	return false;
    };

    /**
     *
     * @param items
     * @returns created items list elements
     */
    function populateItemsPanel(items) {
    	var resourceService = opts.applicationContext + opts.imgResourceService;
    	var html = '<dl class="pkl-items-list">';
    	for(var item in items) {
    		var currentItem = items[item];
    		html += '<dt>';
    		if (currentItem.iconPath){
    			icon = '<span class="item-icon"><img src="' + resourceService + currentItem.iconPath + '"/></span>';
    		} else {
    			icon = '<span class="item-icon ' + (currentItem.type || opts.itemType) + '-icon"></span>';
    		}
	        html += icon;
	        html += '<span class="item-data">';
	        //html += '<span>' + currentItem.id + '</span>';
	        html += '<span class="item-value">' + currentItem.itemValue + '</span>';
	        html += '<span class="item-label">' + currentItem.itemLabel + '</span>';
	        html += '</span>';
	        html += '<span class="item-handler item-select"></span>';
	        html += '</dt>';
    	}
    	html += '</dl>';
    	return container.find('.pkl-left-panel').empty().append(html);
    };

    /**
     * Called when the picklist should be closed.
     *
     * @returns {Boolean}
     */
    function closePicklist() {

      overlay.hide();
      container.hide();
      if((opts.clearOnClose === true) || (opts.clearOnClose === 'true')) {
        container.find('.pkl-body-panel').empty();
      }
      filterField.val('').removeAttr('disabled');
      var previewField= pluginContainer.find('.picklist-preview-field')[0];
      if (previewField){
    	  previewField.onfocus = function() {
    		  this.blur();
    		  return false;
	  	};
      }
      return false;
    };

    /**
     * Retrieves the selected items and returns object with labels and values.
     */
    function getSelectedItems() {
    	var selectedElements = container.find('.pkl-right-panel .selected-items .item-data');
    	var selectedItems = {
    		values: [],
    		labels: []
    	};
        if(selectedElements.length > 0) {
      	  	var itemLabels = selectedElements.find('.item-label');
			var labelsArray = [];
			itemLabels.each(function(i, item) {
				labelsArray[i] = $(item).text();
	    	});
			selectedItems.labels = labelsArray;

			var itemValues = selectedElements.find('.item-value');
			var valuesArray = [];
			itemValues.each(function(i, item) {
				valuesArray[i] = $(item).text();
	    	});
			selectedItems.values = valuesArray;
        }
        return selectedItems;
    };

    /**
     * Called when the preview field should be updated.
     *
     * @param evt The event.
     */
    function updatePreviewField(labels) {
    	if(opts.updatePreviewField) {
    		var previewField = pluginContainer.find('.picklist-preview-field');
    		var joined = labels.join(',');
    	    previewField.val(joined);
    	}
    	EMF.util.textareaResize('.picklist-preview-field');

    	// we should trigger keyUp in order to allow RNC plugin to be notified for the change
    	if (previewField) {
    		previewField.keyup();

    		// because the preview field is actually an input we should prevent
    		if (previewField && previewField[0]) {
    			previewField[0].onfocus = function() {
    				this.blur();
    				return false;
    			};
    		}
    	}
    };

    // If picker is used from action button we don't have the hidden field where selected from the picker value
    // is stored and that is actually bound to model. In normal use when picklist component is built from form builder
    // we have that filed generated. So, if in the configuration options we have submitValueLink and hiddenField
    // parameters, we know that the picker is used outside of form builder generated form and should use provided link and field.
    // Set the selected by the user value in the hidden field and click the ajax link to apply it to the model
    function updateValueField(values) {
    	if(opts.submitValueLink && opts.hiddenField) {
    		var submitValueLink = opts.submitValueLink.eq(0);
  		  	var hiddenField = opts.hiddenField.eq(0);
  		  	var joined = values.join('\u00B6');
  		  	hiddenField.val(joined);
  		  	submitValueLink.click();
  	  	} else {
  	  		var pickerHiddenField = pluginContainer.find('.picklist-field-wrapper').children('.picklist-hidden-field');
  	  		var joined = values.join('\u00B6');
  	  		pickerHiddenField.val(joined);
  	  	}

//    	} else {
//  	  pluginContainer.find('.pkl-selected-items-preview').slideUp(function() {
//  		  $(this).empty();
//  	  });
//    }
    };
  };

  Picklist.prototype.escId = function( id ) {
	  return '#' + id.replace(/(:|\.)/g, '\\$1');
  },

  /**
   *
   */
  Picklist.prototype.getClosePanelTriggers = function(opts) {

    var triggers = [ '.pkl-close-button' ];
    if(opts.showFooterButtons) {
      triggers.push('.pkl-cancel-button');
    }
    return triggers.join(',');
  };

  /**
   * Checks if the provided container id exists and the container
   * itself is in the DOM.
   */
  Picklist.prototype.checkPluginContainer = function() {

    var containerIsOk = true;
    // if container is not provided then return with error
    if(!this.container || $(this.container).length === 0) {
      console.log('Error! Container id must be provided!');
      containerIsOk = false;
    }
    return containerIsOk;
  };

  /**
   * Extend jQuery with our function. The plugin is instantiated only once (singleton).
   */
  $.fn[pluginName] = function ( options ) {

    var pluginObject = $.data(this, 'plugin_' + pluginName);
    if (!pluginObject) {
      pluginObject = $.data(this, 'plugin_' + pluginName,
        new Picklist( this, options ));
    }
    return pluginObject;
  };

  // Assume we will doesn't have more than one picklist on a page
  /*$.fn[pluginName] = function ( options ) {
    return this.each(function () {
      if (!$.data(this, 'plugin_' + pluginName)) {
        $.data(this, 'plugin_' + pluginName,
          new Picklist( this, options ));
      }
    });
  }*/

})( jQuery, window, document );