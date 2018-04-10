package com.recomdata.transmart.data.export

import com.recomdata.transmart.domain.i2b2.AsyncJob
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value

@Slf4j('logger')
class SweepingService {

	@Value('${com.recomdata.export.jobs.sweep.fileAge:0}')
	private int fileAge

	@Transactional
	void sweep() {
		logger.info 'Triggering file sweep'
		def now = new Date()
		def jobList = AsyncJob.createCriteria().list {
			eq("jobType", "DataExport")
			eq("jobStatus", "Completed")
			lt('lastRunOn', now - fileAge)
			//between('lastRunOn',now-fileAge, now)
		}

		DeleteDataFilesProcessor processor = new DeleteDataFilesProcessor()
		for (job in jobList) {
			if (processor.deleteDataFile(job.viewerURL, job.jobName)) {
				job.delete()
			}
		}
	}
}
