
document['getElementsByRegex'] = function(pattern){
	var arrElements = [];   // to accumulate matching elements
	   var re = new RegExp(pattern);   // the regex to match with
	
	   function findRecursively(aNode) { // recursive function to traverse DOM
	      if (!aNode) 
	          return;
	      if (aNode.id !== undefined && aNode.id.search(re) != -1)
	          arrElements.push(aNode);  // FOUND ONE!
	      for (var idx in aNode.childNodes) // search children...
	          findRecursively(aNode.childNodes[idx]);
	  };

		findRecursively(document); // initiate recursive matching
		return arrElements; // return matching elements
	};

	function ToggleDivDisplay(hideDivs,displayDivs,divToShowId,device){
		var deviceClick=device;
		if(deviceClick=="OnDevice"){
			 var hideRegisterDivs = document.getElementsByRegex('^Roster:rosterData:.*?:divRegister');
	         var displayLnkDivs= document.getElementsByRegex('^Roster:rosterData:.*?:divLnk');
			 for ( var i = 0; hideRegisterDivs[i]; i++ ) {
					 hideRegisterDivs[i].style.display='none';
				}
			 for ( var i = 0; displayLnkDivs[i]; i++ ) {
					 displayLnkDivs[i].style.display='block';
				}
		}
		else{
			var hideUpdateDivs = document.getElementsByRegex('^Roster:rosterData:.*?:divUpdate');
	        var displayDeviceDivs= document.getElementsByRegex('^Roster:rosterData:.*?:divDevice');
			 for ( var i = 0; hideUpdateDivs[i]; i++ ) {
					 hideUpdateDivs[i].style.display='none';
				}
			 for ( var i = 0; displayDeviceDivs[i]; i++ ) {
					 displayDeviceDivs[i].style.display='block';
				}
		}
		for ( var i = 0; hideDivs[i]; i++ ) {
				 if(hideDivs[i].id==divToShowId){}
				 else{
					 hideDivs[i].style.display='none';
					 }
			  }
			   for ( var i = 0; displayDivs[i]; i++ ) {
				 if(displayDivs[i].id==divToShowId){}
				 else{
					 displayDivs[i].style.display='block';
					 }
		}
	}
	
function openRWDiv() {
	var rwTmpDiv = 'responseWareDiv' + "__hide_division_";
	var rwTmpImg = 'responseWareDiv' + "__img_hide_division_";
	var rwDivisionNo = getTheElement(rwTmpDiv);
	var rwImgNo = getTheElement(rwTmpImg);
	if (rwDivisionNo) {
		rwDivisionNo.style.display="block";
		if(rwImgNo) {
			rwImgNo.src = downArrowImageSrc;
		}
	}                   
}

function closeRWDiv() {
	var rwTmpDiv = 'responseWareDiv' + "__hide_division_";
	var rwTmpImg = 'responseWareDiv' + "__img_hide_division_";
	var rwDivisionNo = getTheElement(rwTmpDiv);
	var rwImgNo = getTheElement(rwTmpImg);
	if (rwDivisionNo) {
		rwDivisionNo.style.display="none";
		if(rwImgNo) {
			rwImgNo.src = rightArrowImageSrc;
		}
	}                   
}

function openRCDiv() {
	var rcTmpDiv = 'responseCardDiv' + "__hide_division_";
	var rcTmpImg = 'responseCardDiv' + "__img_hide_division_";
	var rcDivisionNo = getTheElement(rcTmpDiv);
	var rcImgNo = getTheElement(rcTmpImg);
	if (rcDivisionNo) {
		rcDivisionNo.style.display="block";
		if(rcImgNo) {
			rcImgNo.src = downArrowImageSrc;
		}
	}                   
}

function closeRCDiv() {
	var rcTmpDiv = 'responseCardDiv' + "__hide_division_";
	var rcTmpImg = 'responseCardDiv' + "__img_hide_division_";
	var rcDivisionNo = getTheElement(rcTmpDiv);
	var rcImgNo = getTheElement(rcTmpImg);
	if (rcDivisionNo) {
		rcDivisionNo.style.display="none";
		if(rcImgNo) {
			rcImgNo.src = rightArrowImageSrc;
		}
	}                   
}

