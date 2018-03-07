<g:set var='level2Request' value="${auth.userSettingValue('level2.request')}"/>
<br/>
<br/>
<div class="row">
	<div class="col-lg-12">
		<auth:ifLevelOne>
		<p>Your current access level is <b>Level 1</b> access.</p>
		<g:if test="${!level2Request}">
		<p>Investigators have two levels of access. Level 1 users can generate summary statistics of patients’ de-identified patient reported outcomes and from knowledge extracted from clinical notes to develop new hypotheses about PMS. Level 2 users may see and download de-identified patient-level data, including curated genetic data, which is not viewable in Level 1. They can also view the de-identified sentences where the NLP engine extracted knowledge from the clinical notes. Level 2 users can opt to cross check the knowledge extracted by the NLP against the sentences from which they were extracted as part of a knowledge validation workflow.</p>
		<p>To request <b>Level2</b> access, complete and submit the five-item <i>Level 2 request form</i>.<br/> <br/>
		<g:link controller='user' action='access' class='btn btn-info'>Level 2 Access Request Form</g:link><br/> <br/>
		The Data Network Specialist will then contact you to discuss your study and provide the Data Access Application.<br/> <br/>
		Each applicant must submit an application that includes
		</p>
		<ul>
			<li>a technical proposal</li>
			<li>a lay summary of the work</li>
			<li>the applicant’s CV</li>
			<li>and a copy of the approval or exemption letter from your current Institutional Review Board (IRB), or showing the study was determined to be non-Human Subjects Research by the IRB. Applications can be reviewed without IRB approval, however IRB approval or exemption is required prior Level 2 approval.</li>
		</ul>
		<p>For questions regarding any of the application processes described above, please contact the Data Network Specialist at <a href="mailto:support-pmsdn@googlegroups.com">support-pmsdn@googlegroups.com</a>.</p>
		</g:if>
		<g:else>
		<g:if test="${level2Request == 'sent'}">
		<p>You have requested Level2 access and it is still under review.<br/><br/>
			You will be notified via e-mail, by the administrator, once you have been granted access, or if additional information is required.</p>
		</g:if>
		</g:else>
		</auth:ifLevelOne>
		<auth:ifLevelTwo>
		<p>Your current access level is <b>Level 2</b> access.</p>
		<p>This means that you are able to run <i>summary statistics</i> and <i>advanced statistics</i> queries, you can also see <i>patient level data</i> and <i>download or print</i> the information presented on the resulting pages.</p>
	</auth:ifLevelTwo>
	<auth:ifLevelAdmin>
	<p>Your current access level is <b>Administrator</b> access.</p>
	<p>This means that you can remove users, change their access levels and complete all the tasks that Level1 and Level2 users can.</p>
	</auth:ifLevelAdmin>
	</div>
</div>
