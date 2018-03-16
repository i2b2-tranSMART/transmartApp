package org.transmart

import groovy.transform.CompileStatic
import org.transmart.searchapp.SearchKeyword

/**
 * @author mmcduffie
 */
@CompileStatic
class GlobalFilter {

	private static final String CATEGORY_GENE = 'GENE'
	private static final String CATEGORY_PATHWAY = 'PATHWAY'
	private static final String CATEGORY_COMPOUND = 'COMPOUND'
	private static final String CATEGORY_DISEASE = 'DISEASE'
	private static final String CATEGORY_TRIAL = 'TRIAL'
	private static final String CATEGORY_TEXT = 'TEXT'
	private static final String CATEGORY_STUDY = 'STUDY'
	private static final String CATEGORY_GENE_SIG = 'GENESIG'
	private static final String CATEGORY_GENE_LIST = 'GENELIST'

	private Map<String, KeywordSet> categoryFilterMap = [:]

	boolean isEmpty() {
		for (KeywordSet value in categoryFilterMap.values()) {
			if (value) {
				return false
			}
		}

		true
	}

	boolean isTextOnly() {
		boolean hasText = false
		for (String key in categoryFilterMap.keySet()) {
			if (categoryFilterMap[key]) {
				if (key == CATEGORY_TEXT) {
					hasText = true
				}
				else {
					return false
				}
			}
		}

		hasText
	}

	boolean containsFilter(SearchKeyword filter) {
		categoryFilterMap[filter.dataCategory].contains filter
	}

	KeywordSet getBioMarkerFilters() {
		KeywordSet all = new KeywordSet()
		all.addAll getGeneFilters()
		all.addAll getPathwayFilters()
		all.addAll getGeneSignatureFilters()
		all.addAll getGeneListFilters()
		all
	}

	boolean hasAnyListFilters() {
		getPathwayFilters() || getGeneSignatureFilters() || getGeneListFilters()
	}

	KeywordSet getGenePathwayFilters() {
		KeywordSet all = new KeywordSet()
		all.addAll getGeneFilters()
		all.addAll getPathwayFilters()
		all
	}

	KeywordSet getAllListFilters() {
		KeywordSet all = new KeywordSet()
		all.addAll getPathwayFilters()
		all.addAll getGeneSignatureFilters()
		all.addAll getGeneListFilters()
		all
	}

	KeywordSet getGeneFilters() {
		findFiltersByCategory CATEGORY_GENE
	}

	KeywordSet getGeneSignatureFilters() {
		findFiltersByCategory CATEGORY_GENE_SIG
	}

	KeywordSet getGeneListFilters() {
		findFiltersByCategory CATEGORY_GENE_LIST
	}

	KeywordSet getGeneSigListFilters() {
		KeywordSet all = new KeywordSet()
		all.addAll getGeneSignatureFilters()
		all.addAll getGeneListFilters()
		all
	}

	KeywordSet getPathwayFilters() {
		findFiltersByCategory CATEGORY_PATHWAY
	}

	KeywordSet getDiseaseFilters() {
		findFiltersByCategory CATEGORY_DISEASE
	}

	KeywordSet getTrialFilters() {
		findFiltersByCategory CATEGORY_TRIAL
	}

	KeywordSet getStudyFilters() {
		findFiltersByCategory CATEGORY_STUDY
	}

	KeywordSet getCompoundFilters() {
		findFiltersByCategory CATEGORY_COMPOUND
	}

	KeywordSet getTextFilters() {
		findFiltersByCategory CATEGORY_TEXT
	}

	KeywordSet getAllFilters() {
		KeywordSet filters = getGeneFilters()
		filters.addAll getPathwayFilters()
		filters.addAll getCompoundFilters()
		filters.addAll getDiseaseFilters()
		filters.addAll getTrialFilters()
		filters.addAll getTextFilters()
		filters.addAll getStudyFilters()
		filters.addAll getGeneSignatureFilters()
		filters.addAll getGeneListFilters()
		filters
	}

	/**
	 * returns a list of keywords for given category or an empty list if not present
	 */
	KeywordSet findFiltersByCategory(String category) {
		KeywordSet filters = categoryFilterMap[category]
		if (filters == null) {
			filters = new KeywordSet()
			categoryFilterMap[category] = filters
		}
		(KeywordSet) filters.clone()
	}

	// Returns list of keywords for keywordset. Useful for building 'in' clauses or search terms.
	String formatKeywordList(KeywordSet set, String separator, String textQualifier, int maxLength) {
		String list = ''
		for (filter in set) {
			String s = ''
			if (list && separator) {
				s = separator
			}
			if (textQualifier) {
				s += textQualifier
			}
			s += filter.keyword
			if (textQualifier) {
				s += textQualifier
			}
			if (maxLength && list.length() + s.length() > maxLength) {
				break
			}
			list += s
		}
		list
	}

	// Returns list of bioDataIds for specified category. Useful for building 'in' clauses.
	String formatIdList(KeywordSet set, String separator) {
		StringBuilder list = new StringBuilder()
		for (filter in set) {
			if (list && separator) {
				list << separator
			}
			list << filter.bioDataId
		}
		list
	}

	void addKeywordFilter(SearchKeyword keyword) {
		KeywordSet klist = categoryFilterMap[keyword.dataCategory]
		if (klist == null) {
			// make sure no dup
			klist = new KeywordSet()
			categoryFilterMap[keyword.dataCategory] = klist
		}
		klist << keyword
	}

	void removeKeywordFilter(SearchKeyword keyword) {
		categoryFilterMap[keyword.dataCategory]?.removeKeyword keyword
	}

	boolean hasPathway() {
		getPathwayFilters()
	}
}
