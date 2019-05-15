<head>
	<meta name='layout' content='main'/>
	<title>${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body>
<div style="text-align: center;">
	<div style="width: 400px; margin: 50px auto 50px auto;">
		<img style="display: block; margin: 12px auto;" alt="Transmart"
		     src="${grailsApplication.config.com.recomdata.largeLogo?:'/static/transmartlogoHMS.jpg'}" />

		<div style="text-align: center;"><h3>ATTENTION: Users of ${grailsApplication.config.com.recomdata.appTitle}</h3></div>

		<div style="text-align: justify; margin: 12px;">
			${grailsApplication.config.com.recomdata.disclaimer}
		</div>
		<div style="text-align: center;">
			<g:form name='disclaimer' id='disclaimerForm'>
				<g:actionSubmit value='I agree' action='agree'/>
				<g:actionSubmit value='I disagree' action='disagree'/>
			</g:form>
		</div>
	</div>
</div>
</body>
