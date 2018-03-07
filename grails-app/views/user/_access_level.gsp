<h3 class="page-header">Access Level</h3>
<div class="row">
	<div class="col-lg-12">
		<auth:ifLevelOne>
			<p>Your current access level is <b>Open Data Access (Level 1)</b>.</p>
			<p>This means that you are able to run <i>summary statistics</i> and <i>advanced statistics</i> queries, but you cannot see or download <b><i>patient-level data</i>
			</b>.</p>
			<p>The process to request <b>Controlled Data Access (Level 2)</b> access is as follows:
			<ul>
				<li>Ensure that your <b>Profile</b> information is up-to-date.</li>
				<li>Fill out the <b>Access Request Form</b>.</li>
			</ul>
			</p>
			<p>After completing the above steps, a confirmation e-mail will be sent to you and the administrator will be notified of your request. The administrator will review the form and will coordinate its submission to the Data Access Committee.</p>
			<br/>
			<g:link controller='user' action='access' class='btn btn-info'>Level 2 Access Request Form</g:link>
		</auth:ifLevelOne>
		<auth:ifLevelTwo>
			<p>Your current access level is <b>Controlled Data Access (Level 2)</b>.</p>
			<p>This means that you are able to run <i>summary statistics</i> and <i>advanced statistics</i> queries, you can also see <i>patient level data</i> and <i>download or print</i> the information presented on the resulting pages.</p>
		</auth:ifLevelTwo>
	</div>
</div>
