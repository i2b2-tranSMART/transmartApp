<html>
	<head>
		<link rel='stylesheet' type='text/css' href='${resource(dir:'css', file:'chartservlet.css')}' />
	</head>
	<body>
		<div class='sampleStatisticsTitle'>Sample Statistics</div>
		
		<div class='sampleDataBox'>${sampleSummary.queryDefinition}</div>
		
		<g:if test="${sampleSummary.size() > 1}">
			<div class='sampleDataBox'>
				<table class='analysis'>
					<tr>
						<th>
							Count Information
						</th>
					</tr>
					<g:each in="${sampleSummary}">
						<g:if test="${it.key != 'queryDefinition'}">
							<tr>
								<td>
									${it.key} : ${it.value} 
								</td>
							</tr>     			
						</g:if>
					</g:each>											
				</table>
			</div>
		</g:if>
	</body>
</html>