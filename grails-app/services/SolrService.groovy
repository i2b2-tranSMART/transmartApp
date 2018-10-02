import PatientSampleCollection
import groovy.util.logging.Slf4j
import groovyx.net.http.HTTPBuilder
import org.apache.http.client.HttpResponseException
import groovyx.net.http.HttpResponseDecorator
import org.jfree.util.Log
import org.springframework.beans.factory.annotation.Value

import static groovyx.net.http.ContentType.URLENC

@Slf4j('logger')
class SolrService {

	private static final List<String> ESCAPE_CHARS = ['\\', '+', '-', '!', '(', ')', '{', '}',
	                                                  '[', ']', '^', '"', '~', '*', '?', ':'].asImmutable()

	static transactional = false

	@Value('${com.recomdata.solr.baseURL:}')
	private String solrServerUrl

	@Value('${com.recomdata.solr.maxRows:0}')
	private int solrMaxRows

	/**
	 * Runs a faceted search on the term provided and return a map with map[term]=facet_count
	 * @param solrServer Base URL for the solr server.
	 * @param fieldList "|" separated list of fields.
	 */
	Map facetSearch(json, Map fieldMap, String coreName) {

		String solrQuery = generateSolrQueryFromJson(json, false)

		//If the query is empty, change it to be 'return all results' in Solr language.
		if (solrQuery == '(())') {
			solrQuery = '*:*'
		}

		Map<String, String> counts = [:]

		// map of {termType: {term : (count)}} that is passed to the view.
		Map termMap = [:]

		//This holds the map till we can get it into the parent map.
		def tempMap = [:]

		for (currentTerm in fieldMap.columns) {

			Map args = [path: '/solr/' + coreName + '/select/',
			            query: [q: solrQuery,
			                    facet: 'true',
			                    'facet.field': currentTerm.dataIndex,
			                    'facet.sort': 'index']]
//			querySolr(args) { xml ->
			def xml = querySolr(args)
			xml.lst.each
			{ outerlst ->

				//We only want the fact_counts node.
				if(outerlst.@name=='facet_counts')
				{
					//Under this we only want the facet_fields node.
					outerlst.lst.each
					{ innerlst ->

						if(innerlst.@name=='facet_fields')
						{
							innerlst.lst.each
							{ innermostlst ->

								//Find the node whose "name" is our term.
								if(innermostlst.@name==currentTerm.dataIndex)
								{
									innermostlst.int.each
									{ termItem ->

										//To the temp map add an entry with the current term name and the count of documents found.
										tempMap[termItem.@name.toString()] = termItem.toString()
									}
								}
							}
						}
					}
				}
			}
			//}

//			//The term map goes under the current category.
//			termMap[currentTerm.dataIndex] = [counts: counts, displayName: currentTerm.header]
//
//			counts.clear()
			//The term hash goes under the current category.
			termMap[currentTerm.dataIndex] = [:]
			termMap[currentTerm.dataIndex].counts = tempMap
			termMap[currentTerm.dataIndex].displayName = currentTerm.header

			termMap[currentTerm.dataIndex]['type'] = currentTerm.get('type','string')
			termMap[currentTerm.dataIndex]['group'] = currentTerm.get('group','string')


			//Reinitialize the temporary map variable.
			tempMap = [:]
		}

		termMap
	}

	/**
	 * Takes a map in an expected solr layout category:[item:count] and reorders
	 * it so that the item specified by the second parameter is on top of that categories list.
	 * @param mapToModify A map in the expected solr format category:[item:count,item:count]
	 * @param termToFloat the name of the item that should be preserved on top.
	 */
	Map floatTopValue(Map mapToModify, String termToFloat) {
		for (termList in mapToModify) {
			def valueRemoved = termList.value.remove(termToFloat)
			if (valueRemoved) {
				termList.value = [(termToFloat): valueRemoved] + termList.value
			}
		}

		mapToModify
	}

