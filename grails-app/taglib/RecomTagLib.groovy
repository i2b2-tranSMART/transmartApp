import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmart.biomart.ContentRepository

import java.text.DecimalFormat
import java.text.SimpleDateFormat

class RecomTagLib {

	def diseaseService

	@Autowired private GrailsPluginManager pluginManager

	@Value('${com.recomdata.appTitle:}')
	private String appTitle

	def createFileLink = { attrs ->

		def document = attrs.document
		def content = attrs.content
		String displaylabel = attrs.displayLabel
		String label
		String path
		String url

		if (document != null) {
			String repository = document.getRepository()
			content = ContentRepository.findByRepositoryType(repository)
			label = document.getFileName()
			path = content.location + '/' + document.getFilePath()
			switch (content.getLocationType()) {
				case 'FILE':
					url = createLink(controller: 'document', action: 'downloadFile', params: [file: path])
					break
				case 'SHARE':
					path = path.replace('/', '\\').replace('\\', '\\\\')
					url = path
					break
				default:
					url = path
					break
			}
		}
		else if (content != null) {
			label = displaylabel ?: content.name
			switch (content.getLocationType()) {
				case 'FILE':
					path = content.getAbsolutePath()
					url = createLink(controller: 'document', action: 'downloadFile', params: [file: path])
					break
				case 'SHARE':
					path = content.getAbsolutePath().replace('/', '\\').replace('\\', '\\\\')
					url = path
					break
				case 'URL':
					path = content.repository.location + content.location
					url = path
					break
				default:
					content.getAbsolutePath()
					url = path
					break
			}
		}
		out << '<a style="border: none" target="_blank" onclick="popupWindow(\''
		out << url
		out << "', 'documentWindow');\">"

		int start = path.lastIndexOf('.')
		String imageName = 'text.png'
		if (start != -1) {
			String ext = path.substring(start + 1, path.length()).toLowerCase()
			switch (ext) {
				case 'pdf': imageName = 'acrobat.png'; break
				case ['doc', 'docx']: imageName = 'word.png'; break
				case ['xls', 'xlsx']: imageName = 'excel.png'; break
				case ['ppt', 'pptx']: imageName = 'powerpoint.png'; break
				case ['htm', 'html']: imageName = 'webpage.png'; break
				case ['txt', 'text']: imageName = 'text.png'; break
				default:
					if (content?.locationType == 'URL') {
						imageName = 'webpage.png'
					}
			}
		}
		out << '<img src="'
		out << resource(dir: 'images', file: imageName)
		out << '"/>&nbsp;'
		out << label.encodeAsHTML()
		out << '</a>'
	}

	/**
	 * Outputs display value for tag item creating links to studies, pubmed IDs.
	 */
	def createTagItemValue = { attrs ->

		def tagItem = attrs.tagItem
		def tagValue = attrs.tagValue

		if (!tagItem || !tagValue) {
			return
		}

		if (tagItem.codeTypeName == 'STUDY_LINK') {
			out << "<a href=\"#\" onclick=\"var w=window.open('" << tagValue.displayValue << "', '_blank'); w.focus(); return false;\">"
			out << tagValue.displayValue << '&nbsp;'
			out << '<img alt="external link" class="ExternalLink" src="' << resource(dir: 'images', file: 'linkext7.gif') << '"/>'
			out << '</a>'
		}
		else if (tagItem.codeTypeName != null && tagItem.codeTypeName.endsWith('_PUBMED_ID')) {
			out << "<a href=\"#\" onclick=\"var w=window.open('http://www.ncbi.nlm.nih.gov/pubmed/" << tagValue.displayValue << "', '_blank'); w.focus(); return false;\">"
			out << tagValue.displayValue << '&nbsp;'
			out << '<img alt="external link" src="' << resource(dir: 'images', file: 'linkext7.gif') << '"/>'
			out << '</a>'
		}
		else if (tagItem.codeTypeName != null && tagItem.codeTypeName.endsWith('_DOI')) {
			out << "<a href=\"#\" onclick=\"var w=window.open('http://doi.org/" << tagValue.displayValue << "', '_blank'); w.focus(); return false;\">"
			out << tagValue.displayValue << '&nbsp;'
			out << '<img alt="external link" src="' << resource(dir: 'images', file: 'linkext7.gif') << '"/>'
			out << '</a>'
		}
		else {
			out << tagValue.displayValue
		}
	}

