<div class="form-section">
	<g:if test="${!user.email ||  !user.email.contains('@') || params.controller == 'user'}">
	%{--E-mail is not really an e-mail, so we need to ask for it.--}%
	<div class="col-xs-12">
		<label for="email">E-mail<sup>*</sup></label>
		<g:field class='form-control' name='email' required='required' type='email' value="${user.email}" />
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
	<div class="col-xs-6">
		<label for="firstname">First Name<sup>*</sup></label>
		<g:textField class='form-control' name='firstname' required='required' value="${user.firstname}" />
	</div>
	<div class="col-xs-6">
		<label for="lastname">Last Name<sup>*</sup></label>
		<g:textField class='form-control' name='lastname' required='required' value="${user.lastname}" />
	</div>
	<div class="col-xs-12">
		<br />
	</div>
	<div class="col-xs-12">
		<label for="phone">Phone<sup>*</sup></label>
		<g:field class='form-control' name='phone' required='required' value="${user.phone}" type='tel' />
	</div>
</div>
