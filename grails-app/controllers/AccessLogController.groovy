import com.recomdata.util.ExcelGenerator
import com.recomdata.util.ExcelSheet
import org.transmart.AccessLogFilter
import org.transmart.searchapp.AccessLog

import java.text.SimpleDateFormat

class AccessLogController {

	private static final List<String> headers = ['Access Time', 'User', 'Event', 'Event Message'].asImmutable()

	static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']
	static defaultAction = 'list'

	SearchService searchService

	def list() {

		def filter = configureFilter()

		def pageMap = searchService.createPagingParamMap(params, grailsApplication.config.com.recomdata.admin.paginate.max, 0)
		pageMap.sort = 'accesstime'
		pageMap.order = 'desc'

		List<AccessLog> result = AccessLog.createCriteria().list(
				max: pageMap.max,
				offset: pageMap.offset,
				sort: pageMap.sort,
				order: pageMap.order) {
			between 'accesstime', filter.startdate, filter.enddate
		}

		SimpleDateFormat df1 = new SimpleDateFormat('dd/MM/yyyy')
		[accessLogInstanceList: result, startdate: df1.format(filter.startdate),
		 enddate: df1.format(filter.enddate), totalcount: result.totalCount]
	}

	def export() {

		def filter = configureFilter()

		def pageMap = searchService.createPagingParamMap(params, grailsApplication.config.com.recomdata.search.paginate.max, 0)
		pageMap.sort = 'accesstime'
		pageMap.order = 'desc'

		List<AccessLog> results = AccessLog.createCriteria().list(
				sort: pageMap.sort,
				order: pageMap.order) {
			between 'accesstime', filter.startdate, filter.enddate
		}

		def values = []
		for (AccessLog accessLog in results) {
			values << [accessLog.accesstime, accessLog.username, accessLog.event, accessLog.eventmessage]
		}

		response.setHeader('Content-Type', 'application/vnd.ms-excel; charset=utf-8')
		response.setHeader('Content-Disposition', 'attachment; filename="pre_clinical.xls"')
		response.setHeader('Cache-Control', 'must-revalidate, post-check=0, pre-check=0')
		response.setHeader('Pragma', 'public')
		response.setHeader('Expires', '0')

		response.outputStream << new ExcelGenerator().generateExcel([new ExcelSheet('sheet1', headers, values)])
	}

	def show(AccessLog accessLog) {
		if (!accessLog) {
			flash.message = "AccessLog not found with id ${params.id}"
			redirect action: 'list'
		}
		else {
			[accessLogInstance: accessLog]
		}
	}

	def delete(AccessLog accessLog) {
		if (accessLog) {
			accessLog.delete()
			flash.message = "AccessLog $params.id deleted"
		}
		else {
			flash.message = "AccessLog not found with id $params.id"
		}
		redirect action: 'list'
	}

	def edit(AccessLog accessLog) {
		if (!accessLog) {
			flash.message = "AccessLog not found with id $params.id"
			redirect action: 'list'
		}
		else {
			[accessLogInstance: accessLog]
		}
	}

	def update(AccessLog accessLog) {
		if (accessLog) {
			accessLog.properties = params
			if (!accessLog.hasErrors() && accessLog.save()) {
				flash.message = "AccessLog $params.id updated"
				redirect action: 'show', id: accessLog.id
			}
			else {
				render view: 'edit', model: [accessLogInstance: accessLog]
			}
		}
		else {
			flash.message = "AccessLog not found with id $params.id"
			redirect action: 'edit', id: params.id
		}
	}

	def create() {
		[accessLogInstance: new AccessLog(params)]
	}

	def save() {
		AccessLog accessLog = new AccessLog(params)
		if (!accessLog.hasErrors() && accessLog.save()) {
			flash.message = "AccessLog $accessLog.id created"
			redirect action: 'show', id: accessLog.id
		}
		else {
			render view: 'create', model: [accessLogInstance: accessLog]
		}
	}

	private configureFilter() {
		def filter = session.accesslogFilter
		if (filter == null) {
			filter = new AccessLogFilter()
			session.accesslogFilter = filter
		}

		SimpleDateFormat df1 = new SimpleDateFormat('dd/MM/yyyy')
		GregorianCalendar calendar = new GregorianCalendar()

		try {
			if (filter.startdate == null || params.startdate != null) {
				filter.startdate = df1.parse(params.startdate)
			}
		}
		catch (ignored) {
			calendar.time = new Date()
			calendar.add Calendar.WEEK_OF_MONTH, -1
			filter.startdate = calendar.time
		}

		try {
			if (filter.enddate == null || params.enddate != null) {
				calendar.time = df1.parse(params.enddate)
				calendar.set Calendar.HOUR_OF_DAY, 23
				calendar.set Calendar.MINUTE, 59
				filter.enddate = calendar.time
			}
		}
		catch (ignored) {
			filter.enddate = new Date()
		}

		filter
	}
}
