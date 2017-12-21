let table = false;

$(document).ready(function(){
	$("#select-term").select2({
		placeholder:"Term",
		ajax: {
			url: function (params) {
				return "http://localhost:8080/direct/stealth-admin/getTerms/";
			},
			delay: 500,
			dataType: 'json'
		}
	});
	$("#select-tool").select2({
	  placeholder:"Tool",
	  ajax: {
			url: function (params) {
				return "http://localhost:8080/direct/stealth-admin/getTools";
			},
			delay: 500,
			dataType: 'json'
		}
	});
  $("#select-multiple-netid").select2({
    placeholder:"NetID",
    minimumInputLength: 2,
  	ajax: {
	  	url: function (params) {
				return "http://localhost:8080/direct/stealth-admin/searchNetid/" + params.term;
			},
		delay: 500,
	    dataType: 'json'
    }
  });
	$("#select-multiple-siteid").select2({
		placeholder:"SiteID",
		minimumInputLength: 5,
		ajax: {
			url: function (params) {
				return "http://localhost:8080/direct/stealth-admin/searchSiteid/" + params.term;
			},
			delay: 500,
			dataType: 'json'
		}
	});
	$("#search-table-netid").select2({
      placeholder:"NetID",
      minimumInputLength: 2,
    	ajax: {
  	  	url: function (params) {
  				return "http://localhost:8080/direct/stealth-admin/searchNetid/" + params.term;
  			},
  		  delay: 500,
  	    dataType: 'json'
      }
    });
  	$("#search-table-siteid").select2({
  		placeholder:"SiteID",
  		minimumInputLength: 5,
  		ajax: {
  			url: function (params) {
  				return "http://localhost:8080/direct/stealth-admin/searchSiteid/" + params.term;
  			},
  			delay: 500,
  			dataType: 'json'
  		}
  	});
  // action="http://localhost:8080/direct/stealth-admin/searchNetid/"
	$('#search-netid-form').submit(function(event){
		event.preventDefault();
		if($('#search-table-netid').select2('data').length === 0){
			alert('NetID is required for search');
			return;
		}
		document.getElementById('display-results').innerHTML="";
		var payload = $('#search-netid-form').serialize();
		// $('#search-permissions-by-netid').click(function(){
		$.post('http://localhost:8080/direct/stealth-admin/getRuleByUser',
		payload,
		function(data,status){
			showTable(data);
		});
		// })
	});
	$('#search-siteid-form').submit(function(event){
		event.preventDefault();
		if($('#search-table-siteid').select2('data').length === 0){
			alert('SiteID is required for search');
			return;
		}
		document.getElementById('display-results').innerHTML="";
		var payload = $('#search-siteid-form').serialize();
		// $('#search-permissions-by-netid').click(function(){
		$.post('http://localhost:8080/direct/stealth-admin/getRuleBySite',
		payload,
		function(data,status){
			showTable(data);
		});
		// })
	});
	$('#grant-permission-form').submit(function(event){
		event.preventDefault();
		if($('#select-multiple-netid').select2('data').length === 0 && $('#select-multiple-siteid').select2('data').length === 0){
			alert('Either NetID or SiteID has to be filled for submission');
			return;
		}else if($('#select-tool').select2('data').length === 0){
			alert('No tool selected');
			return;
		}
		var payload = $('#grant-permission-form').serialize();
		$.post('http://localhost:8080/direct/stealth-admin/handleAddForm/',
		payload,
		function(data,status){
			alert('Tool permission granted');
			$("#select-multiple-netid").val(null).trigger('change');
			$("#select-multiple-siteid").val(null).trigger('change');
			$("#select-term").val(null).trigger('change');
			$("#select-tool").val(null).trigger('change');
		});
		return false;
		// })
	});
	openTab("start");
});

function openTab(tabName) {
	switch(tabName){
		case 'NetID':
			$('#SiteID').hide();
			$('#NetID').show();
			$('#searchsite').removeClass('current');
			$('#searchuser').addClass('current');
			$('#searchuser-link').hide();
			$('#searchuser-link-label').show();
			$('#searchsite-link').show();
			$('#searchsite-link-label').hide();
			break;
		case 'SiteID':
			$('#NetID').hide();
			$('#SiteID').show();
			$('#searchuser').removeClass('current');
			$('#searchsite').addClass('current');
			$('#searchsite-link').hide();
			$('#searchsite-link-label').show();
			$('#searchuser-link').show();
			$('#searchuser-link-label').hide();
			break;
		default:
			$('#NetID').show();
			$('#searchuser').addClass('current');
			$('#searchuser-link').hide();
			$('#searchuser-link-label').show();
			$('#searchsite-link').show();
			$('#searchsite-link-label').hide();
			break;
	}
	$('#display-results').hide();
}

function showTable(jsondata){
	$('#display-results').show();
    table = $('#display-results').DataTable( {
		"destroy": true,
		"data": JSON.parse(jsondata),
        "columns": [
            { title: "Net ID" },
            { title: "Course Title" },
            { title: "Site ID" },
            { title: "Tool Name" }
        ]
    });
}