function getTheElement(thisid) {
	var thiselm = null;

	if (document.getElementById) {
		// browser implements part of W3C DOM HTML ( Gecko, Internet Explorer 5+, Opera 5+
		thiselm = document.getElementById(thisid);
	} else if (document.all) {
		// Internet Explorer 4 or Opera with IE user agent
		thiselm = document.all[thisid];
	} else if (document.layers) {
		// Navigator 4
		thiselm = document.layers[thisid];
	}
	if(thiselm) {
		if(thiselm == null) {
			return;
		} else {
			return thiselm;
		}
	}
}

var alertText = '<h:outputText value="#{msgs.popup_no_device_id}"/>';
var popupCookieName = '<h:outputText value="#{TurningToolBean.popupCookieName}"/>'
function showPopup() {
    popupShown = getCookie(popupCookieName);
    if ("true" != popupShown) {
 	   setCookie(popupCookieName, "true", 1);
        alert(alertText);
    }
}

function setAlertText(t) {
    alertText = t;
}

function setCookie(cookieName, cookieValue, expiredays)
{
	   var exdate=new Date();
	   exdate.setDate(exdate.getDate() + expiredays);
    document.cookie = cookieName + "=" + escape(cookieValue)+
                     ((expiredays==null) ? "" : ";expires=" + exdate.toGMTString());
}

function getCookie(cookieName) {
    if (document.cookie.length > 0) {
 	   cookieStart=document.cookie.indexOf(cookieName + "=");
 	   if (cookieStart != -1) {
 		   cookieStart = cookieStart + cookieName.length + 1;
 		   cookieEnd = document.cookie.indexOf(";", cookieStart);
 		   if (cookieEnd == -1)
 			   cookieEnd = document.cookie.length;
 		   return unescape(document.cookie.substring(cookieStart, cookieEnd));
 	   }
    }
    return "";
}
//<h:outputText value="showPopup();" rendered="#{TurningToolBean.showWarningPopup}"/>

function SetAllCheckBoxes(FormName, AreaID, CheckValue) {
    if (!document.forms[FormName])
        return;
    var objCheckBoxes = document.getElementById(AreaID).getElementsByTagName('input');
    if (!objCheckBoxes)
        return;
    var countCheckBoxes = objCheckBoxes.length;
    if (!countCheckBoxes)
        objCheckBoxes.checked = CheckValue;
    else
        for (var i = 0; i < countCheckBoxes; i++)
            objCheckBoxes[i].checked = CheckValue;
}
function CancelClick() {
    document.getElementById("admintool:divSearchResult").style.display = "none";
    document.getElementById("admintool:txtDeviceId").value = "";
}

function validateDeviceId() 
{                    
    var deviceId = document.getElementById("admintool:txtDeviceId").value;                   
    if( ((deviceId.length == 8) || (deviceId.length == 6)) && validate(deviceId))
    {    	   
    	return true;
    }
    else
    {
        alert("Device ID must be 6 or 8 character hexadecimal string (0-9, A-F) only.");
        return false;
    }
}    
function validate(deviceId)
{
        var alphanum=/^[0-9a-fA-F]+$/; 
        if(deviceId.match(alphanum))
        {
                return true;
        }
        else
        {
                return false;
        }
}


function validateDeviceInput(eventRef) {
	var charCode = eventRef.keyCode ? eventRef.keyCode : ((eventRef.charCode) ? eventRef.charCode : eventRef.which);
    if(charCode == 8  || charCode == 27   || charCode == 9){
    	return true;
    } else if ( eventRef.keyCode != null && (eventRef.keyCode == 46 || eventRef.keyCode == 37 || eventRef.keyCode == 39 ) 
    		&& eventRef.charCode != null && eventRef.charCode == 0) {
    	return true;
    }else if((eventRef.ctrlKey == true || eventRef.metaKey == true ) && (eventRef.charCode == 99 || eventRef.charCode == 118 || eventRef.charCode == 120 || eventRef.charCode == 67  ||            eventRef.charCode == 86 || eventRef.charCode 
    		== 88)){
    	return true;
    }
    var character = String.fromCharCode(charCode);                                              
    var alphanum=/^[0-9a-fA-F]+$/; 
    if(character.match(alphanum)) {
    	return true;
    } else  {
    	return false;
    }
}

