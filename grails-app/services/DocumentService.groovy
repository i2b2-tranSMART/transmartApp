import com.recomdata.search.DocumentHit
import com.recomdata.search.DocumentQuery
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.transmart.SearchFilter
import org.transmart.SearchKeywordService

/**
 * @author mmcduffie
 */
class DocumentService implements InitializingBean {

	SearchKeywordService searchKeywordService
	GlobalFilterService globalFilterService

	@Value('${com.recomdata.searchengine.index:}')
	private String index

	private DocumentQuery documentQuery

	int documentCount(SearchFilter sfilter) {
		Map<String, List<String>> terms = documentTerms(sfilter)
		Map<String, List<String>> filters = sfilter.documentFilter.filters
		documentQuery.searchCount terms, filters
	}

	List<DocumentHit> documentData(SearchFilter sfilter, GrailsParameterMap params) {
		Map pagingParams = globalFilterService.createPagingParamMap(params)
		Map<String, List<String>> terms = documentTerms(sfilter)
		Map<String, List<String>> filters = sfilter.documentFilter.filters
		DocumentHit[] documents = documentQuery.search(terms, filters, pagingParams.max, pagingParams.offset)
		documents ? documents as List : []
	}

	Map<String, List<String>> documentTerms(SearchFilter sfilter) {

		def gfilter = sfilter.globalFilter
		def geneFilters = gfilter.getGeneFilters()
		def pathwayIds = gfilter.formatIdList(gfilter.getAllListFilters(), ",")
		// If there are pathways, then get all genes in pathways and add them to the geneFilters (hash set)
		if (pathwayIds.size() > 0) {
			geneFilters.addAll(searchKeywordService.expandAllListToGenes(pathwayIds))
		}
		def compoundFilters = gfilter.getCompoundFilters()
		def diseaseFilters = gfilter.getDiseaseFilters()
		def trialFilters = gfilter.getTrialFilters()
		def textFilters = gfilter.getTextFilters()

		Map<String, List<String>> terms = [:]

		int termCount = 0;
		if (geneFilters.size() > 0) {
			def list = getTermList(geneFilters)
			termCount += list.size()
			if (termCount < DocumentQuery.MAX_CLAUSE_COUNT) {
				terms.put(gfilter.CATEGORY_GENE, list)
			}
		}
		if (compoundFilters.size() > 0) {
			def list = getTermList(compoundFilters)
			termCount += list.size()
			if (termCount < DocumentQuery.MAX_CLAUSE_COUNT) {
				terms.put(gfilter.CATEGORY_COMPOUND, list)
			}
		}
		if (diseaseFilters.size() > 0) {
			def list = getTermList(diseaseFilters)
			termCount += list.size()
			if (termCount < DocumentQuery.MAX_CLAUSE_COUNT) {
				terms.put(gfilter.CATEGORY_DISEASE, list)
			}
		}
		if (trialFilters.size() > 0) {
			def list = getTermList(trialFilters)
			termCount += list.size()
			if (termCount < DocumentQuery.MAX_CLAUSE_COUNT) {
				terms.put(gfilter.CATEGORY_TRIAL, list)
			}
		}
		if (textFilters.size() > 0) {
			def list = getTermList(textFilters)
			termCount += list.size()
			if (termCount < DocumentQuery.MAX_CLAUSE_COUNT) {
				terms.put(gfilter.CATEGORY_TEXT, list)
			}
		}

		return terms

	}

	List<String> getTermList(keywords) {

		List<String> terms = []
		for (keyword in keywords) {
			if (terms.size() < DocumentQuery.MAX_CLAUSE_COUNT - 1) {
				terms.add(keyword.keyword)
			}
			else {
				break
			}
		}
		return terms

	}

	// Encode string value for display on HMTL page and encode out-of-band characters.
	String encodeHTML(String value) {
		if (!value) {
			return ""
		}

		value = value.replace("<span class=\"search-term\">", "???HIT_OPEN???")
		value = value.replace("</span>", "???HIT_CLOSE???")
		value = value.encodeAsHTML()
		value = value.replace("???HIT_OPEN???", "<span class=\"search-term\">")
		value = value.replace("???HIT_CLOSE???", "</span>")

		StringBuilder result = new StringBuilder()

		if (value.length() > 0) {
			def len = value.length() - 1
			for (i in 0..len) {
				def int ch = value.charAt(i)
				if (ch < 32) {
					result.append(' ')
				}
				else if (ch >= 128) {
					result.append("&#")
					result.append(ch)
				}
				else {
					result.append((char) ch)
				}
			}
		}

		result
	}

	void afterPropertiesSet() {
		documentQuery = new DocumentQuery(index)
	}
}
