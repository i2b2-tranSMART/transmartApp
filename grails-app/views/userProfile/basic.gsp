<html lang="en">
	<head>
	    <r:require module="main_mod"/>
	    <title>User profile</title>
	    <r:layoutResources/>
	    
	    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:400,600,700"/>
	    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
		<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
        <link rel="stylesheet" type="text/css" href="${resource(dir:'css', file: 'userProfile.css')}">

	</head>
	
	
	
	<body>
		<div id="header-div">
		    <g:render template="/layouts/commonheader" model="[app: '']"/>
		</div>
	
		<div class="body" style="padding-left: 15%">
		    <h1 class="menuHeader">User Profile</h1>
		    <g:if test="${flash.message}">
		        <div id="flashMessage" class="message">${flash.message}</div>
		    </g:if>
			<g:if test="${flash.error}">
				<div id="flashMessage" class="warning">${flash.error}</div>
			</g:if>
			
			<div style="min-width: 700px; max-width: 1000px; padding-top: 15px;">
                <input class="profileTab" id="tab1" type="radio" name="tabs" checked>
                <label class="tabLabel" for="tab1">Personal</label>
                <input class="profileTab" id="tab2" type="radio" name="tabs">
                <label class="tabLabel" for="tab2">Access Level</label>

                <section id="content1">
					<g:form action="basic">
						<br />
						<div class="form-section">
							<g:if test="${!user.email.contains('@') || params.controller == 'user'}">
							%{--E-mail is not really an e-mail, so we need to ask for it.--}%
								<div class="col-xs-12">
									<label for="email">E-mail<sup>*</sup></label>
									<g:field class='form-control' name='email' required='required' type='email' value="${user.email.contains('@') ? user.email : ''}" />
								</div>
							</g:if>
							<g:else>
							%{--E-mail seems to be valid, so we just display it--}%
								<div class="col-xs-12">
									<label for="email">E-mail</label>
									<g:hiddenField name='email' value="${user.email}" />
									<p class="form-control-static">${user.email}</p>
								</div>
							</g:else>
							<div class="col-xs-12">
								<br />
							</div>
                            <div class="col-xs-12">
                                <label for="username">Username</label>
                                <g:hiddenField name='username' value="${user.username}" />
                                <p class="form-control-static">${user.username}</p>
                            </div>
                            <div class="col-xs-12">
                                <br />
                            </div>
							<div class="col-xs-12">
								<label for="userRealName">Name<sup>*</sup></label>
								<g:textField class='form-control' name='userRealName' required='required' value="${user.userRealName}" />
							</div>
							<div class="col-xs-12">
                                <br />
                            </div>
                        </div>
						<div class="buttons" style="margin-top:20px; margin-bottom:20px">
							<span class="button">
								<input class="save" type="submit" value="Save"/>
							</span>
						</div>
					</g:form>

                </section>
				<section id="content2">
					<g:if test="${level}">
						<g:render template="/user/access_level"/>
					</g:if>
					<g:else>
						<p>User access levels were <b>not defined</b> for this instance of i2b2/transmart.</p>
					</g:else>
				</section>
			</div>
		</div>
		<r:layoutResources/>
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
	</body>
</html>