function validate(deviceId) {        	 
	var alphanum=/^[0-9a-fA-F]+$/; 
	if(deviceId.match(alphanum)) {
		return true;
	} else {
		return false;
	}
}

function SearchClick() {
	document.getElementById("admintool:txtDeviceId").value = "";
	if(document.getElementById("admintool:tblResult:checkall") != null) {
		document.getElementById("admintool:tblResult:checkall").checked = false ;
	}else if (document.getElementById("admintool:tblResult:0:checkall") != null){
		document.getElementById("admintool:tblResult:0:checkall").checked = false 
	} else {
		document.getElementById("admintool:tblResult:1:checkall").checked = false 
	}
	
}
function AfterDelete() {
	document.getElementById("admintool:txtDeviceId").value = "";  
}

function SetAllCheckBoxes(allCheckBoxId) {
	var checkAllValue = document.getElementById(allCheckBoxId).checked ;
	for (var i = 0; i < document.getElementById('admintool').elements.length; i++) {
        if(document.getElementById('admintool').elements[i].type=='checkbox') {
        	document.getElementById('admintool').elements[i].checked = checkAllValue ;
        }
    }                       
}
function CheckAllCheckBoxes(checkBoxId) {
		var isChecked = document.getElementById(checkBoxId).checked ;
		if (! isChecked ) {
			if(document.getElementById("admintool:tblResult:checkall") != null) {
				document.getElementById("admintool:tblResult:checkall").checked = isChecked ;
			}else if (document.getElementById("admintool:tblResult:0:checkall") != null){
				document.getElementById("admintool:tblResult:0:checkall").checked = isChecked 
			} else {
				document.getElementById("admintool:tblResult:1:checkall").checked = isChecked 
			}
		}
}

function DeleteClick() {
    var canDelete = false;
    for (var i = 0; i < document.getElementById('admintool').elements.length; i++) {
        if(document.getElementById('admintool').elements[i].type=='checkbox')
        {
            if (document.getElementById('admintool').elements[i].checked == true){
            	canDelete = true;
            } 
        }
    }
    if (canDelete) {
        return ( confirm("Are you sure you want to delete selected records ?") );
    }
    else {
        alert("Please select a record to delete.");
        return false;
    }
}

function returnUpdateDeviceId(index){
	return document.getElementById("Roster:rosterData:"+index+":txtUpdate").value;
}

function returnRegisterDeviceId(index){
	return document.getElementById("Roster:rosterData:"+index+":txtRegister").value;
}

function OnRegisterCancel(caller){

	var divToHideId="Roster:rosterData:"+caller+":divRegister";
	var divToShowId="Roster:rosterData:"+caller+":divLnk";
	var divToHide=document.getElementById(divToHideId);
	var divToShow=document.getElementById(divToShowId);
	divToHide.style.display='none';
	divToShow.style.display='block';
	document.getElementById('Roster:rosterData:'+caller+':txtRegister').value = "" ;

	return false;
		
}
function NoDeviceClick(caller){

	var divToHideId="Roster:rosterData:"+caller+":divLnk";
	var divToShowId="Roster:rosterData:"+caller+":divRegister";
	var divToHide=document.getElementById(divToHideId);
	var divToShow=document.getElementById(divToShowId);
	var hideDivs = document.getElementsByRegex('^Roster:rosterData:.*?:divRegister');
    var displayDivs= document.getElementsByRegex('^Roster:rosterData:.*?:divLnk');
    ToggleDivDisplay(hideDivs,displayDivs,divToShowId,"NoDevice");
    document.getElementById('Roster:rosterData:'+caller+':txtRegister').value = "" ;
	divToHide.style.display='none';
	divToShow.style.display='block';

	return false;
}
function OnDeviceClick(caller){

	var divToHideId="Roster:rosterData:"+caller+":divDevice";
	var divToShowId="Roster:rosterData:"+caller+":divUpdate";
	var divToHide=document.getElementById(divToHideId);
	var divToShow=document.getElementById(divToShowId);
	var hideDivs = document.getElementsByRegex('^Roster:rosterData:.*?:divUpdate'); //Roster:rosterData:1:divUpdate"  //getElementsByRegex('^divUpdate_.*')
    var displayDivs= document.getElementsByRegex('^Roster:rosterData:.*?:divDevice');
    ToggleDivDisplay(hideDivs,displayDivs,divToShowId,"OnDevice");
    document.getElementById('Roster:rosterData:'+caller+':txtUpdate').value = document.getElementById('Roster:rosterData:'+caller+':lnkDevice').innerHTML ;
	divToHide.style.display='none';
	divToShow.style.display='block';

	return false;
}
function OnUpdateCancel(caller){

	var divToHideId="Roster:rosterData:"+caller+":divUpdate";
	var divToShowId="Roster:rosterData:"+caller+":divDevice";
	var divToHide=document.getElementById(divToHideId);
	var divToShow=document.getElementById(divToShowId);
	divToHide.style.display='none';
	divToShow.style.display='block';
	document.getElementById('Roster:rosterData:'+caller+':txtUpdate').value = document.getElementById('Roster:rosterData:'+caller+':lnkDevice').innerHTML ;

	return false;
}


