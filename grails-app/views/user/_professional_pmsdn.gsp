<g:set var='usertypeOptions' value="${['Academia', 'Government', 'Industry (Biotech, Pharma)', 'Non-Profit Research Organization', 'Independent Researcher', 'Other']}" />



<div class="form-section">
	<div class="col-xs-12">
		<label for="usertype">Data User Type<sup>*</sup></label>
		<g:select name='usertype' class='form-control' value="${user.usertype}" from="${usertypeOptions}" />
		<div id="fldOtherUserType" style="display: none">
			<br />
			<g:textField name='fldOtherUserType' maxlength='35' size='35' placeholder='Add your usertype here.' required='required' value="${user.fldOtherUserType}" />
			<br /><br/>
		</div>
		During this pilot phase of the PMS_DN, applicants for aggregate data access will be limited to research professionals from academia, government, industry, non-profit research organizations, and independent researchers. If the applicant has questions about their qualifications, please contact <a href="mailto:support-pmsdn@googlegroups.com">support-pmsdn@googlegroups.com</a>.
	</div>
	<div class="col-xs-12">
		<br />
	</div>
	<div class="col-xs-6">
		<label for="degree">Degree(s)<sup>*</sup></label>
		<g:textField class='form-control' name='degree' required='required' value="${user.degree}" />
	</div>
	<div class="col-xs-6">
		<label for="title">Professional Title<sup>*</sup></label>
		<g:textField class='form-control' name='title' required='required' value="${user.title}" />
	</div>
	<div class="col-xs-12">
		<br />
	</div>
	<div class="col-xs-6">
		<label for="organization">Affiliation/Organization<sup>*</sup></label>
		<g:textField class='form-control' name='organization' required='required' value="${user.organization}" />
	</div>
	<div class="col-xs-6">
		<label for="department">Department<sup>*</sup></label>
		<g:textField class='form-control' name='department' required='required' value="${user.department}" />
	</div>
	<div class="col-xs-12">
		<br />
	</div>
	<div class="col-xs-6">
		<label for="focus">Research areas of focus<sup>*</sup></label>
		<g:textField class='form-control' name='focus' required='required' value="${user.focus}" />
	</div>
	<div class="col-xs-6">
		<label for="topic">Research topic related to PMS<sup>*</sup></label>
		<g:textField class='form-control' name='topic' required='required' value="${user.topic}" />
	</div>
	<div class="col-xs-12">
		<br />
	</div>
</div>
