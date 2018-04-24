<html>
<head>
	<meta name="layout" content="admin"/>
	<title>AccessLog List</title>
</head>

<body>
<div class="body">
	<g:render template='/buildInfo/buildInfo' model="[warDeployed: warDeployed]"/><br/>
	<g:render template='/buildInfo/runtimeStatus' model="[envName: envName, javaVersion: javaVersion]"/><br/>
	<g:render template='/buildInfo/installedPlugins' model="[plugins: plugins]"/><br/>
</div>
</body>
</html>