function hide(divID) {
	if(divID == 'divResponseCard'){
		if(document.getElementById("addCardDevice:divResponseCard"))
			document.getElementById("addCardDevice:divResponseCard").className = 'hidden' ;
	}else{
		if(document.getElementById("addResWareDevice:divResponseWare"))
			document.getElementById("addResWareDevice:divResponseWare").className = 'hidden' ;
	}
}




/* Student Related Scripts*/
function validateStudentDeviceId() 
{                    
    var deviceId = document.getElementById("addCardDevice:txtRegisterDeviceId").value;                   
    if( (deviceId.length == 6) && validate(deviceId))
    {    	   
    	return true;
    }
    else
    {
        alert("Not a valid Device ID. Valid IDs are 6 characters (0-9 and/or A-F or a-f).");
        return false;
    }
} 

function validateUpdateResponseCardDeviceId(index) 
{                    
    var deviceId = document.getElementById("Roster:rosterData:"+index+":txtUpdate").value;                   
    if( (deviceId.length == 6) && validate(deviceId))
    {    	   
    	return true;
    }
    else
    {
        alert("Not a valid Device ID. Valid IDs are 6 characters (0-9 and/or A-F or a-f).");
        return false;
    }
} 

function validateRegisterResponseWareDeviceId(index) 
{                    
    var deviceId = document.getElementById("Roster:rosterData:"+index+":txtRegister").value;                   
    if( (deviceId.length == 6) && validate(deviceId))
    {    	   
    	return true;
    }
    else
    {
        alert("Not a valid Device ID. Valid IDs are 6 characters (0-9 and/or A-F or a-f).");
        return false;
    }
} 

function EditResponseCard() {
    var divToHideId = "DeviceIDs:divEditRC";
    var divToShowId = "DeviceIDs:divUpdateCancelRC";
    var textDivToShowId = "DeviceIDs:divTextResponseCard";
    var lblDivToHideId = "DeviceIDs:divResponseCardId";
    var divToHide = document.getElementById(divToHideId);
    var divToShow = document.getElementById(divToShowId);
    var lblToHide = document.getElementById(lblDivToHideId);
    var textdivToShow = document.getElementById(textDivToShowId);
    var txtResponseCardId = "DeviceIDs:txtResponseCard";
    if(lblToHide.innerHTML!="Register ResponseCard"){
    	document.getElementById(txtResponseCardId).value = lblToHide.innerHTML;
    }else{
    	document.getElementById(txtResponseCardId).value = "";
    }
    var hideDivs = document.getElementsByRegex('^DeviceIDs:divUpdateCancelRC');
    var displayDivs = document.getElementsByRegex('^DeviceIDs:divEditRC');
    ToggleDivDisplayStudent(hideDivs, displayDivs, divToShowId, "ResponseCard", textDivToShowId, lblDivToHideId);
    divToHide.style.display = 'none';
    divToShow.style.display = 'inline';
    lblToHide.style.display = 'none';
    textdivToShow.style.display = 'inline';
    return false;
}