	/**
	 * Pulls 'documents' from solr based on the passed in JSON Criteria.
	 * @param json looks like {"SearchJSON":{"Pathology":["Liver, Cancer of","Colorectal Cancer"]}}
	 * @param resultColumns columns to return
	 */
	Map pullResultsBasedOnJson(json, String resultColumns, boolean enforceEmpty, String coreName) {
		String solrQuery
		String tokenizerDelimiter = "`````"
		if (json.Records) {
			solrQuery = generateSolrQueryFromJsonDetailed(json, enforceEmpty)
		} else {
			solrQuery = generateSolrQueryFromJson(json, enforceEmpty)
		}

		//If the query is empty, abort here.
		if (solrQuery == '(())') {
			return [results: []]
		}

		//Construct the rest of the query based on the columns we want back and the number of rows we want.
		solrQuery += '&fl=' + resultColumns + '&sort=id desc&rows=' + solrMaxRows

		logger.debug 'pullResultsBasedOnJson - solr Query to be run: {}', solrQuery

		//We want [results:[{'Pathology':'blah','Tissue':'blah'},{'Pathology':'blah','Tissue':'blah'}]]

		//This will be the hash to store our results.
		Map results = [:]

		Map args = [path: '/solr/' + coreName + '/select/',
		            query: [q: solrQuery]]
		def xml = querySolr(args)


		print "Result doc: " + xml.result.doc.size()
		for (resultDoc in xml.result.doc) {
			//This string will hold the text for each column in the output.
			String resultConcat = ""

			for (stringResult in resultDoc.str) {
				//If this isn't the first column add a seperator.
				if(resultConcat!="") resultConcat+="|"
				//Add tag name : tag value to the hash key.
				print "STRING NAME: " + stringResult.@name.toString()
				resultConcat += stringResult.@name.toString() + tokenizerDelimiter + stringResult.toString()
			}
			for (longResult in resultDoc.long) {
				if(resultConcat!="") resultConcat+="|"
				resultConcat += longResult.@name.toString() + tokenizerDelimiter + longResult.toString()
			}

			for (floatResult in resultDoc.float) {
				if(resultConcat!="") resultConcat+="|"
				resultConcat += floatResult.@name.toString() + tokenizerDelimiter + floatResult.toString()
			}

			for (dateResult in resultDoc.date) {
				if(resultConcat!="") resultConcat+="|"
				resultConcat += dateResult.@name.toString() + tokenizerDelimiter + dateResult.toString()
			}

			for (intResult in resultDoc.int) {
				if(resultConcat!="") resultConcat+="|"
				resultConcat += intResult.@name.toString() + tokenizerDelimiter + intResult.toString()
			}

			//If a hash entry doesn't exist for the data concat'ed together, create one.
			if(!results[resultConcat]) results[resultConcat] = 0;

			//Increment the hash for our concat'ed string.
			results[resultConcat]++
		}


		//This will be the final hash we pass out of this function.
		Map finalHash = ['results':[]];

		//Now that we have this ugly hash we have to convert it to a meaningful hash that can be parsed into JSON.
		for (result in results) {


			//We build a hash with an entry for each field, and the value for that field.
			def tempHash = [:];

			//For each of the keys break on the "|" character.
			for (resultKey in result.key.toString().tokenize("|")) {
				//Within each "|" there is a funky set of characters that delimits the field:value.
				String[] keyValueBreak = resultKey.split(tokenizerDelimiter)

				//Add the key/value to the hash.
				tempHash[keyValueBreak[0]] = keyValueBreak[1]
			}

			//Each value of the parent hash is actually a count of how many of items matching the key were found.
			tempHash['count'] = result.value;

			//Add this category to the final hash.
			finalHash['results'].add(tempHash);

		}
//		{ hashKey ->
//
//			//We build a hash with an entry for each field, and the value for that field.
//			def tempHash = [:];
//
//			//For each of the keys break on the "|" character.
//			hashKey.key.toString().tokenize("|").each
//			{
//				//Within each "|" there is a funky set of characters that delimits the field:value.
//				def keyValueBreak = it.tokenize(tokenizerDelimiter);
//
//				//Add the key/value to the hash.
//				tempHash[keyValueBreak[0]] = keyValueBreak[1];
//			}
//
//			//Each value of the parent hash is actually a count of how many of items matching the key were found.
//			tempHash['count'] = hashKey.value;
//
//			//Add this category to the final hash.
//			finalHash['results'].add(tempHash);
//		}

		//Return the results hash.
		return finalHash
	}

