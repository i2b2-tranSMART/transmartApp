<g:set var='studyApprovalOptions' value="${['Approved', 'Exempt', 'Pending', 'Not yet applied']}" />
<g:set var='studyFundingOptions' value="${['Funded Grant', 'Grant Application', 'N/A']}" />
<g:set var='level2Request' value="${auth.userSettingValue('level2.request')}" />



<html>
<head>
<meta name="layout" content="bootstrap" />
<title>User Profile</title>
<style>
body {
	padding-top: 70px;
	background-color: white;
}
.btn-file {
	position: relative;
	overflow: hidden;
}
.btn-file input[type=file] {
	position: absolute;
	top: 0;
	right: 0;
	min-width: 100%;
	min-height: 100%;
	font-size: 100px;
	text-align: right;
	filter: alpha(opacity = 0);
	opacity: 0;
	outline: none;
	background: white;
	cursor: inherit;
	display: block;
}
</style>
</head>
<body>
	<sec:ifAnyGranted roles='ROLE_ADMIN'>
	<g:render template='/layouts/navbar_admin' />
	</sec:ifAnyGranted>
	<sec:ifNotGranted roles='ROLE_ADMIN'>
	<g:render template='/layouts/navbar_user' />
	</sec:ifNotGranted>
	<g:if test="${!level2Request}">
	<div class="container well">
		<h1>PMSDN Level2 Access Request</h1>
		<p>Please ensure that your profile information is up to date.</p>
		<g:uploadForm controller='user' action='access'>
			<div class="form-group">
				<label for="research_title">Title of research initiative<sup>*</sup></label>
				<g:textField class='form-control' name='research_title' required='required' />
			</div>
			<div class="form-group">
				<label for="study_pi">PI of study (if other than the person requesting access)</label>
				<g:textField class='form-control' name='study_pi' required='required' value="${user.description}" /> %{--TODO user.description cannot be correct here--}%
			</div>
			<div class="form-group">
				<label for="study_funding">Is the study associated with a currently funded grant or grant application? <sup>*</sup></label>
				<g:select name='study_funding' class='form-control' from="${studyFundingOptions}" />
			</div>
			<div class="form-group">
				<label for="funding_source">What is the funding source?<sup>*</sup></label>
				<g:textArea name='funding_source' required='required' class='form-control' rows='5'
				            placeholder='In max. 600 characters, please describe the source of funding for the study.' />
			</div>
			<div class="form-group">
				<label for="study_approval">Do you currently have IRB approval or exception? <sup>*</sup></label>
				<g:select name='study_approval' class='form-control' from="${studyApprovalOptions}" />
			</div>
			<br />
			<a href="${createLink(controller: 'user', action: 'index')}#tab_accesslevel" class="btn btn-warning pull-left">&lt;&lt; Back</a>
			<button type="submit" class="btn btn-info pull-right">Submit Request</button>
		</g:uploadForm>
	</div>
	</g:if>
	<g:else>
	<div class="container well">
		<h1>Your request has been submitted.</h1>
		<br />
		<p>The Data Network Specialist will contact you to discuss your study and provide the Data Access Application. <br /><br />
			Each applicant must submit an application that includes a technical proposal, a lay summary of the work, the applicant’s CV, and a copy of the approval or exemption letter from your current Institutional Review Board (IRB), or showing the study was determined to be non-Human Subjects Research by the IRB.<br /><br />
			Applications can be reviewed without IRB approval, however IRB approval or exemption is required prior to the Level2 approval.</p>
		<p>For questions regarding any of the application processes described above, please contact the Data Network Specialist at <a href="mailto:support-pmsdn@googlegroups.com">support-pmsdn@googlegroups.com</a>.</p>
	</div>
	</g:else>
	<content tag="javascript"><script>
		$(document).ready(function() {
			$('.btn-file :file').on('fileselect', function(event, numFiles, label) {
				var input = $(this).parents('.input-group').find(':text')
				log = numFiles > 1 ? numFiles + ' files selected' : label;
				if (input.length) {
					input.val(log);
				}
				else {
					if (log) {
						$('#uploaded_file_name').html('File ' + log + ' is selected.')
					}
				}
			});
		});
		$(document).on('change', '.btn-file :file', function() {
			var input = $(this), numFiles = input.get(0).files ? input
					.get(0).files.length : 1, label = input.val()
					.replace(/\\/g, '/').replace(/.*\//, '');
			console.log(input);
			input.trigger('fileselect', [ numFiles, label ]);
		});
	</script></content>
</body>
</html>
