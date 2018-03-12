import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.context.MessageSource
import org.springframework.web.servlet.support.RequestContextUtils
import org.transmartproject.db.log.AccessLogService

class UserLandingController {

	AccessLogService accessLogService
	MessageSource messageSource
	SpringSecurityService springSecurityService

	private String getUserLandingPath() {
		grailsApplication.config.with {
			com.recomdata.defaults.landing ?: ui.tabs.browse.hide ? '/datasetExplorer' : '/RWG'
		}
	}

	def index = {
		accessLogService.report "Login", request.getHeader("user-agent")
		def skip_disclaimer = grailsApplication.config.com.recomdata?.skipdisclaimer ?: false;
		if (skip_disclaimer) {
			if (springSecurityService?.currentUser?.changePassword) {
				flash.message = messageSource.getMessage('changePassword', new Objects[0], RequestContextUtils.getLocale(request))
				redirect(controller: 'changeMyPassword')
			}
			else {
				redirect(uri: userLandingPath)
			}
		}
		else {
			redirect(uri: '/userLanding/disclaimer.gsp')
		}
	}
	def agree = {
		accessLogService.report "Disclaimer accepted", null
		if (springSecurityService?.currentUser?.changePassword) {
			flash.message = messageSource.getMessage('changePassword', new Objects[0], RequestContextUtils.getLocale(request))
			redirect(controller: 'changeMyPassword')
		}
		else {
			redirect(uri: userLandingPath)
		}
	}

	def disagree = {
		accessLogService.report "Disclaimer not accepted", null
		redirect(uri: '/logout')
	}

	def checkHeartBeat = {
		render(text: "OK")
	}
}
