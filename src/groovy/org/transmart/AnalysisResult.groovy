package org.transmart

import org.transmart.biomart.BioAssayAnalysis

/**
 * @author mmcduffie
 * */
class AnalysisResult implements Comparable<AnalysisResult> {

	// TEA metrics
	Double teaScore
	boolean bTeaScoreCoRegulated = false
	boolean bSignificantTEA = false
	int defaultTop = 5

	BioAssayAnalysis analysis
	def experimentId
	def experimentAccession
	List assayAnalysisValueList = [] // collection of AssayAnalysisValue objects
	Long bioMarkerCount = 0

	int size() {
		return assayAnalysisValueList.size()
	}

	String getGeneNames() {
		if (!assayAnalysisValueList) {
			return null
		}

		StringBuilder s = new StringBuilder()
		Set nameSet = []
		// remove dup first
		for (value in assayAnalysisValueList) {
			def marker = value.bioMarker
			if (marker.isGene()) {
				nameSet.add(marker.name)
			}
		}

		for (name in nameSet) {
			if (s) {
				s.append(", ")
			}
			s.append(name)
		}

		s
	}

	boolean showTop() {
		// bioMarkerCount was populated only when it's NOT searching for genes
		return bioMarkerCount > defaultTop
	}

	def getAnalysisValueSubList() {

		if (showTop()) {
			def total = defaultTop
			if (assayAnalysisValueList.size() <= defaultTop) {
				total = assayAnalysisValueList.size()
			}
			if (total < 0) {
				total = 0
			}

			return assayAnalysisValueList.subList(0, total)
		}
		else {
			// show all
			return assayAnalysisValueList
		}
	}

	/**
	 * comparable interface implementation, sort on TEAScore
	 */
	int compareTo(AnalysisResult compare) {
		// compare objects
		Double thisScore = teaScore
		Double compScore = compare.teaScore

		// handle invalid values
		if (compScore == null && thisScore != null) {
			return 1
		}
		if (thisScore == null && compScore != null) {
			return -1
		}
		if (thisScore == null && compScore == null) {
			return 0
		}

		// if score is the same, sort on biomarker ct (desc)
		if (thisScore == compScore) {
			return (-1 * assayAnalysisValueList.size().compareTo(compare.assayAnalysisValueList.size()))
		}
		else {
			return (thisScore.compareTo(compScore))
		}
	}

	/**
	 * the TEA score is calculated as -log(teaScore) for UI purposes
	 */
	Double calcDisplayTEAScore() {
		if (teaScore != null) {
			-Math.log(teaScore.doubleValue())
		}
	}
}
