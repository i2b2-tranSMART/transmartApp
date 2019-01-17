import com.recomdata.security.PSAMATokenAuthenticator
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.WebAttributes
import org.transmart.plugin.shared.security.Roles
import org.transmartproject.db.log.AccessLogService
import org.transmartproject.security.BruteForceLoginLockService

@Slf4j('logger')
class LoginController {

	AccessLogService accessLogService
	AuthenticationTrustResolver authenticationTrustResolver
	BruteForceLoginLockService bruteForceLoginLockService
	SpringSecurityService springSecurityService
	UserDetailsService userDetailsService

	@Value('${com.recomdata.adminEmail:}')
	private String adminEmail

	@Value('${com.recomdata.appTitle:}')
	private String appTitle

	@Value('${ui.loginScreen.disclaimer:}')
	private String disclaimer

	@Value('${com.recomdata.guestAutoLogin:false}')
	private boolean guestAutoLogin

	@Value('${com.recomdata.guestUserName:}')
	private String guestUserName

	@Value('${com.recomdata.largeLogo:}')
	private String largeLogo

	@Value('${com.recomdata.providerLogo:}')
	private String providerLogo

	@Value('${com.recomdata.providerName:}')
	private String providerName

	@Value('${com.recomdata.providerURL:}')
	private String providerUrl

	@Value('${org.transmart.security.samlEnabled:false}')
	private boolean samlEnabled

    @Value('${org.transmart.security.oauthEnabled:true}')
    private boolean oauth_enabled

    @Value('${org.transmart.security.oauth.service_url:}')
    private String oauth_service_url

    @Value('${org.transmart.security.oauth.service_token:}')
    private String oauth_service_token

	@Value('${org.transmart.security.oauth.application_id:}')
	private String oauth_application_id


	@Value('${org.transmart.security.oauth.login_endpoint:}')
    private String oauth_login_endpoint

    private String postUrl = SpringSecurityUtils.securityConfig.apf.filterProcessesUrl
	private String defaultTargetUrl = SpringSecurityUtils.securityConfig.successHandler.defaultTargetUrl
    protected static final GrantedAuthority PUBLIC_USER = new SimpleGrantedAuthority(Roles.PUBLIC_USER.authority)

	/**
	 * Default action; redirects to 'defaultTargetUrl' if logged in, /login/auth otherwise.
	 */
	def index() {
        logger.debug("index() starting")

		if (springSecurityService.isLoggedIn()) {
            logger.debug("index() logged in, redirecting to "+defaultTargetUrl)
			redirect uri: defaultTargetUrl
		}
		else {
            if (oauth_enabled) {
                String oauthRedirectionURL = oauth_service_url+oauth_login_endpoint+'?redirection_url='+createLink(action: "callback", controller:"login", absolute: true)
                logger.debug 'index() redirect back to {}', oauthRedirectionURL
                redirect url: oauthRedirectionURL
            } else {
                // Use regular Grails authentication
                logger.debug("index() not logged in, redirect to /auth")
                redirect action: 'auth', params: params
            }
		}
	}

    def callback() {
        logger.debug '/callback starting'
        def errorMessage = ''
        try {
            logger.debug '/callback service url {}', oauth_service_url
            logger.debug '/callback service token {}', oauth_service_token
            com.recomdata.security.PSAMATokenAuthenticator psamaAuthenticator =
                    new com.recomdata.security.PSAMATokenAuthenticator(params.token, oauth_application_id, oauth_service_url, oauth_service_token)
            logger.debug '/callback Configured new psamaAuthenticator'

            if (psamaAuthenticator.authenticated) {
                logger.debug '/callback PSAMA authenticated the user'
                SecurityContextHolder.getContext().setAuthentication(psamaAuthenticator)
                logger.debug '/callback Saved to SecurityContextHolder'
                redirect action: 'index'
                return
            } else {
                errorMessage = psamaAuthenticator.whyDeniedToAuthenticate()
            }

        } catch (Exception e) {
            logger.error '/callback Exception {}', e.getMessage()
            errorMessage = e.getMessage()
        }

        logger.debug '/callback denied PSAMA login'
        redirect action: 'denied', model: [ error: errorMessage]
    }

