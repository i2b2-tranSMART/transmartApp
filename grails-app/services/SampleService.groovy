import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsApplication

import javax.sql.DataSource

@Slf4j('logger')
class SampleService {

	DataSource dataSource
	I2b2HelperService i2b2HelperService
	GrailsApplication grailsApplication
	SolrService solrService

	static transactional = false

	//Populate the QT_PATIENT_SAMPLE_COLLECTION table based on a result_instance_id.
	void generateSampleCollection(String resultInstanceId) {
		String sql = '''
				INSERT INTO I2B2DEMODATA.QT_PATIENT_SAMPLE_COLLECTION (SAMPLE_ID, PATIENT_ID, RESULT_INSTANCE_ID)
				SELECT DISTINCT DSSM.SAMPLE_ID, DSSM.patient_id, ?
				FROM I2B2DEMODATA.QT_PATIENT_SET_COLLECTION QT
				INNER JOIN DEAPP.DE_SUBJECT_SAMPLE_MAPPING DSSM ON DSSM.PATIENT_ID = QT.PATIENT_NUM
				WHERE RESULT_INSTANCE_ID = ?'''
		new Sql(dataSource)l.execute(sql, [resultInstanceId.toInteger(), resultInstanceId.toInteger()])
	}

	Map loadSampleStatisticsObject(String resultInstanceId) {
		Map sampleSummary = [:]

		StringWriter writer = new StringWriter()

		i2b2HelperService.renderQueryDefinition resultInstanceId, 'Query Definition', new PrintWriter(writer)
		sampleSummary.queryDefinition = writer.toString()

		grailsApplication.config.edu.harvard.transmart.sampleBreakdownMap.each { key, value ->

			sampleSummary[value] = solrService.getFacetCountForField(key, resultInstanceId, 'sample')

			logger.debug 'Finished count for field {} - {}: {}', value, key, sampleSummary[currentCountVariable.value]
		}

		sampleSummary
	}
}
