var serializationChanged = new Boolean(false);

// stop propagation of keypress on edit_title fields (SAK-19026)
// some get handled by a keyboard navigation function
$(document).ready(function(){
    $('.new_title').keypress(function(e){
        e.stopPropagation();
    });
})


function serialize(s)
{
	//kill the unsaved changes message
	if (navigator.userAgent.toLowerCase().indexOf("safari") != -1 && window != top) {
		top.onbeforeunload = function() { };
	}
	else {
		window.onbeforeunload = function() { };
	}
	
	var order = "";
    $('ul.sortable').children('li').each(function(idx, elm) {
      order += elm.id.split(':')[3] + " ";
    });       
	
	document.getElementById('content::state-init').value = order;
}

function doRemovePage(clickedLink) {
	var name = $(clickedLink).closest(".sortable_item").find(".item_label_box").text();
	var conf = confirm($("#del-message").text() + " " + name + "?");
	var theHref = $(clickedLink).attr('href');

	if (conf == true) {
		$("#call-results").fadeOut('400');
		$("#call-results").load(theHref, function() {
			var status = $(this).find("div#value").text();
			if (status == "pass") {
				var target = $(clickedLink).closest(".sortable_item");
				$(this).fadeIn('400');		
				$(target).slideUp('fast', $(target).remove());
			}
			else if (status == "fail") {
				$(this).fadeIn('400');
			}
			//TODO: refresh available pages, but don't mess up display message
			resetFrame();
	  	});
	}
	return conf;
}

// When we show a page, it is automatically enabled if it was not before
function doShowPage(clickedLink) {
		var theHref = $(clickedLink).attr('href');
		$("#call-results").fadeOut('10');
		$("#call-results").load(theHref, function() {
			var status = $("#call-results").find("#value").text();
			if (status == "pass") {
				$(clickedLink).closest(".item_control_box").find(".item_control.show_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.enable_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.disable_link").show();
				$(clickedLink).closest(".item_control_box").find(".item_control.hide_link").show();
				$(clickedLink).closest(".sortable_item").find(".item-hidden-flag").hide();
				$(clickedLink).closest(".sortable_item").find(".item-locked-flag").hide();
				$("#call-results").fadeIn('400');
			}
			else if (status == "fail") {
				$("#call-results").fadeIn('400');
			}
	  	});
}

// When we hide a page - it has no effect on enable/disable
function doHidePage(clickedLink) {
		var theHref = $(clickedLink).attr('href');
		$("#call-results").fadeOut('10');
		$("#call-results").load(theHref, function() {
			var status = $("#call-results").find("#value").text();
			if (status == "pass") {
				$(clickedLink).closest(".item_control_box").find(".item_control.hide_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.show_link").show();
				$(clickedLink).closest(".sortable_item").find(".item-hidden-flag").show();
				$("#call-results").fadeIn('400');
			}
			else if (status == "fail") {
				$("#call-results").fadeIn('400');
			}
	  	});
}

// When we enable a page, we mark it visible automatically
function doEnablePage(clickedLink) {
		var theHref = $(clickedLink).attr('href');
		$("#call-results").fadeOut('10');
		$("#call-results").load(theHref, function() {
			var status = $("#call-results").find("#value").text();
			if (status == "pass") {
				$(clickedLink).closest(".item_control_box").find(".item_control.enable_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.disable_link").show();
				$(clickedLink).closest(".item_control_box").find(".item_control.show_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.hide_link").show();
				$(clickedLink).closest(".sortable_item").find(".item-locked-flag").hide();
				$(clickedLink).closest(".sortable_item").find(".item-hidden-flag").hide();
				$("#call-results").fadeIn('400');
				$("#call-results").fadeIn('400');
			}
			else if (status == "fail") {
				$("#call-results").fadeIn('400');
			}
	  	});
}

// When we disable a page, it is also not visible
function doDisablePage(clickedLink) {
		var theHref = $(clickedLink).attr('href');
		$("#call-results").fadeOut('10');
		$("#call-results").load(theHref, function() {
			var status = $("#call-results").find("#value").text();
			if (status == "pass") {
				$(clickedLink).closest(".item_control_box").find(".item_control.disable_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.enable_link").show();
				$(clickedLink).closest(".item_control_box").find(".item_control.hide_link").hide();
				$(clickedLink).closest(".item_control_box").find(".item_control.show_link").show();
				$(clickedLink).closest(".sortable_item").find(".item-locked-flag").show();
				$(clickedLink).closest(".sortable_item").find(".item-hidden-flag").show();
				$("#call-results").fadeIn('400');
			}
			else if (status == "fail") {
				$("#call-results").fadeIn('400');
			}
	  	});
}

