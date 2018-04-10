package transmartapp

import fm.FmFolder
import fm.FmFolderAssociation
import fm.FmFolderService
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NoChildren
import groovy.util.slurpersupport.NodeChild
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmart.biomart.BioMarker
import org.transmart.biomart.BioMarkerExpAnalysisMV

@Slf4j('logger')
class SolrFacetService {

	static transactional = false

	@Autowired private FmFolderService fmFolderService
	@Autowired private OntologyService ontologyService

	@Value('${com.rwg.solr.browse.path:}')
	private String solrBrowsePath

	@Value('${com.rwg.solr.host:}')
	private String solrHost

	@Value('${com.rwg.solr.scheme:}')
	private String solrScheme

	@Value('${com.rwg.solr.update.path:}')
	private String solrUpdatePath

	private List searchLog = [] //Search log for debug only! Will be shared across all sessions

	def getCombinedResults(List<String> categoryList, String page, String globalOperator, List<String> passedInSearchLog) {

		String solrRequestUrl = createSOLRQueryPath()

		def searchResultIds = []
		searchLog = passedInSearchLog

		// whether it is the first category. It is used for the search with AND operator:
		boolean firstCategory = true

		//For each category (except Datanode), construct a SOLR query
		for (String category in categoryList) {
			searchLog << ' - - - Examining category: ' + category
			//Split off the operator with ::
			String operator = category.split('::')[1].toUpperCase()
			category = category.split('::')[0]

			String categoryName = category.split(':', 2)[0]
			String[] termList = category.split(':', 2)[1].split('\\|')

			//If in Browse, we're gathering folder paths. If in Analyze, we want i2b2 paths
			//CategoryResultIds is used to gather IDs returned by this category - always add to this list, don't intersect (producing OR).
			def categoryResultIds = []

			// Start HERE if we're looking for metadata (anything other than text)
			if (categoryName != 'text') {
				//Make this metadata field into a SOLR query
				searchLog << 'Searching for metadata: ' + termList.join(',')

				String categoryQuery = createCategoryQueryString(categoryName, termList, operator)
				String solrQuery = 'q=' + createSOLRQueryString(URLEncoder.encode(categoryQuery), '', '')
				searchLog << solrQuery
				GPathResult xml = executeSOLRFacetedQuery(solrRequestUrl, solrQuery)

				//If browse, convert this to a folder path list - if analyze, a node path list
				if (page == 'RWG') {
					categoryResultIds += getFolderList(xml)
					if (categoryName == 'GENE') {
						searchLog += 'Getting analyses for gene categories...'
						categoryResultIds += getAnalysesForGenes(termList, operator, false)
					}
				}
				else {
					categoryResultIds += getNodesByAccession(getAccessions(xml))
					if (categoryName == 'GENE') {
						searchLog += 'Getting analyses for gene categories...'
						categoryResultIds += getAnalysesForGenes(termList, operator, true)
					}
				}
			}

			/*
			* Start HERE if we're looking for text - and include datanodes (as OR)
			*/
			else {
				//Content: Get the list of terms, and search in text field OR the data nodes
				searchLog += 'Searching for freetext: ' + termList.join(',')

				def categoryQuery = createCategoryQueryString('text', termList, operator)
				def solrQuery = 'q=' + createSOLRQueryString(URLEncoder.encode(categoryQuery), '', '')
				searchLog += solrQuery

				def xml = executeSOLRFacetedQuery(solrRequestUrl, solrQuery)

				//If browse, convert this to a folder path list - if analyze, a node path list
				if (page == 'RWG') {
					categoryResultIds += getFolderList(xml)
				}
				else {
					categoryResultIds += getNodesByAccession(getAccessions(xml))
				}

				//If browse, get the studies from SOLR that correspond to the returned accessions - if analyze, just add the paths.
				if (page == 'RWG') {
					searchLog += 'Getting accessions for datanode search: ' + termList
					def ontologyAccessions = ontologyService.searchOntology(null, termList, 'ALL', 'accession', null, operator)

					if (ontologyAccessions) {
						def solrQueryString = ''

						for (accession in ontologyAccessions) {
							searchLog << 'Got accession from i2b2 search: ' + accession

							if (solrQueryString) {
								solrQueryString += ' OR '
							}

							solrQueryString += 'ACCESSION:"' + accession + '"'
						}

						solrQueryString = 'q=(' + solrQueryString + ')&facet=false&rows=1000'

						searchLog += 'Searching SOLR for studies with accessions: ' + solrQueryString
						xml = executeSOLRFacetedQuery(solrRequestUrl, solrQueryString)
						categoryResultIds += getFolderList(xml)
					}
					else {
						searchLog += 'No accessions found.'
					}
				}
				else {
					searchLog += 'Getting paths for datanode search: ' + termList
					categoryResultIds += ontologyService.searchOntology(null, termList, 'ALL', 'path', null, operator)
				}
			}

			searchLog += 'Category result IDs: ' + categoryResultIds

			//If the master searchResultsIds list is empty, copy this in - otherwise intersect.
			//If we have nothing for a search category during an AND search, return nothing immediately!
			if (!categoryResultIds && globalOperator == 'AND') {
				searchLog += 'No results for this category during an AND search - stopping.'
				return [paths: [], searchLog: searchLog]
			}

			if (!searchResultIds) {
				if (firstCategory) {
					searchLog += 'Starting search results list with the above IDs.'
					searchResultIds = categoryResultIds
					firstCategory = false
				}
				else {
					searchLog += 'Starting search results list with empty list.'
					searchResultIds = []
				}
			}
			else {
				searchLog += 'Search results so far are these IDs: ' + searchResultIds
				if (globalOperator == 'AND') {
					searchLog += 'Doing hierarchical intersect for AND search.'
					searchResultIds = hierarchicalIntersect(searchResultIds, categoryResultIds)
				}
				else {
					searchLog += 'Combining for OR search.'
					searchResultIds = searchResultIds + categoryResultIds
				}
				searchLog += 'Search results after combining: ' + searchResultIds
			}

		}

		//And return the complete list of folder/i2b2 paths!
		return [paths: searchResultIds, searchLog: searchLog]

	}

