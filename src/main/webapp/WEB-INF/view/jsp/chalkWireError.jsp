<%@ page isErrorPage="true" %> 
<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
	<head>
		<link rel="stylesheet" href="css/style.css" type="text/css">
	</head>
<body>

	<div class="error">
		<h2>Chalk & Wire: Authentication Failed</h2>
		<b>Reason: </b><br/><c:out value='${requestScope.exception.message}' />
		
		<p><br><a class="myButton" href="<c:out value='${requestScope.loginUrl}' />">Retry</a>
	</div>

</body>
</html>