	def forceAuth() {
		session.invalidate()
		render view: 'auth', model: authModel()
	}

	/**
	 * Show the login page.
	 */
	def auth() {
		logger.debug("auth() starting")

		nocache response

		boolean forcedFormLogin = request.queryString
		logger.debug 'auth() User is forcing the form login? : {}', forcedFormLogin

		// if enabled guest and not forced login
		if (guestAutoLogin && !forcedFormLogin) {
			logger.info 'proceeding with auto guest login'

			try {
				UserDetails ud = userDetailsService.loadUserByUsername(guestUserName)
				logger.debug 'We have found user: {}', ud.username
				springSecurityService.reauthenticate ud.username
				redirect uri: defaultTargetUrl
				return
			}
			catch (UsernameNotFoundException ignored) {
				logger.info 'can not find the user: {}', guestUserName
			}
		}
        logger.debug("auth() calling authModel()")
		authModel()
        logger.debug("auth() finished")
	}

	/**
	 * Show denied page.
	 */
	def denied() {
        logger.debug("denied() starting")
		if (springSecurityService.isLoggedIn() &&
				authenticationTrustResolver.isRememberMe(SCH.context?.authentication)) {
			// have cookie but the page is guarded with IS_AUTHENTICATED_FULLY
			redirect action: 'full', params: params
		}
        logger.debug("denied() finished")
	}

	/**
	 * Login page for users with a remember-me cookie but accessing a IS_AUTHENTICATED_FULLY page.
	 */
	def full() {
		render view: 'auth', params: params, model: authModel()
	}

	/**
	 * Callback after a failed login. Redirects to the auth page with a warning message.
	 */
	def authfail() {
		String msg = ''
		def exception = session[WebAttributes.AUTHENTICATION_EXCEPTION]
		String username = null
		if (exception instanceof AuthenticationException) {
			username = exception.authentication.name
		}
		if (exception) {
			if (exception instanceof AccountExpiredException) {
				msg = message(code: 'springSecurity.errors.login.expired')
				accessLogService.report username, 'Account Expired', msg
			}
			else if (exception instanceof CredentialsExpiredException) {
				msg = message(code: 'springSecurity.errors.login.passwordExpired')
				accessLogService.report username, 'Password Expired', msg
			}
			else if (exception instanceof DisabledException) {
				msg = message(code: 'springSecurity.errors.login.disabled')
				accessLogService.report username, 'Login Disabled', msg
			}
			else if (exception instanceof LockedException
					//Extra condition to escape confusion with last login attempt that would be ignored anyway
					// because user would be locked at that time.
					//That's confusion caused by the fact that spring event listener for failed attempt is triggered
					// after user status (e.g. locked) is read by spring security.
					|| username && bruteForceLoginLockService.remainedAttempts(username) <= 0) {
				msg = message(code: 'springSecurity.errors.login.locked',
						args: [bruteForceLoginLockService.lockTimeInMinutes])
				accessLogService.report username, 'Login Locked', msg
			}
			else {
				msg = message(code: 'springSecurity.errors.login.fail')
				accessLogService.report username, 'Login Failed', msg
			}
		}
		flash.message = msg
		redirect action: 'auth', params: params
	}

	/** cache controls */
	private void nocache(response) {
		response.setHeader('Cache-Control', 'no-cache') // HTTP 1.1
		response.addDateHeader('Expires', 0)
		response.setDateHeader('max-age', 0)
		response.setIntHeader('Expires', -1) //prevents caching at the proxy server
		response.addHeader('cache-Control', 'private') //IE5.x only
	}

	private Map authModel() {
		logger.debug("authModel() starting")
		[postUrl     : request.contextPath + postUrl,
		 hasCookie   : authenticationTrustResolver.isRememberMe(SCH.context?.authentication),
		 adminEmail  : adminEmail,
		 appTitle    : appTitle,
		 disclaimer  : disclaimer,
		 largeLogo   : largeLogo,
		 providerLogo: providerLogo,
		 providerName: providerName,
		 providerUrl : providerUrl,
		 samlEnabled : samlEnabled]
	}
}
