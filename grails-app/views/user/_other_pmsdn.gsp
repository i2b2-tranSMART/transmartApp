<g:set var='tosTextSettingValue' value="${auth.settingValue(name: 'tos.text')}" />
<g:set var='tosTextSettingLastUpdated' value="${auth.setting(name: 'tos.text')?.lastUpdated}" />
<g:set var='tosTextContValue' value="${auth.settingValue(name: 'tos.text_cont')}" />



<div class="form-section">
	<div class="col-xs-12">
		<h2>Terms of Service</h2>
		<g:if test="${!tosTextSettingValue}">Not yet configured.</g:if>
		<g:else>
		<p>Effective date: <b><g:formatDate format='MMM dd, yyyy' date="${tosTextSettingLastUpdated}"/></b></p>
		${tosTextSettingValue + tosTextContValue}
		</g:else>
	</div>
	<div class="col-xs-12">
		<br />
	</div>
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