	def getAnalysesForGenes(termList, operator, convertToNodes) {
		//We get a list of genes here - slash-delimited for OR.
		//For each set of terms, create a list then check against materialized view.
		def analysisResults = []

		for (geneList in termList) {
			searchLog += 'Getting analyses for genes: ' + geneList
			def geneUids = geneList.split('/')
			def bioMarkers = []
			for (uid in geneUids) {
				bioMarkers.push(BioMarker.findByUniqueId(uid))
			}
			def result = BioMarkerExpAnalysisMV.createCriteria().list {
				'in'('marker', bioMarkers)
			}

			searchLog += 'Found ' + result.size() + ' analysis matches'

			//Union or intersection, as needed by AND/OR
			if (operator == 'OR') {
				analysisResults += result*.analysis
			}
			else {
				if (!result) {
					return []
				}

				if (!analysisResults) {
					analysisResults = result*.analysis
				}
				else {
					def newAnalyses = result*.analysis
					//Manually intersect
					def newResults = []
					for (aResult in analysisResults) {
						for (newResult in newAnalyses) {
							if (aResult.id == newResult.id) {
								newResults.add(aResult)
							}
						}
					}
					analysisResults = newResults
					if (!analysisResults) {
						return []
					}
				}
			}
		}

		searchLog += 'Final analysis ID list: ' + (analysisResults*.id).join(', ')

		//Convert to folder UIDs, then convert to nodes if needed
		def folders = []
		for (result in analysisResults) {
			FmFolderAssociation folderAssoc = FmFolderAssociation.findByObjectUid(result.uniqueId.uniqueId)
			if (folderAssoc) {
				def folder = folderAssoc.fmFolder
				if (folder.activeInd) {
					folders.push(folder)
				}
			}
		}

		if (convertToNodes) {
			def accessions = []
			for (fmFolder in folders) {
				searchLog += 'Finding associated accession for folder: ' + fmFolder.folderFullName
				def accession = fmFolderService.getAssociatedAccession(fmFolder)
				if (accession) {
					searchLog += 'Got accession: ' + accession
					accessions.push(accession)
				}
				else {
					searchLog += 'No accession found'
				}
			}
			return getNodesByAccession(accessions)
		}
		else {
			def paths = []
			for (folder in folders) {
				paths.push(folder.folderFullName)
			}
			return paths
		}
	}

