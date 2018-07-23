<html>
<head>
<meta name="layout" content="admin"/>
<title>Loggers</title>
</head>
<body>
<g:form action='setLogLevel'>
	<label for="logger">Logger:</label>
	<g:textField name='logger' size='75' />
	<select name="level" id="level"><g:each var='level' in='${allLevels}'>
		<option>${level}</option></g:each>
	</select>
	<g:submitButton name="Set Level"/>
</g:form>
</body>
</html>