	def createFilterDetailsLink = { attrs ->

		String id = attrs.id ?: ''
		String altId = attrs.altId ?: ''
		String label = attrs.label ?: ''
		String type = attrs.type ?: ''
		boolean plain = 'true' == attrs.plain

		if (type == 'gene' || type == 'pathway' || type == 'compound') {
			out << "<a href=\"#\" onclick=\"var w=window.open('"
			out << createLink(controller: 'details', action: type, params: [id: id, altId: altId])
			out << "', 'detailsWindow', 'width=900,height=800'); w.focus(); return false;\">"
			out << '<span class="filter-item filter-item-'
			out << type
			out << '">'
			out << label
			out << '</span>'
			if (!plain) {
				out << '&nbsp;<img class="ExternalLink" src="'
				out << resource(dir: 'images', file: 'linkext7.gif')
				out << '"/>'
			}
			out << '</a>'
		}
		else if (type == 'genesig' || type == 'genelist') {
			out << "<a href=\"#\" onclick=\"var w=window.open('"
			out << createLink(controller: 'geneSignature', action: 'showDetail', params: [id: id])
			out << "', 'detailsWindow', 'width=900,height=800,scrollbars=yes'); w.focus(); return false;\">"
			out << '<span class="filter-item filter-item-'
			out << type
			out << '">'
			out << label
			out << '</span>'
			if (!plain) {
				out << '&nbsp;<img class="ExternalLink" src="'
				out << resource(dir: 'images', file: 'linkext7.gif')
				out << '"/>'
			}
			out << '</a>'
		}
		else if (type == 'text') {
			out << '<span class="filter-item filter-item-'
			out << type
			out << '">"'
			out << label
			out << '"</span>'
		}
		else {
			out << '<span class="filter-item filter-item-'
			out << type
			out << '">'
			out << label
			out << '</span>'
		}
	}

	def createRemoveFilterLink = { attrs ->
		def id = attrs.id // Note that text will use the text string for its id value.

		out << '<a class="filter-item filter-item-remove" href="#" onclick="removeFilter(\''
		out << id
		out << '\');"><img alt="remove" src="'
		out << resource(dir: 'images', file: 'remove.png')
		out << '" /></a>'
	}

	/**
	 * Takes a search keyword instance to create a search link
	 */
	def createKeywordSearchLink = { attrs ->
		def keyword = attrs.keyword
		String function = attrs.jsfunction
		String controller = 'search'
		String link = createLink(controller: controller, action: 'search', params: [id: keyword.id])
		if (function) {
			out << '<a href="'
			out << link
			out << '" onclick="'
			out << function
			out << "('"
			out << link
			out << "');\">"
		}
		else {
			out << '<a href="'
			out << link
			out << '" onclick="window.document.location=\''
			out << link
			out << "';\">"
		}
		out << keyword.keyword
		out << '<img class="ExternalLink" alt="search" src="'
		out << resource(dir: 'images', file: 'internal-link.gif')
		out << '"/></a>'
	}

	def createPropertyTableRow = { attrs ->

		def width = attrs.width
		def label = attrs.label
		def value = attrs.value
		out << '<tr class="prop">'
		out << '<td class="name" width="' << width << '" align="right">'
		out << '<span class="Label">' << label << ':</span>'
		out << "</td>"
		out << '<td class="value">'
		if (value) {
			out << value
		}
		else {
			out << '&nbsp;'
		}
		out << '</td>'
		out << '</tr>'
	}

	def createNameValueRow = { attrs ->

		String name = message(code: attrs.name, default: attrs.name)
		String value = attrs.value?.toString()?.trim() ?: ''

		if (value) {
			out << '<tr class="prop">'
			out << '<td valign="top" class="name">' << name << ':</td>'
			out << '<td valign="top" class="value">' << value << '</td>'
			out << '</tr>'
		}
	}

	def createCustomFilterEmailLink = { attrs ->

		def customFilter = attrs.customFilter

		out << '<a href="mailto:?subject=Link to '
		out << customFilter?.name?.replace('"', '%22')
		out << '&body=The following is a link to the '
		out << customFilter?.name?.replace('"', '%22')
		out << ' saved filter in the '
		out << appTitle
		out << ' application.%0A%0A'
		out << createLink(controller: 'search', action: 'searchCustomFilter', id: customFilter.id, absolute: true)
		out << '" traget="_blank" class="tiny" style="text-decoration:underline;color:blue;font-size:11px;">email</a>'
	}

	/**
	 * display wait image. This tag takes attributes 'divId' - the div id for you <div> and 'message' - the message to display in the wait image
	 * If the message is null by default 'Loading...' displays. The <div> is initally hidden so you will need to toggle the display style to show
	 */
	def waitIndicator = { attrs, body ->

		String divId = attrs.divId
		String message = attrs.message ?: 'Loading...'

		// render tag
		out << '<div id="'
		out << divId
		out << '" class="loading-indicator" style="display: none;">'
		out << message
		out << '</div>'
	}

	/**
	 * useful for input screens to identify required fields with a red asterisk
	 */
	def requiredIndicator = { attrs, body ->
		out << '<span style="color: red;">*</span>'
	}