function DeleteResponseCard(caller) {
    var canDelete = true;
    var str = caller.id;
    var n = str.split("_");
    if (canDelete) {
        var r = confirm("Are you sure you want to delete this Device ID?");
        if (r == true) {
            document.deviceregistration.formAction.value = "<%= FormAction.RC_DELETE %>";
            document.deviceregistration.action = "deviceregistration.jsp";
            document.deviceregistration.submit();
        }
    } else {
    }
}
function DeleteResponseWare(caller) {
    var canDelete = true;
    var str = caller.id;
    var n = str.split("_");
    if (canDelete) {
        var r = confirm("Are you sure you want to delete this Device ID?");
        if (r == true) {
            document.deviceregistration.formAction.value = "<%= FormAction.RW_DELETE %>";
            document.deviceregistration.action = "deviceregistration.jsp";
            document.deviceregistration.submit();
        }
    } else {
    }
}
function RegisterResponseCard(caller) {
    var deviceId = document.deviceregistration.txtResponseCard_1.value;
    if ((deviceId.length == 6) && validate(deviceId)) {
        document.deviceregistration.formAction.value = "<%= FormAction.RC_ADD %>";
        document.deviceregistration.deviceId.value = deviceId;
        document.deviceregistration.action = "deviceregistration.jsp";
        document.deviceregistration.submit();
    }
    else {
        alert("Not a valid Keypad ID. Valid IDs are 6 characters (0-9 and/or A-F or a-f).");
        return false;
    }

}
function UpdateResponseWare(caller) {
    var deviceId = document.deviceregistration.txtResponseWare_1.value;
    if ((deviceId.length == 8) && validate(deviceId)) {
        document.deviceregistration.formAction.value = "<%= FormAction.RW_UPDATE %>";
        document.deviceregistration.deviceId.value = deviceId;
        document.deviceregistration.action = "deviceregistration.jsp";
        document.deviceregistration.submit();
    }
    else {
        alert("Not a valid Keypad ID. Valid IDs are 8 characters (0-9 and/or A-F or a-f).");
        return false;
    }
}
function CancelResponseCard() {
    var divToShowId = "DeviceIDs:divEditRC";
    var divToHideId = "DeviceIDs:divUpdateCancelRC";
    var divToHide = document.getElementById(divToHideId);
    var divToShow = document.getElementById(divToShowId);
    var textDivToHideId = "DeviceIDs:divTextResponseCard";
    var lblDivToShowId = "DeviceIDs:divResponseCardId";
    var lblToShow = document.getElementById(lblDivToShowId);
    var textdivToHide = document.getElementById(textDivToHideId);
    if(divToHide){
    	divToHide.style.display = 'none';
    }
    if(divToShow){
    	divToShow.style.display = 'block';
    }
    if(textdivToHide){
    	textdivToHide.style.display = 'none';
    }
    if(lblToShow){
    	lblToShow.style.display = 'block';
    }
    return false;
}
function CancelResponseWare() {
    //var divToShowId = "DeviceIDs:divEditRW";
    var divToHideId = "DeviceIDs:divUpdateCancelRW";
    var divToHide = document.getElementById(divToHideId);
    //var divToShow = document.getElementById(divToShowId);
    var textDivToHideId = "DeviceIDs:divTextResponseWare";
    var lblDivToShowId = "DeviceIDs:divResponseWare";
    var lblToShow = document.getElementById(lblDivToShowId);
    var textdivToHide = document.getElementById(textDivToHideId);
    divToHide.style.display = 'none';
    //divToShow.style.display = 'block';
    textdivToHide.style.display = 'none';
    lblToShow.style.display = 'block';
    return false;
}
var counter = 0 ;
function unhide(divID) {
    document.getElementById("addCardDevice:txtRegisterDeviceId").value = "";
    document.getElementById("addResWareDevice:txtResponseWareEmail").value = "";
    document.getElementById("addResWareDevice:txtResponseWarePassword").value = "";
    document.getElementById("addResWareDevice:spnResponseWareMessage").innerHTML = "";
    document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "";
    document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "";
    document.getElementById("addCardDevice:spnMessage").innerHTML = "";
    
    var	item;
    if(divID == 'divResponseCard')
    	item = document.getElementById("addCardDevice:"+divID);
    else
    	item = document.getElementById("addResWareDevice:"+divID);

    var itemToHide;
    if (item) {
    	CancelResponseCard();
        switch (divID) {
            case "divResponseCard":
                itemToHide = document.getElementById("addResWareDevice:divResponseWare");
                itemToHide.className = 'hidden';
                item.className = (item.className == 'hidden') ? 'unhidden' : 'hidden';
                setTimeout("document.getElementById('addCardDevice:txtRegisterDeviceId').focus()", 50);
                break;
            case "divResponseWare":
                itemToHide = document.getElementById("addCardDevice:divResponseCard");
                itemToHide.className = 'hidden';
                item.className = (item.className == 'hidden') ? 'unhidden' : 'hidden';
                setTimeout("document.getElementById('addResWareDevice:txtResponseWareEmail').focus()", 50);
                break;
            default:
                item.className = 'hidden';
        }
    }
    else {
        var hideRw = document.getElementById("addResWareDevice:divResponseWare");
        var hideRc = document.getElementById("addCardDevice:divResponseCard");
        hideRw.className = 'hidden';
        hideRc.className = 'hidden';
    }
    if(counter == 0) {
    	setMainFrameHeight(window.name);
    	counter = counter +1 ;
    }
    
}

