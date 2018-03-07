<div class="form-section">
	<g:if test="${useRecaptcha}">
	<div class="col-xs-6">
		<label>Verification</label>
		<div class="g-recaptcha" data-sitekey="${captchaSitekey}"></div>
	</div>
	</g:if>
	<div class="col-xs-12">
		<br />
	</div>
</div>