	/**
	 * creates a <thead> element in a table with a highlighted header along with a toggle for a specified div id prefix defined in <tbody> element
	 * attribute label - header text to display
	 * attribute divPrefix - div id prefix to be used for the <tbody> div id with format '<prefix>_detail' which is toggled by js fct toggleDetail()
	 * attribute status - open or closed (indicates if arrow is down (closed) or up (open)
	 */
	def tableHeaderToggle = { attrs, body ->

		String label = attrs.label
		String divPrefix = attrs.divPrefix
		String status = attrs.status ?: 'closed'
		def colSpan = attrs.colSpan ?: 1
		boolean open = status == 'open'

		String openStyle = open ?
				'visibility: hidden; display: none; vertical-align: middle;' :
				'visibility: visible; display: block; vertical-align: middle;'
		String closedStyle = open ?
				'visibility: visible; display: block; vertical-align: middle;' :
				'visibility: hidden; display: none; vertical-align: middle;'

		out << '<thead><tr><th colSpan="' << colSpan << '" class="tableToggle">'
		out << '<a id="' << divPrefix << '_fopen" style="' << openStyle << '" '
		out << "onclick=\"javascript:toggleDetail('" << divPrefix << "');\">" << label << "&nbsp;<img alt='Open' src=\"${resource(dir: 'images/skin', file: 'sorted_desc.gif')}\" /></a> "
		out << "<a id='" << divPrefix << "_fclose' style='" << closedStyle << "' "
		out << "onclick=\"javascript:toggleDetail('" << divPrefix << "');\">" << label << "&nbsp;<img alt='Close' src=\"${resource(dir: 'images/skin', file: 'sorted_asc.gif')}\" /></a> "
		out << "</th></tr></thead>"
	}

	def fieldDate = { attrs, body ->

		def bean = attrs.bean
		def field = attrs.field
		def format = attrs.format

		def date = bean[field]
		if (date) {
			out << new SimpleDateFormat(format).format(date)
		}
		else {
			out << 'None'
		}
	}

	def fieldBytes = { attrs, body ->
		def bean = attrs.bean
		def field = attrs.field
		def bytes = bean[field]

		if (bytes < 1024) {
			out << bytes + ' B' //Don't format a decimal on!
			return
		}

		bytes /= 1024

		if (bytes < 1024) {
			out << new DecimalFormat('0.0').format(bytes) << ' KB'
			return
		}

		bytes /= 1024

		if (bytes < 1024) {
			out << new DecimalFormat('0.0').format(bytes) << ' MB'
			return
		}
	}

	def meshLineage = { attrs, body ->

		def disease = attrs.disease
		def lineage = diseaseService.getMeshLineage(disease)

		int index = 0
		for (item in lineage) {
			out << "<div class='diseaseHierarchy'" << (index == 0 ? " style='background-image: none;'" : "") << ">" << item.disease
			index++
		}
		for (item in lineage) {
			out << "</div>"
		}
	}

	def ifPlugin = { attrs, body ->

		String name = attrs.name
		String yes = attrs.true
		String no = attrs.false

		//If the tag does not have true/false reactions, do the body. If it does, output the yes/no string.
		if (pluginManager.hasGrailsPlugin(name)) {
			if (yes) {
				out << yes
			}
			else {
				out << body()
			}
		}
		else if (no) {
			out << no
		}
	}

	/**
	 * container for content displayed in overlay div with a title
	 */
	def overlayPopup = { attrs, body ->

		String width = attrs.width
		String title = attrs.title
		String divId = attrs.divContainerId

		if (width) {
			out << '<table cellspacing="0" style="width: ' << width << '">'
		}
		else {
			out << '<table cellspacing="0">'
		}

		out << '<thead><tr>'
		out << '	<td style="padding: 4px; font-weight: bold; font-size: 14px; background-color: #2C5197; color: white;">' << title << '</td>'
		out << '	<td style="padding: 4px; background-color: #2C5197; padding: 3px; text-align: right;">'
		out << "		<a href='#' onclick=\"hideElement('" <<  divId << "'); hideElement('" << divId << "_fade'); return false;\">"
		out << '			<img alt="Close" style="vertical-align: middle;" border="0" src="' << resource(dir: 'images', file: 'close.gif') << '" />'
		out << '		</a>'
		out << '	</td>'
		out << '</tr></thead>'
		out << '<tbody>'
		out << '<tr><td colspan="2">'

		// page content
		out << body()

		out << '</td></tr>'
		out << '</tbody>'
		out << '</table>'
	}

	/**
	 * create an overlay which is the target for DHTML popup windows (i.e. overlay)
	 */
	def overlayDiv = { attrs, body ->
		String divId = attrs.divId
		String cssClass = attrs.cssClass ?: 'overlay'
		out << "<div id='${divId}' class='${cssClass}'>"
		out << '	<p>Loading data. Please wait...</p>'
		out << '</div>'
		out << '<div id="' << divId << '_fade" class="backgroundOverlay" />'
	}
}
