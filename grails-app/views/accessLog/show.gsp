<html>
<head>
	<meta name="layout" content="admin"/>
	<title>Show AccessLog</title>
</head>

<body>
<div class="body">
	<h1>Show AccessLog</h1>
	<g:if test="${flash.message}">
		<div class="message">${flash.message}</div>
	</g:if>
	<div class="dialog">
		<table>
			<tbody>

			<tr class="prop">
				<td valign="top" class="name">Id:</td>
				<td valign="top" class="value">${fieldValue(bean: accessLogInstance, field: 'id')}</td>
			</tr>

			<tr class="prop">
				<td valign="top" class="name">User:</td>
				<td valign="top" class="value">${fieldValue(bean: accessLogInstance, field: 'username')}</td>
			</tr>

			<tr class="prop">
				<td valign="top" class="name">Event:</td>
				<td valign="top" class="value">${fieldValue(bean: accessLogInstance, field: 'event')}</td>
			</tr>

			<tr class="prop">
				<td valign="top" class="name">Event Message:</td>
				<td valign="top" class="value">${fieldValue(bean: accessLogInstance, field: 'eventmessage')}</td>
			</tr>

			<tr class="prop">
				<td valign="top" class="name">Access Time:</td>
				<td valign="top" class="value">${fieldValue(bean: accessLogInstance, field: 'accesstime')}</td>
			</tr>

			</tbody>
		</table>
	</div>

	<div class="buttons">
		<g:form>
			<input type="hidden" name="id" value="${accessLogInstance?.id}"/>
			<span class="button"><g:actionSubmit class="edit" value="Edit"/></span>
			<span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete"/></span>
		</g:form>
	</div>
</div>
</body>
</html>