	/**
	 * Runs a solr 'terms' query with a prefix on the provided list of columns and return some results.
	 * @param solrServer Base URL for the solr server.
	 * @param fieldList ',' separated list of fields that we search for the term within.
	 * @param termPrefix search for values like this prefix.
	 * @return map that looks like [Pathology:[SomeDisease:22,SomeOtherDisease:33],Tissue:[Skin:32]]
	 */
	def suggestTerms(String fieldList, String termPrefix, String numberOfSuggestions, coreName) {

		List<Map> rows = []
		Map result = [rows: rows]

		if (termPrefix) {
			Map args = [path: '/solr/' + coreName + '/suggest',
			            query: ['spellcheck.q': termPrefix,
			                    'spellcheck.count': '10']]
//			querySolr(args) { xml ->
			def xml = querySolr(args)
			for (outerlst in xml.lst) {
				if (outerlst?.@name == 'spellcheck') {
					for (innerlst in outerlst.lst.lst.arr) {
						for (termItem in innerlst.str) {
							rows << [id:  'STR|' + termItem,
									 source: '',
									 keyword: termItem.toString(),
									 synonyms: '',
									 category: 'STR',
									 display: 'Term']
						}
					}
				}
			}
//			}
		}

		result
	}

	/**
	 * Based on the JSON object, run a query and return only the IDs.
	 * @param solrServer Base URL for the solr server.
	 * @param json looks like {"SearchJSON":{"Pathology":["Liver, Cancer of","Colorectal Cancer"]}}
	 */
	List<String> getIDList(json, String coreName) {
		String solrQuery
		if (json.Records) {
			solrQuery = generateSolrQueryFromJsonDetailed(json, false)
		}
		else {
			solrQuery = generateSolrQueryFromJson(json, false)
		}

		//If the query is empty, abort here.
		if (solrQuery == '(())') {
			return []
		}

		//Construct the rest of the query based on the columns we want back and the number of rows we want.
		solrQuery += '&fl=id&rows=' + solrMaxRows

		List<String> ids = []

		logger.debug 'getIDList - solr Query to be run: {}', solrQuery

		Map args = [path: '/solr/' + coreName + '/select/',
		            query: [q: solrQuery]]
		def xml = querySolr(args)// { xml ->
		for (resultDoc in xml.result.doc) {
			for (it in resultDoc.str) {
				ids << it.toString()
			}
		}
		ids
	}

	/**
	 * All the available fields from Solr.
	 */
	List<String> getCategoryList(String fieldExclusionList, String coreName) {
		List<String> names = []

		//The luke request handler returns schema data.
		Map args = [path: '/solr/' + coreName + '/schema?wt=xml']
		def xml = querySolr(args)// { xml ->
		for (xmlField in xml.schema.fields) {
			if (!(fieldExclusionList.contains(xmlField.name.toString() + '|'))) {
				names << xmlField.name.toString()
			}
		}
		names
	}

	/**
	 * Does the actual work of parsing the JSON data and creating the solr Query with criteria.
	 */
	private String generateSolrQueryFromJson(json, enforceEmpty) {
		StringBuilder query = new StringBuilder()

		def allColumnsInGrid
		List columnsInQuery = []

		for (Map.Entry category in json) {
			String key = category.key.toString()

			//Only add to the query if the category has values.
			if (category.value &&
					key != 'count' &&
					!key.startsWith('GridColumnList') &&
					!key.startsWith('result_instance_id')) {
				columnsInQuery << category.key

				if (query) {
					query << ') AND ('
				}

				boolean needOr = false

				for (categoryItem in category.value) {
					if (needOr) {
						query << ' OR '
					}

					query << key << ':"' << escapeCharList(categoryItem.toString()) << '"'

					needOr = true
				}
			}
			else if (key.startsWith('GridColumnList')) {
				allColumnsInGrid = category.value
			}
		}

		String solrQuery = '((' + query + '))'

		// After creating the string, loop through the columns in the grid and see if we
		// need to enforce empty values for columns not contained in the search object.
		if (enforceEmpty) {
			for (currentColumn in allColumnsInGrid[0]) {
				if (!columnsInQuery.contains(currentColumn) && currentColumn.toString() != 'count') {
					solrQuery += " AND -" + currentColumn + ":[* TO *] "
				}
			}
		}

		if (json != '' && json.result_instance_id) {
			List<String> ids = PatientSampleCollection.executeQuery('''
				select patientId
				from PatientSampleCollection
				where resultInstanceId=:resultInstanceId''',
				[resultInstanceId: json.result_instance_id])
			//solrQuery = idListForSampleSpecificQuery(solrQuery, ids)
			solrQuery = patientNumListForSampleSpecificQuery(solrQuery,ids)
		}

		solrQuery
	}

