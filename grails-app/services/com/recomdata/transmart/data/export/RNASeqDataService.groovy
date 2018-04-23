package com.recomdata.transmart.data.export

import com.recomdata.transmart.data.export.util.FileWriterUtil
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues

import javax.annotation.PostConstruct

@CompileStatic
class RNASeqDataService {

	static transactional = false

	private static final List<String> HEADER_NAMES = ['regionname', 'chromosome', 'start',
	                                                  'end', 'num.probes', 'cytoband'].asImmutable()
	private static final Closure<Integer> getReadcount = { RnaSeqValues v -> v.getReadcount() }
	private static final String readcount = 'readcount'

	HighDimensionResource highDimensionResourceService
	HighDimensionDataTypeResource<RegionRow> rnaSeqResource

	@PostConstruct
	void init() {
		/* No way to automatically inject the acgh resource in Spring?
		 * Would be easy in CDI by having a producer of HighDimensionDataTypeResource
		 * beans creating it on the fly by looking at the injection point */
		rnaSeqResource = highDimensionResourceService.getSubResourceForType 'rnaseq'
	}

	void writeRegions(String study, File studyDir, String fileName, String jobName, resultInstanceId) {
		List<AssayConstraint> assayConstraints = createAssayConstraints(study, resultInstanceId as Long)
		Projection projection = rnaSeqResource.createProjection([:], 'rnaseq_values')

		FileWriterUtil writerUtil = null
		TabularResult<AssayColumn, RegionRow> result = null
		try {
			// dataType == 'RNASeq' => file created in a subdir w/ that name
			writerUtil = new FileWriterUtil(studyDir, fileName, jobName, 'RNASeq',
					null, "\t" as char)
			result = rnaSeqResource.retrieveData assayConstraints, [], projection
			doWithResult result, writerUtil
		}
		finally {
			writerUtil?.finishWriting()
			result?.close()
		}
	}

	private void doWithResult(TabularResult<AssayColumn, RegionRow> rnaseqRegionResult,
	                          FileWriterUtil writerUtil) {

		List<AssayColumn> assays = rnaseqRegionResult.indicesList
		String[] header = createHeader(assays)
		writerUtil.writeLine(header as String[])

		String[] templateArray = new String[header.size() + 1]
		//+1 b/c 1st row has no header
		long i = 1 //for the first row

		for (Iterator<RegionRow> iterator = rnaseqRegionResult.rows; iterator.hasNext();) {
			RegionRow row = iterator.next()

			String[] line = templateArray.clone()

			line[0] = i++ as String
			line[1] = row.name as String
			line[2] = row.chromosome as String
			line[3] = row.start as String
			line[4] = row.end as String
			line[5] = row.numberOfProbes as String
			line[6] = row.cytoband

			int j = 7
			for (AssayColumn assay in assays) {
				line[j++] = getReadcount(row[assay]) as String
			}

			writerUtil.writeLine line
		}
	}

	private String[] createHeader(List<AssayColumn> assays) {
		List<String> r = ([] + HEADER_NAMES) as List

		for (AssayColumn assay in assays) {
			r << readcount + "." + assay.patientInTrialId
		}

		r as String[]
	}

	@CompileDynamic
	private List<AssayConstraint> createAssayConstraints(String study, long resultInstanceId) {
		List<AssayConstraint> assayConstraints = [
				rnaSeqResource.createAssayConstraint(
						AssayConstraint.TRIAL_NAME_CONSTRAINT,
						name: study),
				rnaSeqResource.createAssayConstraint(
						AssayConstraint.PATIENT_SET_CONSTRAINT,
						result_instance_id: resultInstanceId),
		]

	}
}
