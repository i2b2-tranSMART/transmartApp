<head>
	<meta name='layout' content='main'/>
	<title>Authentication Error in ${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body style="background-color: darkRed">
<div style="text-align: center;">
	<div style="margin: 50px auto 50px auto;">
		<img style="width: 700px"
		     src="${resource(dir: 'images', file: grailsApplication.config.com.recomdata.largeLogo?:'transmartlogoHMS.jpg')}" alt="Transmart"/>

        <div style="font-size: 200%; margin-top: 20px; color: red">You do not have access to this appliaction</div><br />
		<div style="color: red; font-size: 120%; display: block">ErrorMessage: ${ errorMessage }</div><br />
			<br />
		<div style="text-align: center;">If you would like to use the tranSMART system, please contact a <a
				href="mailto:${grailsApplication.config.com.recomdata.adminEmail}??subject=tranSMART new user request&body=Please, add me as a new user to the tranSMART system.">tranSMART Admin</a>
		</div>
        <br />
        <a href="${resource(action: 'index')}/">Back to Home</a>
	</div>
</div>
</body>
