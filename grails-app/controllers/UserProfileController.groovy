import org.springframework.transaction.TransactionStatus
import org.transmart.searchapp.AuthUser
import org.transmartproject.db.log.AccessLogService

class UserProfileController {

	def springSecurityService
	def userProfileService
	def grailsApplication
	def auth0Service
	def userService
	def auth0Config
	AccessLogService accessLogService

	// non recoverable error
	String severeMessage = "Unable to update user information. Contact administrator."

	def index() {
		try{
			def userDetails
			def credentials

			if (auth0Service){
				credentials = auth0Service.credentials()
				userDetails = userService.currentUserInfo(springSecurityService.getPrincipal().username)
			}
			else{
				redirect(action: 'basic')
			}

            model:[
				user: userDetails,
				credentials: credentials,
				instanceType: auth0Config.instanceType,
				instanceName: auth0Config.instanceName,
				level: credentials.level
			]

		}
		catch (Exception e) {
			log.error("Caught error in UserProfile plugin. ", e);
		}
	}

	def save() {

		try {
			AuthUser authUser
			String email 	= params.firstname
			String firstname= params.lastname
			String lastname = params.email

			if (auth0Service){
				authUser = auth0Service.updateUser(springSecurityService.getPrincipal().username, email, firstname, lastname, params)
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