	def hierarchicalIntersect(searchResults, categoryResults) {

		def newSearchResults = []

		//Add both sets of results to a map - folder/annotation.
		def oldMap = [:]
		def newMap = [:]
		for (s in searchResults) {
			oldMap.put(s, [])
		}
		for (c in categoryResults) {
			newMap.put(c, [])
		}

		//Iterate over both maps, annotating items according to whether old result is a superset, new result is a superset/match, or a conflict is created.
		def oldKeys = oldMap.keySet()
		def newKeys = newMap.keySet()

		for (nk in newKeys) {
			for (ok in oldKeys) {
				if (nk.startsWith(ok)) {
					newMap[nk] << 'N'
					oldMap[ok] << 'N'
				}
				else if (ok.startsWith(nk)) { //Equal will have been handled above
					newMap[nk] << 'O'
					oldMap[ok] << 'O'
				}
			}
		}

		//Take Os from old keys, Ns from new keys
		for (nk in newKeys) {
			if (newMap[nk].contains('N') && !newSearchResults.contains(nk)) {
				newSearchResults.push(nk)
			}
		}
		for (ok in oldKeys) {
			if (oldMap[ok].contains('O') && !newSearchResults.contains(ok)) {
				newSearchResults.push(ok)
			}
		}

		newSearchResults
	}


	def getNodesByAccession(accessions) {
		if (!accessions) {
			return []
		}

		//If we have any accessions, return the node paths from i2b2 (on the study level)
		else {
			searchLog += "Finding study paths in i2b2 with these accessions: " + accessions
			def results = ontologyService.searchOntology(null, null, 'ALL', 'path', accessions, "")
			searchLog += "Got paths: " + results
			return results
		}
	}

	private List<String> getAccessions(xml) {
		searchLog << "Getting accessions from SOLR search results"

		List<String> accessions = []

		for (node in xml.result.doc) {

			def folderId
			//Use "folder" if this is a file result, "id" otherwise
			def folderNode = node.str.findAll { it.@name == 'folder' }
			if (folderNode.size() > 0) {
				folderId = folderNode.text()
				searchLog += "Got folder ID from SOLR file result: " + folderId
			}
			else {
				def idNode = node.str.findAll { it.@name == 'id' }
				if (idNode.size() > 0) {
					folderId = idNode.text()
					searchLog += "Got folder ID from SOLR folder result: " + folderId
				}
				else {
					logger.error 'SolrFacetService.getAccessions: result node does not contain an id or folder'
				}
			}

			def fmFolder = FmFolder.findByUniqueId(folderId)
			searchLog << "Finding associated accession for folder: " + folderId
			String accession = fmFolderService.getAssociatedAccession(fmFolder)
			if (accession) {
				searchLog << "Got accession: " + accession
				accessions << accession
			}
			else {
				searchLog += "No accession found"
			}
		}

		accessions
	}

	private getFolderList(GPathResult xml) {

		//retrieve all folderUIDs from the returned data

		def folderSearchList = []
		for (node in xml.result.doc) {

			String folderId
			//Use "folder" if this is a file result, "id" otherwise
			def folderNode = node.str.findAll { it.@name == 'folder' }
			if (folderNode.size() > 0) {
				folderId = folderNode.text()
				searchLog += "Got folder ID from SOLR file result: " + folderId
			}
			else {
				def idNode = node.str.findAll { it.@name == 'id' }
				if (idNode.size() > 0) {
					folderId = idNode.text()
					searchLog += "Got folder ID from SOLR folder result: " + folderId
				}
				else {
					logger.error 'SolrFacetService.getFolderList: result node does not contain an id or folder'
				}
			}

			def fmFolder = FmFolder.findByUniqueId(folderId)
			if (fmFolder != null) {
				folderSearchList.push(fmFolder?.folderFullName)
			}
			else {
				logger.error 'No folder found for unique ID: {}', folderId
			}
		}

		return folderSearchList
	}

