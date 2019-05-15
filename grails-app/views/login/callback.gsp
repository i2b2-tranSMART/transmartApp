<head>
	<meta name='layout' content='main'/>
	<title>Authentication Error in ${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body style="background-color: darkRed">
<div style="text-align: center;">
	<div style="margin: 50px auto 50px auto;">
		<img style="width: 700px"
		     src="${grailsApplication.config.com.recomdata.largeLogo?:'/images/transmartlogoHMS.jpg'}" alt="Transmart"/>

        <div style="font-size: 200%; margin-top: 20px; color: red">Token is missing. Let me check some other stuff in the session and get back something else.</div><br />

        ${flash.error}

	</div>


</div>
</body>
