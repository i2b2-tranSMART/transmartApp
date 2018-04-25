<html>
<head>
	<r:require module="main_mod"/>
	<link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:400,600,700"/>
	<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
	<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
	<g:layoutHead/>
	<r:layoutResources/>
</head>

<body>
<div id="header-div">
	<g:render template='/layouts/commonheader' model="[app: '']"/>
</div>
<g:layoutBody/>
<script>
	$(document).ready(function () {
		setTimeout(function () {
			$('#flashMessage').fadeOut('fast');
		}, 5000);
	});
</script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<r:layoutResources/>
</body>
</html>
