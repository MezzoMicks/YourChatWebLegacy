	    var editMode = false;
	    var inheritsize = false;

	    function editAll(editable, transfer) {
	        if (editable == true || editable == false) {
	            var edElements = $('.editable');
	            $.each(edElements, function() {
	                edit(this, editable, transfer);
	            });
	        }
	        editMode = editable;
	    }

	    function edit(element, editable, transfer) {
	    	// get the view-element
	        var viewer = $(element).find('.editable-view');
	    	// get the editor-element
	        var editor = $(element).find('.editable-editor');

	        var vieweableVisibles = $(element).find('.editable-isviewing');
	        var editableVisibles = $(element).find('.editable-isediting');
	    	// switch to editable?
	        if (editable) {
	        	vieweableVisibles.css('visibility', 'hidden');
	        	editableVisibles.css('visibility', 'visible');
	        	// hide viewer, show editor
	            viewer.css('visibility', 'hidden');
	            editor.css('visibility', 'visible');
	            // inherit?
	            if (inheritsize) {
	            	// sizes!
	            	editor.css('height', viewer.css('height'));
	            	editor.css('width', viewer.css('width'));
	            }
	            // switch positioning
	            viewer.css('position', 'absolute');
	            viewer.css('top', '0');
	            viewer.css('right', '0');
	            editor.css('position', 'relative');
	            // transfer data?
	            if (transfer) {
	            	// do the trigger-Method first
	                var result = {value: null, done: false};
	                $(element).trigger('edit', result);
	                // if proceeding is wished...
	                if (!result.done) {
	                	// read value from trigger
	                	var value = result.value;
	                	// if there's none
	                    if (value == null) {
	                    	// look for an alternate data-element
	                        var realData = viewer.find('.editable-data');
	                        if (realData.length) {
	                            viewer = realData;
	                        }
                            if (viewer.is('img')) {
                                value = viewer.attr('src');
                            } else {
                                // get the value
                                value = viewer.html();
                            }
	                    }
	                	// write to the editor
	                    var realEditor = editor.find('.editable-data');
                        if (realEditor.length) {
                            editor = realEditor;
                        }
	                    if (editor.is('textarea')) {
	                        value = br2nl(value);
	                    }
                        if (editor.is('input[type="radio"]')) {
                            $('input[name="' + editor.attr('name') + '"][value="' + value + '"]').attr('checked','checked');
                        } else {
	                        editor.val(value);
                        }
	                }
	           } 
	        } else {
	        	vieweableVisibles.css('visibility', 'visible');
	        	editableVisibles.css('visibility', 'hidden');
	            viewer.css('visibility', 'visible');
	            viewer.css('position', 'relative');
	            editor.css('visibility', 'hidden');
	            editor.css('position', 'absolute');
	            editor.css('top', '0');
	            editor.css('right', '0');
	            if (transfer) {
	                var result = {
	                    value: null,
	                    done: false
	                };
	                $(element).trigger('view', result);
	                if (!result.done) {
	                    var value = result.value;
	                    if (value == null) {
	                        var realEditor = editor.find('.editable-data');
	                        if (realEditor.length) {
	                            editor = realEditor;
	                        }
	                        if (editor.is('textarea')) {
	                            value = nl2br(editor.val());
	                        } else if (editor.is('input[type="radio"]')) {
                               value = $('input[name="' + editor.attr('name') + '"]:checked').val();
                            } else {
                               value = editor.val();
                            }
	                    }
	                    if (viewer.is('img')) {
	                        viewer.attr('src', value);
	                    } else {
	                        viewer.html(value);
	                    }
	                }
	            }
	        }
	    }

	    function nl2br(str, is_xhtml) {
	        var breakTag = (is_xhtml || typeof is_xhtml === 'undefined') ? '<br />' : '<br>';
	        return (str + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + breakTag);
	    }

	    function br2nl(str) {
	        return (str + '').replace(/<br\s*\/?>/mg, "\n");
	    }

	    $(document).ready(function() {
	        editAll(false);
	    });