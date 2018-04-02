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
		<link rel="stylesheet" type="text/css" href="${resource(dir:'css', file: 'userProfile.css')}">

	</head>
	
	
	
	<body>
		<div id="header-div">
		    <g:render template="/layouts/commonheader" model="['app': '']"/>
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
				<label class="tabLabel" for="tab2">Professional</label>
			    
				<input class="profileTab" id="tab3" type="radio" name="tabs">
				<label class="tabLabel" for="tab3">Access Level</label>
				
				<input class="profileTab" id="tab4" type="radio" name="tabs">
				<label class="tabLabel" for="tab4">IRCT Token</label>

				<section id="content1">
					<g:form action="save">
						<br />
						<g:render template="/user/personal" />
						<br/>
						<div class="buttons" style="margin-top:20px; margin-bottom:20px">
				            <span class="button">
				                <input class="save" type="submit" value="Save"/>
				            </span>
				        </div>
					</g:form>

				</section>
				<section id="content2">
					<g:form action="save">
						<br/>
						<g:render template="/user/professional" />
						<br/>
						<div class="buttons" style="margin-top:20px; margin-bottom:20px">
							<span class="button">
								<input class="save" type="submit" value="Save"/>
							</span>
						</div>
					</g:form>

				</section>
				<section id="content3">
					<g:render template="/user/access_level"/>
				</section>
			    <section id="content4">
					<g:if test="${level > org.transmart.plugin.auth0.UserLevel.ONE}">
						<br /><textarea rows="10" style="width:100%">${token}</textarea>
					</g:if>
					<g:else>
						Token access it not available for your level of access.
					</g:else>
				</section>
			</div>
		</div>
		<script>
            $(document).ready(function(){
                setTimeout(function() {
                    $('#flashMessage').fadeOut('fast');
                }, 5000);
            });
		</script>
		<r:layoutResources/>
		<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
	</body>
</html>
