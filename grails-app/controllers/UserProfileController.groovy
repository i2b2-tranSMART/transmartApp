import org.transmart.searchapp.AuthUser
import org.transmartproject.db.log.AccessLogService

class UserProfileController {

	def springSecurityService
	def userProfileService
	def auth0Service
	def userService
	def auth0Config
	AccessLogService accessLogService

	// non-recoverable error
	String severeMessage = "Unable to update user information. Contact administrator."

	def index() {
		try{
			def userDetails
			def token

			token = auth0Service.jwtToken()
			userDetails = userService.currentUserInfo()

            model:[
				user: userDetails,
				token: token?: "Unable to retrieve token.",
				instanceType: auth0Config.instanceType,
				instanceName: auth0Config.instanceName,
				level: userDetails.level
			]

		}
		catch (Exception e) {
			log.error("Caught error in UserProfile plugin.", e);
			redirect(action: 'basic')
		}
	}

	def save() {

		try {
			AuthUser authUser
			String email     = params.email
			String firstname = params.firstname
			String lastname  = params.lastname

			if (auth0Service){
				authUser = auth0Service.updateUser(email, firstname, lastname, params)
				if (authUser.hasErrors()){
					log.error("UserProfile.save() errors: " + userService.errorStrings(authUser))
					flash.error = "Error occurred while updating user profile. Please try again later or contact administrator if error persists."
				}
				else {
					flash.message = "Profile successfully updated."
				}
			}
			else{
				log.error("Missing property Auth0Service. Check configuration settings.")
				flash.error = severeMessage
			}
		}
		catch (Exception ex){
			log.error(ex.message, ex)
			flash.error = severeMessage
		}
		redirect(action: 'index')
	}

	/**
	 * Basic user profile view if auth0Service not available
	 */
	def basic() {
		AuthUser user = AuthUser.findByUsername(springSecurityService.getPrincipal().username)
		def level = userProfileService.getAuthUserLevel()

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
				log.error("UserProfile.basic() save errors: " + errors)
				flash.error = "Error occurred while updating user profile. Please try again later or contact administrator if error persists."
			}
			else{
				accessLogService.report user.username, "Profile-update", "User profile $user.email has been updated"
				flash.message = "Profile successfully updated."
			}
		}
		model:[user: user, level: level]
	}

}