	/**
	 * Create a query string for the category in the form of (<cat1>:"term1" OR <cat1>:"term2")
	 */
	private String createCategoryQueryString(String category, String[] termList, String operator) {

		// create a query for the category in the form of (<cat1>:"term1" OR <cat1>:"term2")
		String categoryQuery = ''
		for (String t in termList) {
			t = cleanForSOLR(t)

			String pathwayInGeneSearch = null

			//If searching on text and we have no spaces (not a phrase search), add wildcards instead of quote marks
			if (category == 'text') {
				if (t.contains(" ")) {
					t = '"' + t.toLowerCase() + '"'
				}
				else {
					t = "*" + t.toLowerCase() + "*"
				}
			}
			else if (category == "GENE") {
				//GENE may have individual genes separated by slashes. OR these, and quote each individual one
				//If this is a pathway, flag it
				def geneList = []
				for (g in t.split("/")) {
					if (g.startsWith("PATHWAY")) {
						pathwayInGeneSearch = g
					}
					else {
						geneList += ('"' + g + '"')
					}
				}
				t = "(" + geneList.join(" OR ") + ")"
			}
			else {
				t = '"' + t + '"'
			}

			String queryTerm

			//Special case for pathways in a gene search
			if (category == "GENE" && pathwayInGeneSearch) {
				queryTerm = /(PATHWAY:("${pathwayInGeneSearch}") OR GENE:${t})/
			}
			else {
				queryTerm = /${category}:${t}/
			}

			if (categoryQuery == "") {
				categoryQuery = queryTerm
			}
			else {
				categoryQuery = /${categoryQuery} ${operator} ${queryTerm}/
			}
		}

		// enclose query clause in parens
		categoryQuery = /(${categoryQuery})/

		return categoryQuery
	}

	def cleanForSOLR(t) {
		return t.replace("&", "%26").replace("(", "\\(").replace(")", "\\)")
	}

	/**
	 * Create a query string for the category in the form of (<cat1>:"term1" OR <cat1>:"term2")
	 */
	def getDataNodeSearchTerms = { queryParams ->

		def datanodeterms = []
		for (qp in queryParams) {

			// each queryParam is in form cat1:term1|term2|term3
			String category = qp.split(":")[0]

			if (category == "DATANODE" || category == "text") {
				String termList = qp.split(":")[1]

				for (t in termList.tokenize("|")) {
					datanodeterms.push(t)
				}
			}
		}

		return datanodeterms
	}

	/**
	 * Create the SOLR query string for the faceted fields (i.e. those that are in tree) that
	 *   are not being filtered on
	 * It will be of form facet.field=<cat1>&facet.field=<cat2>
	 */
	def createSOLRFacetedFieldsString = { facetFieldsParams ->
		def facetedFields = ""
		// loop through each regular query parameter
		for (ff in facetFieldsParams) {

			//This list should be in a config, but we don't facet on some of the fields.
			if (ff != "REGION_OF_INTEREST" && ff != "GENE" && ff != "SNP") {
				// skip TEXT search fields (these wouldn't be in tree so throw exception since this should never happen)
				if (ff == "TEXT") {
					throw new Exception("TEXT field encountered when creating faceted fields string")
				}

				def ffClause = /facet.field=${ff}/

				if (facetedFields == "") {
					facetedFields = /${ffClause}/
				}
				else {
					facetedFields = /${facetedFields}&${ffClause}/
				}
			}
		}

		return facetedFields
	}

