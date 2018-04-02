import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.transmart.plugin.auth0.Auth0Config
import org.transmart.plugin.auth0.Auth0Service
import org.transmart.plugin.auth0.AuthService
import org.transmart.plugin.auth0.UserLevel
import org.transmart.plugin.auth0.UserService
import org.transmart.searchapp.AuthUser
import org.transmartproject.db.log.AccessLogService
import groovy.util.logging.Slf4j

@Slf4j('logger')
class UserProfileController {

	@Autowired private SpringSecurityService springSecurityService
	@Autowired private UserProfileService userProfileService
	@Autowired private Auth0Service auth0Service
	@Autowired private UserService userService
	@Autowired private Auth0Config auth0Config
	@Autowired private AccessLogService accessLogService
	@Autowired private AuthService authService

	// non-recoverable error
	String severeMessage = "Unable to update user information. Contact administrator."

	def index() {
		try{
			Map userDetails
			String token

			token = auth0Service.jwtToken()
			userDetails = userService.currentUserInfo()

            [
				user: userDetails,
				token: token?: "Unable to retrieve token.",
				instanceType: auth0Config.instanceType,
				instanceName: auth0Config.instanceName,
				level: userDetails.level
			]

		}
		catch (Exception e) {
			logger.error 'Caught error "{}" in UserProfile plugin.', e.message, e
			redirect(action: 'basic')
		}
	}

	def save(String email, String firstname, String lastname) {

		try {
			AuthUser authUser

			if (auth0Service){
				authUser = auth0Service.updateUser(email, firstname, lastname, params)
				if (authUser.hasErrors()){
					logger.error  'UserProfile.save() errors: {}', userService.errorStrings(authUser)
					flash.error = "Error occurred while updating user profile. Please try again later or contact administrator if error persists."
				}
				else {
					flash.message = "Profile successfully updated."
				}
			}
			else{
				logger.error 'Missing property Auth0Service. Check configuration settings.'
				flash.error = severeMessage
			}
		}
		catch (Exception ex){
			logger.error 'Error occurred while saving user info: {}', ex.message, ex
			flash.error = severeMessage
		}
		redirect(action: 'index')
	}

	/**
	 * Basic user profile view if auth0Service not available
	 */
	def basic() {
		AuthUser user = AuthUser.findByUsername(springSecurityService.getPrincipal().username)
		UserLevel level = authService ? authService.currentUserLevel() : null

		if (request.post){
			bindData user, params, [include: [
					'enabled', 'username', 'userRealName', 'email',
					'description', 'emailShow', 'authorities', 'changePassword'
			]]
			user.name = user.userRealName
			user.save(flush:true)
			if (user.hasErrors()) {
				def errors = ""
				user.errors.allErrors.each {
					errors << it + " "
				}
				logger.error 'UserProfile.basic() save errors: {}', errors
				flash.error = "Error occurred while updating user profile. Please try again later or contact administrator if error persists."
			}
			else{
				accessLogService.report user.username, "Profile-update", "User profile $user.email has been updated"
				flash.message = "Profile successfully updated."
			}
		}
		[user: user, level: level]
	}

}

