import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmart.plugin.shared.SecurityService
import transmartapp.OntologyService

class DatasetExplorerController {

	@Value('${com.recomdata.i2b2.subject.domain:}')
	private String i2b2Domain

	@Value('${com.recomdata.i2b2.subject.projectid:}')
	private String i2b2ProjectId

	@Value('${com.recomdata.i2b2.subject.username:}')
	private String i2b2Username

	@Value('${com.recomdata.i2b2.subject.password}')
	private String i2b2Password

	@Autowired private I2b2HelperService i2b2HelperService
	@Autowired private OntologyService ontologyService
	@Autowired private SecurityService securityService

	def index(String accession, String path) {
		String pathToExpand
		//If we have an accession passed, retrieve its path
		if (accession) {
			pathToExpand = ontologyService.getPathForAccession(accession)
		}

		//code for retrieving a saved comparison
		pathToExpand = pathToExpand ?: path

		def rwgSearchFilter = session.rwgSearchFilter
		if (rwgSearchFilter) {
			rwgSearchFilter = rwgSearchFilter.join(',,,')
		}
		else {
			rwgSearchFilter = ''
		}

		def rwgSearchOperators = session.rwgSearchOperators
		if (rwgSearchOperators) {
			rwgSearchOperators = rwgSearchOperators.join(';')
		}
		else {
			rwgSearchOperators = ''
		}

		def tokens = i2b2HelperService.getSecureTokensCommaSeparated()
		def initialaccess = new JSON(i2b2HelperService.getAccess(i2b2HelperService.getRootPathsWithTokens())).toString()
		render view: 'datasetExplorer', model: [pathToExpand      : pathToExpand,
		                                        admin             : securityService.principal().isAdmin(),
		                                        tokens            : tokens,
		                                        initialaccess     : initialaccess,
		                                        i2b2Domain        : i2b2Domain,
		                                        i2b2ProjectID     : i2b2ProjectId,
		                                        i2b2Username      : i2b2Username,
		                                        i2b2Password      : i2b2Password,
		                                        rwgSearchFilter   : rwgSearchFilter,
		                                        rwgSearchOperators: rwgSearchOperators,
		                                        globalOperator    : session.globalOperator,
		                                        rwgSearchCategory : session.searchCategory,
		                                        debug             : params.debug,
		                                        dseOpenedNodes    : session.dseOpenedNodes,
		                                        dseClosedNodes    : session.dseClosedNodes]
	}

	def queryPanelsLayout() {
		render view: '_queryPanel'
	}
}