	/**
	 * Does the actual work of parsing the JSON data and creating the solr Query
	 * with criteria. The difference between this and generateSolrQueryFromJson
	 * is that this looks for JSON Criteria that needs to be interpreted
	 * as (1 AND 2 AND 3) OR (4 AND 5 AND 6).
	 * @param json looks like "Records":[{"Pathology":"Rheumatoid Arthritis","Tissue":"Synovial Membrane","DataSet":"GSE13837","DataType":"Gene Expression","Source_Organism":"Homo Sapiens","Sample_Treatment":"Tumor Necrosis Factor","Subject_Treatment":"Not Applicable","BioBank":"No","Timepoint":"Hour 0","count":3}]
	 */
	private String generateSolrQueryFromJsonDetailed(json, boolean enforceEmpty) {
		def allColumnsInGrid
		List columnsInQuery

		StringBuilder query = new StringBuilder()

		if (json.GridColumnList) {
			allColumnsInGrid = json.GridColumnList
		}

		for (Map record in json.Records) {

			columnsInQuery = []

			if (query) {
				query << ') OR ('
			}

			boolean needAnd = false

			for (Map.Entry category in record) {
				String key = category.key.toString()

				if (category.value &&
						key != 'count' &&
						!key.startsWith('GridColumnList') &&
						!key.startsWith('result_instance_id')) {
					columnsInQuery << category.key

					if (needAnd) {
						query << ' AND '
					}

					//For each of the values in this category, add onto the search string. There should be only one in the detailed case.
					for (categoryItem in category.value) {
						query << key << ':"' << escapeCharList(categoryItem.toString()) << '"'
					}

					needAnd = true
				}
			}

			// After creating the string, loop through the columns in the grid and see if we
			// need to enforce empty values for columns not contained in the search object.
			if (enforceEmpty && allColumnsInGrid) {
				for (currentColumn in allColumnsInGrid) {
					if (!columnsInQuery.contains(currentColumn) && currentColumn.toString() != 'count') {
						query << " AND -" << currentColumn << ":[* TO *] "
					}
				}
			}
		}

		String solrQuery = '((' + query + '))'

		if (json != '' && json.result_instance_id) {
			List<String> ids = PatientSampleCollection.executeQuery('''
					select id
					from PatientSampleCollection
					where resultInstanceId=:resultInstanceId''',
					[resultInstanceId: json.result_instance_id])
			solrQuery = idListForSampleSpecificQuery(solrQuery, ids)
		}

		solrQuery
	}

	private String idListForSampleSpecificQuery(String solrQuery, List<String> idValuesForSpecificSampleQuery) {
		if (!idValuesForSpecificSampleQuery) {
			return ' id:(0) '
		}

		if (solrQuery == '(())') {
			solrQuery = ' id:('
		}
		else {
			solrQuery += ' AND id:('
		}

		solrQuery + idValuesForSpecificSampleQuery.join(' OR ') + ')'
	}