function ToggleDivDisplayStudent(hideDivs, displayDivs, divToShowId, device, txtToShowId, lnkToHideId) {
    var deviceClick = device;
    if (deviceClick == "ResponseCard") {
        var hideRegisterDivs = document.getElementsByRegex('^DeviceIDs:divUpdateCancelRC');
        var displayLnkDivs = document.getElementsByRegex('^DeviceIDs:divEditRC');
        for (var i = 0; hideRegisterDivs[i]; i++) {
            hideRegisterDivs[i].style.display = 'none';
        }
        for (var i = 0; displayLnkDivs[i]; i++) {
            displayLnkDivs[i].style.display = 'inline';
        }
        //var hideRegisterDivsRW = document.getElementsByRegex('^DeviceIDs:divUpdateCancelRW');
        //var displayLnkDivsRW = document.getElementsByRegex('^DeviceIDs:divEditRW');
        //var hideTextsRW = document.getElementsByRegex('^DeviceIDs:divTextResponseWare');
        var displayLinkRW = document.getElementById('addResWareDevice:divResponseWare');
        var hideTextRC = document.getElementsByRegex('^DeviceIDs:divTextResponseCard');
        //var displayLinkRC = document.getElementsByRegex('^addCardDevice:divResponseCard');
        var displayLinkRC = document.getElementById('addCardDevice:divResponseCard');
        
        for (var i = 0; hideTextRC[i]; i++) {
            if (hideTextRC[i].id == txtToShowId) { }
            else {
                hideTextRC[i].style.display = 'none';
            }
        }
        //for (var i = 0; hideTextRC[i]; i++) {
            if (hideTextRC.id == txtToShowId) { }
            else {
                //displayLinkRC[i].style.display = 'inline';
            	displayLinkRC.className = 'hidden';
            }
        //}
        /*for (var i = 0; hideRegisterDivsRW[i]; i++) {
            hideRegisterDivsRW[i].style.display = 'none';
        }
        for (var i = 0; displayLnkDivsRW[i]; i++) {
            displayLnkDivsRW[i].style.display = 'inline';
        }
        for (var i = 0; hideTextsRW[i]; i++) {
            hideTextsRW[i].style.display = 'none';;
        }
        for (var i = 0; displayLinksRW[i]; i++) {
            displayLinksRW[i].style.display = 'inline';
        }*/
        displayLinkRW.className = 'hidden';
        //if()
        //displayLinkRC.className = 'hidden';
    }
    else {
        var hideUpdateDivs = document.getElementsByRegex('^DeviceIDs:divUpdateCancelRW');
        //var displayDeviceDivs = document.getElementsByRegex('^DeviceIDs:divEditRW');
        for (var i = 0; hideUpdateDivs[i]; i++) {
            hideUpdateDivs[i].style.display = 'none';
        }
        /*for (var i = 0; displayDeviceDivs[i]; i++) {
            displayDeviceDivs[i].style.display = 'block';
        }*/
        var hideRegisterDivsRC = document.getElementsByRegex('^DeviceIDs:divUpdateCancelRC');
        var displayLnkDivsRC = document.getElementsByRegex('^DeviceIDs:divEditRC');
        var hideTextsRC = document.getElementsByRegex('^DeviceIDs:divTextResponseCard');
        var displayLinksRC = document.getElementsByRegex('^addCardDevic:divResponseCard');
        var hideTextRW = document.getElementsByRegex('^DeviceIDs:divTextResponseWare');
        var displayLinkRW = document.getElementsByRegex('^addResWareDevice:divResponseWare');
        for (var i = 0; hideTextRW[i]; i++) {
            if (hideTextRW[i].id == txtToShowId) { }
            else {
                hideTextRW[i].style.display = 'none';
            }
        }
        for (var i = 0; hideTextRW[i]; i++) {
            if (hideTextRW[i].id == txtToShowId) { }
            else {
                displayLinkRW[i].style.display = 'block';
            }
        }
        for (var i = 0; hideRegisterDivsRC[i]; i++) {
            hideRegisterDivsRC[i].style.display = 'none';
        }
        for (var i = 0; displayLnkDivsRC[i]; i++) {
            displayLnkDivsRC[i].style.display = 'block';
        }
        for (var i = 0; hideTextsRC[i]; i++) {
            hideTextsRC[i].style.display = 'none';
        }
        for (var i = 0; displayLinksRC[i]; i++) {
            displayLinksRC[i].style.display = 'block';
        }
    }
    for (var i = 0; hideDivs[i]; i++) {
        if (hideDivs[i].id == divToShowId) { }
        else {
            hideDivs[i].style.display = 'none';
        }
    }
    for (var i = 0; displayDivs[i]; i++) {
        if (hideDivs[i].id == divToShowId) { }
        else {
            displayDivs[i].style.display = 'block';
        }
    }
}
function returnAddResponseCard(){
	return document.getElementById("addCardDevice:txtRegisterDeviceId").value;
}

