import com.auth0.jwt.JWT
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import org.transmart.plugin.auth0.Auth0Service
import org.transmart.plugin.auth0.UserService

import org.transmart.plugin.custom.CustomizationConfig
import org.transmart.plugin.custom.CustomizationService
import org.transmart.plugin.custom.UserLevel
import org.transmart.plugin.shared.SecurityService
import org.transmart.plugin.shared.UtilService
import org.transmart.plugin.shared.security.AuthUserDetails
import org.transmart.plugin.shared.security.Roles
import org.transmart.searchapp.AuthUser
import org.transmartproject.db.log.AccessLogService

@Slf4j('logger')
class UserProfileController {

	// non-recoverable error
	private static final String severeMessage = 'Unable to update user information. Contact administrator.'

	@Autowired private AccessLogService accessLogService
	@Autowired private Auth0Service auth0Service
	@Autowired private CustomizationConfig customizationConfig
	@Autowired private CustomizationService customizationService
	@Autowired private SecurityService securityService
	@Autowired private UserService userService
	@Autowired private UtilService utilService

    @Value('${org.transmart.security.oauthEnabled:true}')
    private boolean oauth_enabled

	private boolean auth0Enabled = SpringSecurityUtils.securityConfig.auth0.active

	def index() {
		try {
            logger.debug '/index current UserLevel: {}', customizationService.currentUserLevel()

			[user        : oauth_enabled?getOAuthUser(securityService.principal()):userService.currentUserInfo(),
			 token       : jwtToken(),
			 instanceType: customizationConfig.instanceType,
			 instanceName: customizationConfig.instanceName,
			 level       : customizationService.currentUserLevel()
			]
		} catch (e) {
			logger.error '/index Exception "{}" in UserProfileController.', e.message, e
			redirect action: 'basic'
		}
	}

	def save(String email, String firstname, String lastname) {
		try {
			AuthUser authUser = userService.updateUser(email, firstname, lastname, params)
			if (authUser.hasErrors()) {
				logger.error 'UserProfile.save() errors: {}', utilService.errorStrings(authUser)
				flash.error = 'Error occurred while updating user profile. Please try again later or contact administrator if error persists.'
			}
			else {
				flash.message = 'Profile successfully updated.'
			}
		}
		catch (e) {
			logger.error 'Error occurred while saving user info: {}', e.message, e
			flash.error = severeMessage
		}

		redirect action: 'index'
	}

	/**
	 * Basic user profile view if auth0Service not available
	 */
	def basic() {
		logger.debug '/basic starting'

        AuthUserDetails principal = securityService.principal()
        AuthUser user = null

		logger.debug '/basic auth {} ', securityService.principal().authorities
        UserLevel userLevel = customizationService.currentUserLevel()

        if (!oauth_enabled) {
            // Pull additional user information from the database, based on the
            // username (as index)
            user = AuthUser.findByUsername(securityService.currentUsername())
            logger.debug '/basic user (from database): {}', user
            if (request.post) {
                bindData user, params, [include: [
                        'enabled', 'username', 'userRealName', 'email',
                        'description', 'emailShow', 'authorities', 'changePassword'
                ]]
                user.name = user.userRealName
                user.save(flush: true)

                if (user.hasErrors()) {
                    logger.error '/basic save errors: {}', utilService.errorStrings(user)
                    flash.error = 'Error occurred while updating user profile. Please try again later or contact administrator if error persists.'
                }
                else {
                    accessLogService.report user.username, 'Profile-update', "User profile $user.email has been updated"
                    flash.message = 'Profile successfully updated.'
                }
            }

        } else {
            // Use only the principal, and do not look up additional information in the database.
            logger.debug '/basic OAuth Principal: {}', principal
            user = getOAuthUser(principal)
        }

        // Use the `AuthUser` object, to display information about the current user.
        logger.debug '/basic current user_level: {}', userLevel

		[
				user: user,
				level: userLevel
		]
	}

	private String jwtToken() {
		(auth0Enabled ? auth0Service.jwtToken() : securityService.jwtToken()) ?: 'Unable to retrieve token.'
	}

	private String jwtTokenClaims() {
		logger.debug 'jwtTokenClaims() get current token {}', JWT.decode(jwtToken())
		JWT.decode(jwtToken()).claims.get('exp')
	}

	private Map getOAuthUser(AuthUserDetails currentUserDetails) {
		// Get the current user, but NOT from the database, but rather from the SpringSecurityContext
		// Basically, just return the principal
        [
				userRealName : currentUserDetails.userRealName?:'unknown real name data',
				email : currentUserDetails.email?:'no e-mail',
				username : currentUserDetails.username?:'no username provided',
                firstname: 'FirstName',
                lastname: 'LastName'
        ]
	}
}
