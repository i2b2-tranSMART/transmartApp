<head>
	<meta name='layout' content='main'/>
	<title>Authentication Error in ${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body style="background-color: darkRed">
<div style="text-align: center;">
	<div style="margin: 50px auto 50px auto;">
		<img style="width: 700px"
		     src="${resource(dir: 'images', file: grailsApplication.config.com.recomdata.largeLogo?:'transmartlogoHMS.jpg')}" alt="Transmart"/>

        <div style="font-size: 200%; margin-top: 20px; color: orange">Processing login information.</div><br />

        <script>
            window.location='callback?token='+localStorage.id_token;
        </script>
         ${flash.error}
	</div>
</div>
</body>
