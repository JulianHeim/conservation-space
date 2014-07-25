(function($) {

    $.fn.upload = function(options) {

    	var plugin = { };
    	
	    var defaults = { 
	    	defaultUploadPath	: "",
	    	maxSize				: 10000000,
	    	targetInstanceId	: null,
	    	targetInstanceType	: null,
	    	showBrowser			: true,
	    	allowSelection		: true,	// if checkboxes should be rendered in front of the nodes to allow selection
	    	multipleFiles		: true, // false if multi upload is disabled 
	    	isNewObject			: false, // true if the upload is used in unsaved iDoc/Object
	    	browserConfig		: {
				container		: 'upload-panel-browser',
				node			: EMF.documentContext.rootInstanceId,
				type			: EMF.documentContext.rootInstanceType,
				currentNodeId	: EMF.documentContext.currentInstanceId,
				currentNodeType	: EMF.documentContext.currentInstanceType,
				contextPath		: SF.config.contextPath,
				rootType		: EMF.documentContext.rootInstanceType,
	    		allowSelection	: true,
	    		singleSelection	: true,
	    		changeRoot		: true,
	    		autoExpand		: true,
	    		mimetypeFilter	: null,
	    		instanceFilter	: null
	    	},
	    	uploadTemplateUrl: EMF.applicationPath + '/upload/upload.tpl.html'
		};
	    
    	plugin.settings = $.extend(true, {}, defaults, options);
    	
    	plugin.template = '';
 
    	// load upload plugin template
    	plugin.loadTemplate = function(placeholder) {
    		$.ajax({
        		type: 'GET',
        		url: plugin.settings.uploadTemplateUrl,
        		complete: function(data) {
        			plugin.template = data.responseText;
        			var data = {
            			template : $(plugin.template),
            			settings : plugin.settings
            		}

                	var template = $(data.template).appendTo(placeholder);
        			var formAction = EMF.servicePath + '/upload/upload';
        			template.find('#fileupload').attr('action', formAction);
                	// init plugins and bind handlers
                	plugin.bindHandlers(placeholder);
        		}
    		});
		};

		// Load all document types allowed for selected section
		plugin.loadDocumentTypes = function(element, documentType, sectionId, sectionOf) {
			$.ajax({
				dataType: "json",
				type: 'GET',
				url: EMF.servicePath + "/upload/" + documentType + "/" + sectionId ,
				async: true,
				complete: (function(element) {
					return function(response) {
						var result = $.parseJSON(response.responseText);

						// if there's no available document types disable upload button
						if(result.length == 0) {
							$('#startlUpload').attr("disabled", true);
						} else {
							$('#startlUpload').removeAttr("disabled");
						}
						
						// section selection is mandatory for file upload
			    		if(plugin.settings.caseOnly && documentType != 'sectioninstance') {
			    			$('#startlUpload').attr('disabled', true);
			    		}
						
						element.find('option').remove();
					    $.each(result || [ ], function(index, option) {
					    	if(option.value && option.label){

					    		var optionElement = $('<option />').attr('value', option.value).html(option.label);
				
					    		// if files filter is set allow only this type to be selected
					    		if(plugin.settings.filesFilter) {
					    			var split = plugin.settings.filesFilter.split('(');
					    			if(split[split.length - 1].slice(0,-1) == option.value) {
					    				optionElement.appendTo(element); 
					    			}
					    		} else {
					    			optionElement.appendTo(element);
					    		}
					    	}
					    });
					    
		    			if(plugin.settings.task && _.indexOf(plugin.settings.browserConfig.instanceFilter, documentType) == -1 || sectionOf == 'objectsSection') {
		    				$('#startlUpload').attr('disabled', true);
		    			} else {
		    				$('#startlUpload').attr('disabled', false);
		    			}
		    			
					    // section selection is mandatory for file upload
			    		if(plugin.settings.filesFilter && plugin.settings.caseOnly && sectionOf == 'objectsSection') {
			    			$('#startlUpload').attr('disabled', true);
			    		}
			    		
					    if(element.find('option').length == 0) {
					    	$('#startlUpload').attr("disabled", true);
					    }
					    
			    		var config = {
			    				width: '170px'
			        	};
			    		element.chosen(config).change(function(event) {
			    			var value = $(event.target).val();
			    		});

			    		element.trigger("chosen:updated");
					}
		    	}(element)) 
			});
		}

		// BIG TODO: Find out WHY original plugin formatSize function do not work!!
		plugin.formatFileSize =  function (bytes) {
            if (typeof bytes !== 'number') {
                return '';
            }
            if (bytes >= 1000000000) {
                return (bytes / 1000000000).toFixed(2) + ' GB';
            }
            if (bytes >= 1000000) {
                return (bytes / 1000000).toFixed(2) + ' MB';
            }
            return (bytes / 1000).toFixed(2) + ' KB';
        }

		// Add object browser to upload panel
		plugin.initBrowser = function() {
			var browserConfig = $.extend(true, {}, plugin.settings.browserConfig, {});
			EMF.CMF.objectsExplorer.init(browserConfig);
			var browser = $('#upload-panel-browser');
			browser.addClass("fade");
			browser.on('object.tree.selection', function(evt) {
				
 	        	var selectedItems = evt.selectedNodes;
	        	if(selectedItems.length > 0) {
	        		var firstItem = selectedItems[0];
	        		var sectionOf = (firstItem.path).split('/')[1];

		        	plugin.loadDocumentTypes($('.docType'), firstItem.nodeType, firstItem.nodeId, sectionOf);
		        	$('#contextPath').val('{ "path": ' + JSON.stringify(selectedItems) + '}');

					$('#instance').val(firstItem.nodeType);
					$('#id').val(firstItem.nodeId); 
					
		    		if(plugin.settings.caseOnly) {
		    			$('#startlUpload').attr('disabled', 'disabled');
		    		}
	        	} else {
	        		$('#contextPath').val('');
	        		
		    		if(plugin.settings.caseOnly  || plugin.settings.task) {
		    			$('#startlUpload').attr('disabled', 'disabled');
		    		}
	        	}
	        });
		}

		plugin.bindHandlers = function(placeholder) {
			$('#docPath').val('{ "path": ' + JSON.stringify(EMF.currentPath) + '}');
			var instanceType;
			var instanceId;
			if(plugin.settings.targetInstanceId && plugin.settings.targetInstanceType) {
				instanceId = plugin.settings.targetInstanceId;
				instanceType = plugin.settings.targetInstanceType;
			} else {
				var contextPath = EMF.currentPath;
				var lastElement = contextPath[contextPath.length - 1];
				instanceType = lastElement.type;
				instanceId = lastElement.id;
			}

			$('#instance').val(instanceType);
			$('#id').val(instanceId);

			var fileQueue = [];
    		$('#fileupload').fileupload({
    			dataType: 'json',
    	        autoUpload: false,
    	        singleFileUploads: false,
    	        maxNumberOfFiles: 1,
				add: function(event, data) {
					
					$('#upload-panel-browser').removeClass("fade"); 
					fileQueue.push(data);
					$.each(data.files, function(index, file) {
						
						// EGOV-4567
						if(!plugin.settings.multipleFiles && index > 0) {
							return false;
						}
						
						if(!file.size || file.size < plugin.settings.maxSize) {
							var $upload = $('<tr class="template-upload">'
									       + '<td>'
									       +     '<span class="preview"></span>'
									       + '</td>' 
									       + '<td style="max-width:520px;">'
									       +     '<p><input class="ui-corner-all ui-textfield placeholder-fix" type="text" id="fileName" style="width:170px" placeholder="' + file.name + '"> '
									       +     '<span class="name" style="width:160px; display:inline-block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; vertical-align: center;">' + file.name + '</span> ' 
									       +     '<select class="docType" id="type" chosen="documentType" data-placeholder="Select type"></select></p>'
									       +	 '<div><input class="ui-corner-all ui-textfield placeholder-fix" type="text" id="description" style="width:510px" placeholder="' + window._emfLabels["upload.document.description"] + '"></div>'
									       + '</td>'
									       + '<td>'
									       +     '<p class="size"></p>'
									       +    	'<div class="fileupload-progress" style="min-width:120px"><div class="progress progress-striped active" ><div class="progress-bar" style="width:0%; font-weight:bold;"></div></div></div>'
									       + '</td>'
									       + '<td>'
									       +       '<button class="btn btn-default cancel">'
									       +             '<i class="glyphicon glyphicon-ban-circle"></i> '
									       +             '<span>Remove</span>'
									       +         '</button>'
									       + '</td>'
									       + '</tr>');
						} else {
							var $upload = $('<tr class="template-upload">' 
								       + '<td>'
								       +     '<span class="preview"></span>'
								       + '</td>' 
								       + '<td style="max-width:520px;">'
								       +     '<span style="color: red;">' + file.name + ' Exceed max allowed size of ' + plugin.formatFileSize(plugin.settings.maxSize) + '</span> ' 
								       + '</td>'
								       + '<td>' 
								       +     '<p class="size"></p>'
								       + '</td>'
								       + '<td>'
								       +       '<button class="btn btn-default cancel">'
								       +             '<i class="glyphicon glyphicon-ban-circle"></i> '
								       +             '<span>Remove</span>'
								       +         '</button>'
								       + '</td>');
						}
					
					$('#table-header').removeClass('hide');
					if($('#startlUpload').attr("disabled") == 'disabled') {
	                    $('#table-files tr').each(function() {
	                    	if($(this).hasClass('template-upload')) {
	                    		$(this).remove();
	                    	}
	                    });
						$('#startlUpload').removeAttr("disabled");
						$('#cancelUpload').removeAttr("disabled");
						$('#progress-all-wrapper').addClass("hide"); 

						$('#progress-all').addClass('progress-striped').addClass('active').find('.progress-bar').removeClass('progress-bar-success').removeClass('progress-bar-danger').css('width', '0%').text('');
					}
				    formData = {
				    		contextPath	: "",
				    		docPath		: "",
				    		instance	: "",
				    		id			: "",
				    		fileName	: "",
		                    type		: "",
		                    description	: "",
		                    itemsCount	: "" 
		                };
					data.context = $upload;
					$("#table-files").append($upload);
					plugin.loadDocumentTypes($('.docType'), instanceType, instanceId, ""); 
					$('#startlUpload').removeClass('hide');
	                $('#cancelUpload').removeClass('hide');
	        		
	                $upload.find('#fileName').val(file.name);
		            $upload.find('.size').append(plugin.formatFileSize(file.size));

		            // if user is selected only one file and this file exceed max allowed size disable upload
		            if(index == 0 && file.size > plugin.settings.maxSize) {
		            	$('#startlUpload').addClass('hide');
		            }
		            
		    		// single file selection
		    		if(!plugin.settings.multipleFiles) {
		    			if($('#table-files tr').length == 2) {
		    				$('#file-upload-input').removeAttr('multiple');
		    				$('.fileinput-button').attr('disabled', 'disabled');
		    			}
		    		}
		             
		            // abort upload when cancel button is clicked
		            var $cancelButton = $upload.find('button.cancel');
		            $cancelButton.html(window._emfLabels["upload.file.remove"]);
		            $cancelButton.off('click');
		            $cancelButton.on('click', function() {
		                $upload.remove();
		               
		                // if all files are removed hide upload option and clear table
		                if($('#table-files tr').length == 1) {
		                	$('#startlUpload').addClass('hide');
				            $('#cancelUpload').addClass('hide');
				            $('#table-header').addClass('hide');
				            $('#upload-panel-browser').addClass("fade");
				            $('.fileinput-button').removeAttr('disabled');
		                }
		                $('#progress-all').addClass('progress-striped').addClass('active').find('.progress-bar').removeClass('progress-bar-success').removeClass('progress-bar-danger');
		                var rows = $("#table-files tr");
		                $.each(rows, function(index, row) {
				    		$(row).find('.progress').addClass('progress-striped').addClass('active').find('.progress-bar').removeClass('progress-bar-success').removeClass('progress-bar-danger');
				    	});
		            });
		            
		            var msieVersion;
		            var safariVersion;
		            if ($.browser.msie ) {
		            	msieVersion = $.browser.version;
		            }
		            
		            if ($.browser.safari && navigator.appVersion.indexOf("Win") != -1) {
		            	safariVersion = $.browser.version;
		            }
		            
		            // image preview can not be shown in IE < 10 and Safari for Windows
		            if(msieVersion != "8.0" && msieVersion != "9.0" && safariVersion != "534.57.2" && $.browser.version != "536.28.10") {
		            	// add thumbnail preview
			            var image = document.createElement('img');
			            image.src = URL.createObjectURL(file);
			            image.width = 80;
	
			            image.onerror = function () {
			            	this.style.display = "none";
			            }
	
			            $upload.find('.preview').append(image);
		            }
		            
					});
					
		            var $uploadAll = $('#startlUpload');
		            $uploadAll.off('click');
		            $uploadAll.on('click',function () {
		            	data.submit().success(function(response) {
		            	});
		             })

	                var $cancelButton = $('#cancelUpload');
	                $("#cancelUpload").on('click',function () {
	                	data.abort();
	    		    	$('#startlUpload').off('click');
	    		        $('#cancelUpload').off("click");
	                	$('#startlUpload').attr("disabled", true);
	                    $('#cancelUpload').attr("disabled", true);
	                    var cancelButtons = data.context.find('button.cancel');
	    		        cancelButtons.off('click');
	    		        cancelButtons.attr("disabled", true);
	    		        var rows = $("#table-files tr");

	    		    	$.each(rows, function(index, row) {
	    		    		if(index > 0) {
	    		    			$(row).remove();
	    		    		}
	    		    	}); 
	    		    	
	    		    	$('#startlUpload').addClass('hide');
			            $('#cancelUpload').addClass('hide');
			            $('#table-header').addClass('hide');
			            $('.fileinput-button').removeAttr('disabled');
			            $('#upload-panel-browser').addClass("fade"); 
	                });

			},

			submit: function(e, data) {
				var fileName = [];
				var type = [];
				var description = [];
			 	var itemsCount = 0; 
  
			    $.concat || $.extend({
			    	concat: function(b,c) {
			    		var a=[];
			    		for(x in arguments) {
			    			a=a.concat(arguments[x]);
			    		}
			    		return a;
			    	}
			    });
			    
			    var concFiles =  data.files;
			    $.each(fileQueue, function(index, file) {
			    	if(index < fileQueue.length - 1) {
			    		concFiles = $.concat(concFiles, file.files);
			    	}
			    });

			    data.files = concFiles;

			    $.each(data.form[0], function(index, field) {
			    	if($(field).get(0).id == 'type') {
			    		type.push($(field).val());
			    		itemsCount++; 
			    	}
			    		
			    	if($(field).get(0).id == 'fileName') {
			    		fileName.push($(field).val() || " ");
			    	}

			    	if($(field).get(0).id == 'description') {
			    		description.push($(field).val() || " ");
			    	}
			    });

			    data.formData = {
			    		contextPath	: $('#contextPath').val(),
			    		docPath		: $('#docPath').val(),
			    		instance	: $('#instance').val(),
			    		id			: $('#id').val(),
	                    type		: type,
	                    fileName	: fileName,
	                    description	: description,
	                    itemsCount	: itemsCount 
	            };
	
			    $('#progress-all-wrapper').removeClass("hide");
			},

		    progress: function(e, data) {
		    	var rows = $("#table-files tr");

		    	var progress = parseInt(data.loaded / data.total * 100, 10);
		    	$.each(rows, function(index, row) {
		    		$(row).find('.progress').find('.progress-bar')
	                .css('width', progress + '%')
	                .text(progress+'%');
		    	});
		    },
		    done: function(e, data) {
		    	var rows = $("#table-files tr");
		    	$.each(rows, function(index, row) {
		    		$(row).find('.progress')
		                .removeClass('active')
		                .removeClass('progress-striped')
		                .addClass('progress')
		                .find('.progress-bar')
		                .addClass('progress-bar-success')
		                .css('width', '100%')
		                .text(window._emfLabels["upload.upload.complete"]);
		    	});
		        $('#progress-all')
		        .removeClass('progress-striped')
                .addClass('progress')
                .find('.progress-bar')
                .addClass('progress-bar-success')
                .css('width', '100%')
                .text(window._emfLabels["upload.upload.complete"]);
		        $('#startlUpload').off('click');
		        $('#cancelUpload').off("click");
		        $('#cancelUpload').attr("disabled", true);
		        var cancelButtons = data.context.find('button.cancel');
		        cancelButtons.off('click');
		        cancelButtons.attr("disabled", true);

		        plugin.fileUploadDone(data.result); 
		        if(plugin.settings.allowSelection) { 
		        	plugin.responseDecorator(data.result); 
		        }
		    },   
		    fail: function(e, data) {
		    	if(e.currentTarget) {
		    		return;
		    	}
		    	var rows = $("#table-files tr"); 
		    	$.each(rows, function(index, row) {
		    		$(row).find('.progress')
	                .removeClass('active')
	                .removeClass('progress-bar-info')
	                .addClass('progress-bar-danger')
	                .find('.progress-bar')
	                .addClass('progress-bar-danger')
	                .css('width', '100%')
	                .text(window._emfLabels["upload.upload.failed"]);
		    	});
		    	
		    	$('#progress-all')
		        .removeClass('progress-striped')
                .addClass('progress')
                .find('.progress-bar')
                .addClass('progress-bar-danger')
                .text(window._emfLabels["upload.upload.failed"]);
		    	
		    	plugin.placeholder.trigger("file-upload-fail");
		    },
		    stop: function(e, data) {

		    }

    		});

    		// single file selection
    		if(!plugin.settings.multipleFiles) {
    			$('#file-upload-input').removeAttr('multiple');
    		}
    		
    		// show project browser
    		if(plugin.settings.showBrowser) {

    			if(_.indexOf(["sectioninstance", "objectinstance", "documentinstance"], instanceType) == -1) {
    				plugin.settings.browserConfig.instanceFilter = ["sectioninstance"];
    				plugin.settings.browserConfig.changeRoot = false;
    				plugin.settings.browserConfig.allowRootSelection = false;
    				plugin.settings.task = true;
    				
    				var contextPath = EMF.currentPath;
    				if(instanceType == 'caseinstance') {
    					var root = contextPath[contextPath.length - 1];
    				} else {
    					var root = contextPath[1];
    				}
    				
    				if(root && root.type == 'caseinstance') {
    					plugin.settings.browserConfig.rootNodeText = _emfLabels['cmf.root.node.text']; //Case
    					plugin.settings.browserConfig.node = root.id;
    					plugin.settings.browserConfig.type = root.type;
    				}
    				
    				if(contextPath.length == 1) {
    					root = contextPath[0];
    					plugin.settings.browserConfig.rootNodeText = root.compactHeader;
    					plugin.settings.browserConfig.node = root.id;
    					plugin.settings.browserConfig.type = root.type;
    				}
    			}
    			plugin.initBrowser();
    		}
    		
    		plugin.translateLabels();
    		
    	    if(plugin.settings.isNewObject) {
    	    	$('.upload-wrapper').empty();
    	    	$('.upload-wrapper').append("<div class='upload-error-message'>" + window._emfLabels["upload.error.messge"] + "</div>");
    	    }
		};
		
		// Translate labels using properties 
		plugin.translateLabels = function() {
    		$("#startlUpload").html(window._emfLabels["upload.start.upload"]);
    		$("#cancelUpload").html(window._emfLabels["upload.cancel.upload"]);
    		$("#addFiles").html(window._emfLabels["upload.add.files"]);
    		$("#documentTitle").html(window._emfLabels["upload.document.title"]);
    		$("#fileName").html(window._emfLabels["upload.file.name"]);
    		$("#documentType").html(window._emfLabels["upload.document.type"]);
    		$("#fileSize").html(window._emfLabels["upload.file.size"]);
		}
		
		plugin.fileUploadDone = function(data) {
			uploadedFiles = []; 
			
			// collect data
			$.each(data, function(index, item) {
				uploadedFiles.push(item);
			});
			
			// fire an event with the collected data
			plugin.placeholder.trigger({
				type: 'file-upload-done', 
				uploadedFiles: uploadedFiles
			});
			
			// this event is fired bacause issue - CMF-5569
			$('.idoc-editor').trigger({
				type: 'file-upload-done', 
				uploadedFiles: uploadedFiles
			});
		}
		
		plugin.responseDecorator = function(data) {
			$.each(data, function(index, item) {
				if(index == 0) {
					var rows = $("#table-files tr").eq(index);
					$firstCell = $('th:first-child', rows);
					var th = $('<th></th>');
		    		th.insertBefore($firstCell); 
				} 
				var rows = $("#table-files tr").eq(index + 1);
				$firstCell = $('td:first-child', rows);
				
				var tdCheckbox = $('<td></td>');
				if(plugin.settings.singleSelection) {
					var checkbox = $('<input type="radio" name="picker-radio-group" value="'+ item.dbId +'" />');
				} else {
					var checkbox = $('<input type="checkbox" value="'+ item.dbId +'" />');
				}
				checkbox.data('data', item);

				checkbox.change(function(event) {
					
					var input = $(event.target);
					var data = $(this).data('data');
					data.op = input.prop('checked') ? 'add' : 'remove';
					data.value = input.val();
					data.srcElement = input;

				    var selectionData = {
				    	currentSelection : data
				    };
				 
					$(this).trigger('uploaded.file.selection', selectionData);
				});
				
				checkbox.appendTo(tdCheckbox);
	    		tdCheckbox.insertBefore($firstCell); 
			});
		};

    	// build the UI
    	return this.each(function(index) {
			// make sure only one instance of the plugin is initialized for element
    	    var initialized = $.data(this, 'plugin_upload' );
    	    if (!initialized) {
    	    	$.data(this, 'plugin_upload', true);
    	    } else {
    	    	return;
    	    }
    	     
    		var placeholder = plugin.placeholder = $(this);

    		// Load the upload template, parse it and append it to the placeholder
    		if (plugin.settings.uploadTemplateUrl) {
    			plugin.loadTemplate(placeholder);
    		} else {
    			// if template is loaded already, we just bind the handlers
    			plugin.bindHandlers(placeholder);
    		}

    		return this;
    	});
    }

}(jQuery));