function getUserEmailId(){
	return document.getElementById("addResWareDevice:txtResponseWareEmail").value;
}

function getUserPassword(){
	return document.getElementById("addResWareDevice:txtResponseWarePassword").value;
}

function validateStudentResponseCard() 
{                    
    var deviceId = document.getElementById("DeviceIDs:txtResponseCard").value;                   
    if( (deviceId.length == 6) && validate(deviceId))
    {    	   
    	return true;
    }
    else
    {
        alert("Not a valid Device ID. Valid IDs are 6 characters (0-9 and/or A-F or a-f).");
        return false;
    }
} 

function validateStudentResponseWare() 
{                    
    var deviceId = document.getElementById("DeviceIDs:txtResponseWare").value;                   
    if( (deviceId.length == 8) && validate(deviceId))
    {    	   
    	return true;
    }
    else
    {
        alert("Not a valid Device ID. Valid IDs are 6 characters (0-9 and/or A-F or a-f).");
        return false;
    }
} 

function returnResponseCard(){
	return document.getElementById("DeviceIDs:txtResponseCard").value;
}

function returnResponseWare(){
	return document.getElementById("DeviceIDs:txtResponseWare").value;
}

function returnResponseCardDelete(){
	return document.getElementById("DeviceIDs:divResponseCardId").innerHTML;
}
function returnResponseWareDelete(){
	return document.getElementById("DeviceIDs:divResponseWareId").innerHTML;
}
function chkRegisterResponseWare(){
	var email = document.getElementById("addResWareDevice:txtResponseWareEmail").value;
	var password = document.getElementById("addResWareDevice:txtResponseWarePassword").value;

	if(email==""){
		document.getElementById("addResWareDevice:spnEmailMessage").innerHTML='You must provide a ResponseWare Email Address';
		return false;
	}else{
		document.getElementById("addResWareDevice:spnEmailMessage").innerHTML='';
	}
	if(password==""){
		document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML='You must provide a ResponseWare password';
		return false;
	}else{
		document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML='';
	}
	if(email!="" && password!=""){
		return true;
	}
}
function registerCompleteRC(){
	
	var status = document.getElementById("DeviceIDs:statusField").value;
	  if(status == 'true'){
		  RegisterRCSuccess();
	  }else{
		  RegisterRCServerError();
	  }
} 

