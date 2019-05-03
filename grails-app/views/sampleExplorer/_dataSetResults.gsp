<div style="background-color:#EEEEEE;height:100%;margin-left: auto;margin-right: auto;overflow: hidden;">
	<div id="cohortInformation">
		<g:if test="${includeCohortInformation == true}">
			<g:render template="sampleStatistics" model="[sampleSummary:sampleSummary]" />
		</g:if>
	</div>
	<div id="divDataSetResults"></div>
	<div id="divDataSetResults2"></div>
	<div id="site_content"></div>
</div>