	/**
	 * Create the SOLR query string for the faceted fields (i.e. those that are in tree) that are being filtered
	 * It will be of form facet=true&facet.field=(!ex=c1)<cat1>&facet.field=(!ex=c2)<cat2>&
	 *     fq={!tag=c1}(<cat1>:"term1" OR <cat1>:"term2")&.... )
	 * Each category query gets tagged in fq clauses {!tag=c1}, and then the category query is excluded
	 *   for determining the facet counts (!ex=c1) in facet.field clauses
	 */
	def createSOLRFacetedQueryString = { facetQueryParams ->
		def facetedQuery = ""
		// loop through each regular query parameter
		for (qp in facetQueryParams) {

			// each queryParam is in form cat1:term1|term2|term3
			String category = qp.split(";")[0]
			String termList = qp.split(";")[1]

			// skip DATANODE search fields
			if (category == "DATANODE") {
				continue
			}

			def categoryQueryString = createCategoryQueryString(category, termList)

			def categoryTag = /{!tag=${category}}/

			def fqClause = /fq=${categoryTag}${categoryQueryString}/

			def categoryExclusion = /{!ex=${category}}/
			def ffClause = /facet.field=${categoryExclusion}${category}/

			def categoryClause = /${ffClause}&${fqClause}/

			if (facetedQuery == "") {
				facetedQuery = /${categoryClause}/
			}
			else {
				facetedQuery = /${facetedQuery}&${categoryClause}/
			}

		}

		return facetedQuery
	}

	/**
	 * Create the SOLR query string for the nonfaceted fields (i.e. those that are not in tree)
	 * It will be of form ((<cat1>:"term1" OR <cat1>:"term2") AND ( (<cat2>:"term3") ) AND () .. )
	 */
	public String createSOLRNonfacetedQueryString(List queryParams) {
		def nonfacetedQuery = ""
		// loop through each regular query parameter
		for (qp in queryParams) {

			// each queryParam is in form cat1:term1|term2|term3
			String category = ((String) qp).split(":", 2)[0]
			String termList = ((String) qp).split(":", 2)[1]

			def categoryQueryString = createCategoryQueryString(category, termList)

			// skip DATANODE search fields - handled later
			if (category == "DATANODE") {
				continue
			}

			// add category query to main nonfaceted query string using ANDs between category clauses
			if (nonfacetedQuery == "") {
				nonfacetedQuery = categoryQueryString
			}
			else {
				nonfacetedQuery = /${nonfacetedQuery} AND ${categoryQueryString}/
			}
		}

		// use all query if no params provided
		if (nonfacetedQuery == "") {
			nonfacetedQuery = "*:*"
		}

		nonfacetedQuery = /q=(${nonfacetedQuery})/

		return nonfacetedQuery
	}

	/**
	 * Create the base URL for the SOLR request
	 * @return string containing the base URL for the SOLR query
	 */
	private String createSOLRQueryPath() {
		new URI(solrScheme, solrHost, solrBrowsePath, "", "").toURL()
	}

	private String createSOLRUpdatePath() {
		new URI(solrScheme, solrHost, solrUpdatePath, "", "").toURL()
	}

	/**
	 * Create the SOLR query string for the faceted query
	 * @param nonfacetedQueryString - the portion of the URL containing the non faceted query string
	 * @param facetedQueryString - the portion of the URL containing the faceted query string
	 * @param facetedFieldsString - the portion of the URL containing the faceted fields string
	 * @param maxRows - max number of result rows to return (default to 0
	 * @return string containing the SOLR query string
	 */
	def createSOLRQueryString = {
		nonfacetedQueryString, facetedQueryString, facetedFieldsString, maxRows = 1000, facetFlag = false ->
			def solrQuery = /${nonfacetedQueryString}&facet=${facetFlag}&rows=${maxRows}/

			if (facetedQueryString != "") {
				solrQuery = /${solrQuery}&${facetedQueryString}/
			}

			if (facetedFieldsString != "") {
				solrQuery = /${solrQuery}&${facetedFieldsString}/
			}
			return solrQuery
	}

