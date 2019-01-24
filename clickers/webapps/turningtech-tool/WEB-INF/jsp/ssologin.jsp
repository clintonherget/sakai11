<%@page language="java" pageEncoding="ISO-8859-1" %>

<%@page import="com.turningtech.turningtool.authentication.TurningSecurityInterceptor"%>
<%@page import="com.turningtech.turningtool.vo.Token"%>

<%
		String access_token = "test token" ;
		String userName  = "test user name" ;
		try {
			TurningSecurityInterceptor turningSecurity = new TurningSecurityInterceptor() ; 
			if (! turningSecurity.isAuthenticated(request) )
			{
				String loginURL = turningSecurity.getLoginUrl(request) ;
				response.sendRedirect(loginURL);
				return;
			}
			Token token = turningSecurity.getToken(request) ;
			access_token = token.getAccessToken() ;
			userName = token.getUserName();
			response.addCookie(new Cookie("TurningPointUserTokenHash", access_token)) ;	
		} catch (Exception ex) {
			
		}
		
		
%>
		<form name="authentication"  title="Turning Technologies ">
		 	
		 	<h3>Turning Technologies is requesting access to your account. You are authorizing this app as <%=userName %> Instructor.</h3>
		 	<br></br><br></br>
		 	 
			<div id="token" align="left" style="width: 100%">                        
            	Authorization Token: <input type="text" name="token" value="<%=access_token%>" width="75%" /> 
            	    (will be removed after development..)
            	<br></br>
            	
            </div>
            <div id="submit" align="left" style="width: 100%">
           		<input type="submit" value="Cancel" />  
                <input type="submit" value="Authorize"  style="color:blue; "  />            
             </div>
            </form>
