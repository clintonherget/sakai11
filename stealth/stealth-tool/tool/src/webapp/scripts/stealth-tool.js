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
  $(".unstealth-multiple-netid").select2({
    placeholder:"NetID",
    minimumInputLength: 2,
  	ajax: {
	  	url: function (params) {
	  		console.log("Search string" + params.term);
				return "http://localhost:8080/direct/stealth-admin/searchNetid/" + params.term;
			},
		  delay: 500,
	    dataType: 'json'
    }
  });
	$(".unstealth-multiple-siteid").select2({
		placeholder:"SiteID",
		minimumInputLength: 10,
		ajax: {
			url: function (params) {
				console.log("Search string" + params.term);
				return "http://localhost:8080/direct/stealth-admin/searchSiteid/" + params.term;
			},
			delay: 500,
			dataType: 'json'
		}
	});
  // action="http://localhost:8080/direct/stealth-admin/searchNetid/"
	$('#search-netid-form').submit(function(event){
		event.preventDefault();
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
		var payload = $('#grant-permission-form').serialize();
		$.post('http://localhost:8080/direct/stealth-admin/handleAddForm/',
		payload,
		function(data,status){
			console.log("Data:" + data);
			console.log("Status: " + status);
			alert('tool permission granted');//showNetidTable(data);
		});
		return false;
		// })
	});

});

function openTab(evt, TabName) {
	var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
	   tabcontent[i].style.display = "none";
	}
	tablinks = document.getElementsByClassName("tablinks");
	for (i = 0; i < tablinks.length; i++) {
	   tablinks[i].className = tablinks[i].className.replace(" active", "");
	}
	document.getElementById(TabName).style.display = "block";
	evt.currentTarget.className += " active";
}
function showTable(jsondata){
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