	/**
	 * Updates solrQuery parameter with list of patient numbers based on result instance id
	 * @param solrQuery
	 * @param patientNumValuesForSpecificSampleQuery
	 * @return
	 */
	private patientNumListForSampleSpecificQuery(solrQuery,patientNumValuesForSpecificSampleQuery)
	{
		if(patientNumValuesForSpecificSampleQuery.size() == 0)
		{
			solrQuery = " PATIENT_NUM:(0) "
		}
		else
		{
			if(solrQuery == "(())")
			{
				solrQuery = " PATIENT_NUM:("
			}
			else
			{
				solrQuery += " AND PATIENT_NUM:("
			}

			solrQuery += patientNumValuesForSpecificSampleQuery.join(" OR ")
			solrQuery += ")"
		}

		return solrQuery

	}

	private String escapeCharList(String stringToEscapeIn) {
		for (String s in ESCAPE_CHARS) {
			stringToEscapeIn = stringToEscapeIn.replace(s, '\\' + s)
		}
		stringToEscapeIn
	}

	Map buildSubsetList(json) {

		Map result = [:]

		for (subset in json) {
			result[subset.key] = getIDList(subset.value)
		}

		//Make sure subsets are in order.
		result.sort { it.key }
	}

	int getFacetCountForField(String columnToRetrieve, String resultInstanceId, String coreName) {

		List<String> samplePatientNums = PatientSampleCollection.executeQuery('''
				select id
				from PatientSampleCollection
				where resultInstanceId=:resultInstanceId''',
				[resultInstanceId: resultInstanceId])
		int count = 0

		if (samplePatientNums?.size() > 0) {
			String solrQuery = 'PATIENT_NUM:(' + samplePatientNums.join(' OR ') + ')'

			logger.debug 'getFacetMapForField - {} - solr Query to be run: {}', columnToRetrieve, solrQuery

			Map args = [path: '/solr/' + coreName + '/select/',
			            query: [q: solrQuery,
			                    facet: 'true',
			                    rows: '0',
			                    'facet.field': columnToRetrieve,
			                    'facet.limit': '-1',
			                    'facet.mincount': '1']]
			def xml = querySolr(args) //{ xml ->
			for (outerlst in xml.lst) {
				if (outerlst.@name == 'facet_counts') {
					for (innerlst in outerlst.lst) {
						if (innerlst.@name == 'facet_fields') {
							for (innermostItem in innerlst.lst) {
								for (countValue in innermostItem.int) {
									if (countValue.toString() != '0') {
										count++
									}
								}
							}
						}
					}
				}
			}

		}

		count
	}

	private querySolr(Map args) {
        //Create the http object we will use to retrieve the faceted counts.
        def http = new HTTPBuilder(solrServerUrl)
        //http.auth.basic(username, password)
		Map postArgs = [path: args.get('path'),
					 body: args.get('query'),
					 requestContentType: URLENC]

		try {
			http.post(postArgs) { HttpResponseDecorator response, xml ->
				//We should probably do something with the status.
				if (response.status != '200') {
					logger.error 'Response status from solr web service call: {}', response.status
				}
				return xml
			}
		}
        catch (HttpResponseException e){
            if (e.getStatusCode() == 401)
                throw new HttpResponseException(e.getStatusCode(), "Not authorized.")
            else
                throw e;
        }
		catch (Exception e) {
			logger.error 'Error running query', e
		}


	}

	/**
	 * This method will make post request to Apache Solr server
	 * @param path
	 * @param body Map with params to send
	 * @return XML object from response
	 */
	def postToSolr(path, body)
	{
		//String username = grailsApplication.config.com.recomdata.solr.username
		//String password = grailsApplication.config.com.recomdata.solr.password

		//Create the http object we will use to retrieve the faceted counts.
		def http = new HTTPBuilder(solrServerUrl)
		//http.auth.basic(username, password)

		def xmlResponse
		try
		{
			def html = http.post(
					path: path,
					body: [
							body
					],
					requestContentType: URLENC,
					header: [Accept: 'application/xml'] )
					{ resp, xml ->

						if(resp.status!="200")
							Log.debug("Response status from solr web service call: ${resp.status}")

						xmlResponse = xml;
					}
		}
		catch (Exception e){
			if (e.getStatusCode() == 401)
				throw new HttpResponseException(e.getStatusCode(), "Not authorized.")
			else
				throw e;
		}
		return xmlResponse;
	}
}
