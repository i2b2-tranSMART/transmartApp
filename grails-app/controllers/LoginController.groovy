import com.recomdata.security.PSAMATokenAuthenticator
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
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
import org.transmart.plugin.custom.CustomizationConfig
import org.transmart.plugin.shared.security.Roles
import org.transmartproject.db.log.AccessLogService
import org.transmartproject.security.BruteForceLoginLockService

import static java.lang.String.join

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

	@Value('${com.recomdata.largeLogo:/static/transmartlogoHMS.jpg}')
	private String largeLogo

	@Value('${com.recomdata.providerLogo:}')
	private String providerLogo

	@Value('${com.recomdata.providerName:}')
	private String providerName

	@Value('${com.recomdata.providerURL:}')
	private String providerUrl

	@Value('${org.transmart.security.samlEnabled:false}')
	private boolean samlEnabled

    @Value('${org.transmart.security.oauthEnabled:false}')
    private boolean oauth_enabled

    @Value('${org.transmart.security.oauth.service_token:}')
    private String oauth_service_token

    @Value('${org.transmart.security.oauth.login_endpoint:}')
    private String oauth_login_endpoint

    @Value('${org.transmart.security.oauth.logout_endpoint:}')
    private String oauth_logout_endpoint

    @Value('${org.transmart.security.oauth.tokeninspect_endpoint:}')
    private String oauth_tokeninspect_endpoint

    private String postUrl = SpringSecurityUtils.securityConfig.apf.filterProcessesUrl
	private String defaultTargetUrl = SpringSecurityUtils.securityConfig.successHandler.defaultTargetUrl
    protected static final GrantedAuthority PUBLIC_USER = new SimpleGrantedAuthority(Roles.PUBLIC_USER.authority)

    @Autowired private CustomizationConfig customizationConfig

	def about() {
		def details = [
                params: params
        ]

        def configDetails = 'options: [security|custom|com|edu|org]'
        switch (params.config) {
            case 'security':
                configDetails = SpringSecurityUtils.securityConfig;
                break;
            case 'custom':
                configDetails = applicationContext.customizationConfig;
                break;
            case 'com':
                configDetails = grailsApplication.config.com;
                break;
            case 'edu':
                configDetails = grailsApplication.config.edu;
                break;
            case 'org':
                configDetails = grailsApplication.config.org.transmart.security;
                break;
        }
        details.config = configDetails

        render details as JSON
	}

	/**
	 * Default action; redirects to 'defaultTargetUrl' if logged in, /login/auth otherwise.
	 */
	def index() {
        logger.debug '/index starting'

		if (springSecurityService.isLoggedIn()) {
            logger.debug '/index logged in, redirecting to {}', defaultTargetUrl
			redirect uri: defaultTargetUrl
		}
		else {
            if (oauth_enabled) {
                logger.debug '/index redirecting to {}', oauth_login_endpoint
                redirect url: oauth_login_endpoint
            } else {
                // Use regular Grails authentication
                logger.debug '/index not logged in, redirect to /auth'
                redirect action: 'auth', params: params
            }
		}
	}

	def callback_processor() {
        [
                isPsamaEnabled: oauth_enabled,
                mysesison:session
        ]

	}

    def callback() {
        logger.debug '/callback starting, params: {}', params

        if (params.token == null || params.token == '') {
            logger.error '/callback missing `token` parameter'
            flash.error ='No token received from OAuth provider service.'
        } else {

            try {
                logger.debug '/callback inspect user token'
                com.recomdata.security.PSAMATokenAuthenticator psamaAuthenticator =
                        new com.recomdata.security.PSAMATokenAuthenticator(
								params.token,
								oauth_tokeninspect_endpoint,
								oauth_service_token
						)
                logger.debug '/callback Configured new psamaAuthenticator'

                if (psamaAuthenticator.authenticated) {
                    logger.debug '/callback PSAMA authenticated the user'
                    SecurityContextHolder.getContext().setAuthentication(psamaAuthenticator)
                    logger.debug '/callback Saved to SecurityContextHolder'
                    redirect action: 'index'
                    return
                } else {
                    def errorMessage = psamaAuthenticator.whyDeniedToAuthenticate()
                    flash.error errorMessage
                    logger.error '/callback PSAMA could not authenticate, because {}', errorMessage
                }

            } catch (Exception e) {
                logger.error '/callback Exception {}', e.getMessage()
                flash.error = e.getMessage()
            }
            logger.debug '/callback denied PSAMA login'
            redirect action: 'denied'
        }
    }

	def forceAuth() {
		session.invalidate()
		render view: 'auth', model: authModel()
	}

	/**
	 * Show the login page.
	 */
	def auth() {
		logger.debug("/auth starting")

		nocache response

		if (oauth_enabled) {
			redirect action: callback_processor()
		}

		boolean forcedFormLogin = request.queryString
		logger.debug '/auth User is forcing the form login? : {}', forcedFormLogin

		// if enabled guest and not forced login
		if (guestAutoLogin && !forcedFormLogin) {
			logger.info 'proceeding with auto guest login'

			try {
				UserDetails ud = userDetailsService.loadUserByUsername(guestUserName)
				logger.debug '/auth We have found user: {}', ud.username
				springSecurityService.reauthenticate ud.username
				redirect uri: defaultTargetUrl
				return
			}
			catch (UsernameNotFoundException ignored) {
				logger.info 'can not find the user: {}', guestUserName
			}
		}
        logger.debug("/auth calling authModel()")
		authModel()
        logger.debug("/auth finished")
	}

    private void listParams(Enumeration<String> list, String label) {
        while (list.hasMoreElements()) {
            String key = list.nextElement()
            logger.debug '/denied \trequest.{}Name {}', label, key
        }

    }
	/**
	 * Show denied page.
	 */
	def denied() {
        logger.debug '/denied starting'

        // TODO: Not sure why this `if` statement is here?! This step is already on the `denied login` path!
        if (springSecurityService.isLoggedIn() &&
				authenticationTrustResolver.isRememberMe(SCH.context?.authentication)) {
			// have cookie but the page is guarded with IS_AUTHENTICATED_FULLY
			redirect action: 'full', params: params
		}

        def errMessage = 'Unauthorized request'
        if (session.getAttribute('authentication_denied_message')) {
            errMessage = (String) session.getAttribute('authentication_denied_message')
        }
        logger.debug '/denied finished, {}', errMessage
        [errorMessage: errMessage]
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
        [
                postUrl     : request.contextPath + postUrl,
                hasCookie   : authenticationTrustResolver.isRememberMe(SCH.context?.authentication),
                adminEmail  : adminEmail,
                appTitle    : appTitle,
                disclaimer  : disclaimer,
                largeLogo   : largeLogo,
                providerLogo: providerLogo,
                providerName: providerName,
                providerUrl : providerUrl,
                samlEnabled : samlEnabled
        ]
    }
}