function doEditPage(clickedLink) {
	var theHref = $(clickedLink).attr('href');
	$("#call-results").load(theHref, function() {
		var status = $("#call-results").find("#value").text();
		if (status == "pass") {
	    	var target = document.getElementById('content::page-row:' + $("#call-results").find("#pageId").text() + ':');
			$("#call-results").fadeIn('500');
					
		}
		else if (status == "fail") {
			$("#call-results").fadeIn('500');
		}
  	});
}

function showAddPage(clickedLink, init) {
	var theHref = $(clickedLink).attr('href');
	if (init) {
		$("#add-control").hide();
		$("#list-label").show();
		$(".tool_list").css("border", "1px solid #ccc");
	}
	$("#add-panel").fadeOut(1, $("#add-panel").load(theHref, function() {
		$("#call-results").fadeOut(200, function() {
			$("#call-results").html($("#add-panel").find("#message").html());
			$("#add-panel").fadeIn(200, $("#call-results").fadeIn(200, resetFrame()));
		});
	}));
}

function showEditPage(clickedLink) {
	li = $(clickedLink).closest(".sortable_item");
	clone = li.clone();
	clone.data('source', li);
	li.hide();
	clone.insertAfter(li);

	clone.find(".item_label_box").hide();
	clone.find(".item_control_box").hide();
	clone.find(".item_edit_box").fadeIn('normal');
	clone.addClass("editable_item");
	clone.unbind();
    clone.find(".item_edit_box :input").select().on("keypress", function(event) {
        if (event.keyCode == 13) {
            event.preventDefault();
            clone.find(".do-save-edit").trigger("click");
            return false;
        }
        return true;
    });
	resetFrame();
}

function doSaveEdit(clickedLink) {
	var theHref = $(clickedLink).attr('href');
	clone = $(clickedLink).closest(".sortable_item");
	newTitle = clone.find(".new_title");
	newConfig = clone.find(".new_config");
	$("#call-results").load(clickedLink + "&newTitle=" + encodeURIComponent(newTitle.val()) + "&newConfig=" + encodeURIComponent(newConfig.val()), function() {

		var status = $("#call-results").find("#value").text();
		if (status == 'pass') {
			li = clone.data("source");
			li.find(".item_label_box").html(newTitle.val());
			li.show();
			clone.remove();
		}
  	});
}

function doCancelEdit(clickedLink) {
	clone = $(clickedLink).closest(".sortable_item");
	clone.data("source").show();
	clone.remove();
}

function checkReset() {
	var reset = confirm($("#reset-message").text());
	if (reset)
		return true;
	else
		return false;
}
				
function sortByTitle() {
    // Do natural sorting
    $('ul.sortable').children('li').sort(function(a, b) {
    	var as = $(a).children('.item_label_box').text();
    	var bs = $(b).children('.item_label_box').text();
        	var a, b, a1, b1, i= 0, n, L,
        	rx=/(\.\d+)|(\d+(\.\d+)?)|([^\d.]+)|(\.\D+)|(\.$)/g;
        	if(as===bs) return 0;
        	a= as.toLowerCase().match(rx);
        	b= bs.toLowerCase().match(rx);
        	L= a.length;
        	while(i<L){
        		if(!b[i]) return 1;
        		a1= a[i],
        		b1= b[i++];
        		if(a1!== b1){
        			n= a1-b1;
        			if(!isNaN(n)) return n;
        			return a1.localeCompare(b1);
        		}
        	}
        	return b[i]? -1:0;
    }).appendTo('ul.sortable');
}
				
function addTool(draggable, manual) {
	if (manual == true) {
		// we got fired via the add link not a drag and drop..
		//  so we need to manually add to the list
		$('#reorder-list').append(draggable);
	}
	$(draggable).attr("style", "");
	//force possitioning so IE displays this right
	$(draggable).position("static");
	$("#call-results").fadeOut('200');
	url = $(draggable).find(".tool_add_url").attr("href");
	oldId = $(draggable).id();
	$(draggable).empty();
	li = $(draggable);
	$("#call-results").load(url, function() {
		$(li).DraggableDestroy();
		$(li).id("content::" + $("#call-results").find("li").id());
		$(li).html($("#call-results").find("li").html());
		$(this).find("li").remove();
		$("#call-results").fadeIn('200', resetFrame());
	});
	return false;
}

// NYU disable edit title for lesson pages
$(document).ready(function(){
  var LESSONS_TOOL_ID = 'sakai-lessonbuildertool';

  function disableLessonsEditMenuItem(li) {
    var $editMenuItem = $('.edit-page-title', li);
    $editMenuItem.attr('href', 'javscript:void(0);');
    $editMenuItem.attr('onclick', 'javscript:void(0);');
    $editMenuItem.addClass('disabled');
    $editMenuItem.attr('title', 'This page title can only be edited via the Lesson tool\'s \'Page Settings\' tab');
  };
  
  $('#page-list').find('.tool-'+LESSONS_TOOL_ID).each(function() {
    disableLessonsEditMenuItem($(this));
  });
});