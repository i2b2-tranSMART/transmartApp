import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.db.log.AccessLogService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Logout Controller just writes an entry to the log and redirects to the login page (Identity Vault or form based)
 */
@Slf4j('logger')
class LogoutController {
    @Autowired private List<LogoutHandler> logoutHandlers
	AccessLogService accessLogService

	/**
	 * Index action. Redirects to the Spring security logout uri.
	 */
	def index() {
		accessLogService.report 'Logout', null
		redirect uri: SpringSecurityUtils.securityConfig.logout.filterProcessesUrl
	}

	def psama() {
		logger.debug '/logout/psama Starting ...'
        accessLogService.report 'Logout', 'Logging out user and redirecting to PSAMA.'

        HttpServletRequest request = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes().request
        HttpServletResponse response = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes().response
        Authentication authentication = SecurityContextHolder.context.authentication
        for (LogoutHandler handler in logoutHandlers) {
            handler.logout request, response, authentication
        }
        logger.debug '/logout/psama Finished'

        // TODO: This should be parameterized
        redirect url: request.getScheme() + "://" + request.getServerName() + '/psamaui/logout'
        return
	}
}