function RegisterRCSuccess() {
    //unhide('divResponseCard');
    window.location.hash = '#divResponseCard';
    document.getElementById("addCardDevice:spnMessage").style.display = "inline";
    document.getElementById("addCardDevice:spnMessage").innerHTML = "Your Device ID has been successfully registered for all courses.";
    document.getElementById("addCardDevice:txtRegisterDeviceId").value = "";
          
  }     
  function RegisterRCServerError() {
    //unhide('divResponseCard');
    window.location.hash = '#divResponseCard';
    document.getElementById("addCardDevice:spnMessage").style.display = "inline";
          document.getElementById("addCardDevice:spnMessage").innerHTML = "Failed to register Device ID due to Server error.";                
  }
  
  function RegisterRW() {
    document.getElementById("addCardDevice:spnMessage").style.display = "none";
      var eMail = document.getElementById("addResWareDevice:txtResponseWareEmail");
      var password = document.getElementById("addResWareDevice:txtResponseWarePassword");

      if (eMail.value == "" && password.value == "") {
          document.getElementById("addResWareDevice:spnEmailMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "You must provide a ResponseWare Email Address.";
          document.getElementById("addResWareDevice:spnPasswordMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "You must provide a ResponseWare password.";
          document.getElementById("addResWareDevice:spnResponseWareMessage").style.display = "none";

      }
      else if (password.value == "" && eMail.value != "") {
          document.getElementById("addResWareDevice:spnPasswordMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "You must provide a ResponseWare password.";
          document.getElementById("addResWareDevice:spnEmailMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnResponseWareMessage").style.display = "none";

      }
      else if (eMail.value == "" && password.value != "") {
          document.getElementById("addResWareDevice:spnEmailMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "You must provide a ResponseWare Email Address.";
          document.getElementById("addResWareDevice:spnPasswordMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnResponseWareMessage").style.display = "none";
      }
      else {
       return true;      
      }
  }
  function onCompleteRegisterRW(){
	  var status = document.getElementById("DeviceIDs:statusField").value;
	  if(status == 'true'){
		  RegisterRWSuccess();
	  }else{
		  RegisterRWInvalidId();
	  }
  }
  function RegisterRWSuccess () {
    //unhide('divResponseWare');
    window.location.hash = '#divResponseWare';
    document.getElementById("addResWareDevice:spnPasswordMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnEmailMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnResponseWareMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnResponseWareMessage").innerHTML = "Your Device ID has been successfully registered for all courses.";
          
     document.getElementById("addResWareDevice:txtResponseWareEmail").value = "";
     document.getElementById("addResWareDevice:txtResponseWarePassword").value = "";
  }
  function RegisterRWInvalidId () {
    //unhide('divResponseWare');
    window.location.hash = '#divResponseWare';
    document.getElementById("addResWareDevice:spnPasswordMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnEmailMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnResponseWareMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnResponseWareMessage").innerHTML = "The ResponseWare Email Address and Password does not exist. If you have not registered or have forgotten your password use the appropriate link below.";
  }
  function RegisterRWServerError () {
    unhide('divResponseWare');
    window.location.hash = '#divResponseWare';
    document.getElementById("addResWareDevice:spnPasswordMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnPasswordMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnEmailMessage").style.display = "none";
          document.getElementById("addResWareDevice:spnEmailMessage").innerHTML = "";
          document.getElementById("addResWareDevice:spnResponseWareMessage").style.display = "inline";
          document.getElementById("addResWareDevice:spnResponseWareMessage").innerHTML = "Failed to register Device ID due to Server error.";
  }
  
  function ConfirmDelete() {
	return ( confirm("Are you sure you want to delete this Device ID?") );
  }
