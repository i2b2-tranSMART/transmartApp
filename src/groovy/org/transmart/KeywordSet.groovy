package org.transmart

import groovy.transform.CompileStatic
import org.transmart.searchapp.SearchKeyword

/**
 * @author mmcduffie
 */
@CompileStatic
class KeywordSet extends LinkedHashSet<SearchKeyword> {

	List<String> getKeywordUniqueIds() {
		List<String> uidlist = []
		for (SearchKeyword keyword in this) {
			uidlist << keyword.uniqueId
		}
		uidlist
	}

	List<Long> getKeywordDataIds() {
		List<Long> bioids = []
		for (SearchKeyword keyword in this) {
			bioids << keyword.bioDataId
		}
		bioids
	}

	String getKeywordDataIdString() {
		StringBuilder s = new StringBuilder()

		for (keyword in this) {
			if (s) {
				s << ", "
			}
			s << keyword.bioDataId
		}

		s
	}

	boolean removeKeyword(SearchKeyword keyword) {
		for (SearchKeyword k in this) {
			if (k.uniqueId == keyword.uniqueId) {
				return remove(k)
			}
		}
		false
	}
}