	/**
	 * Execute the SOLR faceted query
	 * @param solrRequestUrl - the base URL for the SOLR request
	 * @param solrQueryParams - the query string for the faceted search, to be passed into the data for the POST request
	 * @return JSONObject containing the facet counts
	 */
	private GPathResult executeSOLRFacetedQuery(String solrRequestUrl, String solrQueryParams) {

		logger.debug solrQueryParams

		// submit request
		def solrConnection = new URL(solrRequestUrl).openConnection()
		solrConnection.requestMethod = "POST"
		solrConnection.doOutput = true

		// add params to request
		def dataWriter = new OutputStreamWriter(solrConnection.outputStream)
		dataWriter.write(solrQueryParams)
		dataWriter.write("&fl=id,folder")
		dataWriter.flush()
		dataWriter.close()

		// process response
		if (solrConnection.responseCode == solrConnection.HTTP_OK) {
			def xml

			solrConnection.inputStream.withStream { InputStream it ->
				xml = new XmlSlurper().parse(it)
			}

			solrConnection.disconnect()
			return xml
		}
		else {
			throw new Exception("SOLR Request failed! Request url:" + solrRequestUrl + "  Response code:" + solrConnection.responseCode + "  Response message:" + solrConnection.responseMessage)
		}
	}

	def reindexFolder = { folderUid, folderType = "" ->

		def solrRequestUrl = createSOLRUpdatePath()
		def solrUpdateParams = "command=full-import&commit=true&clean=false&uid=" + folderUid
		if (folderType) {
			solrUpdateParams += "&entity=" + folderType
		}

		// submit request
		def solrConnection = new URL(solrRequestUrl).openConnection()
		solrConnection.requestMethod = "POST"
		solrConnection.doOutput = true

		// add params to request
		def dataWriter = new OutputStreamWriter(solrConnection.outputStream)
		dataWriter.write(solrUpdateParams)
		dataWriter.flush()
		dataWriter.close()

		//If HTTP OK, return success
		if (solrConnection.responseCode == solrConnection.HTTP_OK) {
			solrConnection.disconnect()
			return true
		}
		else {
			log.error("SOLR update failed! Request url:" + solrRequestUrl + "  Response code:" + solrConnection.responseCode + "  Response message:" + solrConnection.responseMessage)
		}

	}

	Map getSearchHighlight(FmFolder folder, List<String> categoryList) {

		String textSearch = categoryList?.find { it.startsWith("text:") }
		if (textSearch) {

			// Parse the search terms into an operator, category and term list
			String operator = textSearch.split("::")[1].toUpperCase()
			textSearch = textSearch.split("::")[0]
			String categoryName = textSearch.split(":", 2)[0]
			def termList = textSearch.split(":", 2)[1].split("\\|")

			// Construct search query (with highlight parameters)
			String url = createSOLRQueryPath()
			String categoryQuery = createCategoryQueryString(categoryName, termList, operator)
			String solrQuery = "q=" + createSOLRQueryString(URLEncoder.encode(categoryQuery), "", "")
			String highlight = "hl=true&hl.fl=title+description&hl.fragsize=0" +
					"&hl.simple.pre=<mark><b>&hl.simple.post=</b></mark>"
			String parameters = [solrQuery, highlight].join("&")

			// Execute search
			NodeChild xml = executeSOLRFacetedQuery(url, parameters)

			String hlTitle
			String hlDescription
			List<String> hlFileIds = []
			// Search the response for the current folder and matching file ids
			def highlighting = xml.lst.find { it.@name == "highlighting" }
			if (!(highlighting instanceof NoChildren)) {
				for (NodeChild match in highlighting.lst) {
					String matchId = match.@name.text()
					if (matchId == folder.uniqueId) {
						// Extract the highlighted title & description from the result
						def hlTitleNode = match.arr.find { it.@name == "title" }
						if (!(hlTitleNode instanceof NoChildren)) {
							hlTitle = hlTitleNode.str[0].text()
						}
						def hlDescriptionNode = match.arr.find { it.@name == "description" }
						if (!(hlDescriptionNode instanceof NoChildren)) {
							hlDescription = hlDescriptionNode.str[0].text()
						}
					}
					if (matchId.startsWith("FIL")) {
						// File match found; add it for highlighting in filesTable
						hlFileIds << matchId
					}
				}
			}
			return [title      : hlTitle,
			        description: hlDescription,
			        fileIds    : hlFileIds
			]
		}
	}
}
