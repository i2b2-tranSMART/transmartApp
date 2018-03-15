<!DOCTYPE html>
<html lang="en">
	<head>
	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	    <r:require module="main_mod"/>
	    <title>User profile</title>
	    <r:layoutResources/>
	    
	    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:400,600,700"/>
	    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
		<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">

        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>

        <style>
            *, *:before, *:after {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            p {
                margin: 0 0 20px;
                line-height: 1.5;
            }

            section {
                display: none;
                padding: 20px 0 0;
                border-top: 1px solid #ddd;
            }

            .profileTab {
                display: none;
            }

            .tabLabel {
                display: inline-block;
                margin: 0 0 -1px;
                padding: 10px 20px;
                font-weight: 600;
                text-align: center;
                color: #808080;
                border: 1px solid transparent;
            }

            .tabLabel:hover {
                color: #888;
                cursor: pointer;
            }

            input:checked + label {
                color: #555;
                border: 1px solid #ddd;
                border-bottom: 1px solid #fff;
            }

            #tab1:checked ~ #content1,
            #tab2:checked ~ #content2 {
                display: block;
            }
        </style>

	</head>
	
	
	
	<body>
		<div id="header-div">
		    <g:render template="/layouts/commonheader" model="['app': '']"/>
		</div>
	
		<div class="body" style="padding-left: 15%">
		    <h1 style="color: #006dba; font-weight: normal; font-size: 16px; margin: .8em 0 .3em 0;">User Profile</h1>
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
                    <h3 class="page-header">Access Level</h3>
                    <div class="row">
                        <div class="col-lg-12">
                            <g:if test="${level < 2}">
                                <p>Your current access level is <b>Open Data Access</b>.</p>
                                <p>This means that you are able to run <i>summary statistics</i> and <i>advanced statistics</i> queries, but you cannot see or download <b><i>patient-level data</i>
                                </b>.</p>
                                <p>Contact system administrator to request <b>Controlled Data Access</b>.
                                <ul>
                                    <li>Ensure that your <b>Profile</b> information is up-to-date.</li>
                                    <li>Fill out the <b>Access Request Form</b>.</li>
                                </ul>
                                </p>
                                <p>The administrator will review information provided in the email and will coordinate its submission to the Data Access Committee.</p>

                            </g:if>
                            <g:else>
                                <p>Your current access level is <b>Controlled Data Access</b>.</p>
                                <p>This means that you are able to run <i>summary statistics</i> and <i>advanced statistics</i> queries, you can also see <i>patient level data</i> and <i>download or print</i> the information presented on the resulting pages.</p>
                            </g:else>
                        </div>
                    </div>
                </section>

			</div>
		</div>
		<r:layoutResources/>
	</body>
</html>
