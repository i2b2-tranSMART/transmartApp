package com.recomdata.security

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.userdetails.GormUserDetailsService
import grails.plugin.springsecurity.userdetails.GrailsUserDetailsService
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.hibernate.criterion.CriteriaSpecification
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.transmart.plugin.shared.security.AuthUserDetails
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.users.User
import org.transmartproject.security.BruteForceLoginLockService

/**
 * @author mmcduffie
 */
@Slf4j('logger')
class AuthUserDetailsService implements User, GrailsUserDetailsService, InitializingBean {

	/**
	 * Some Spring Security classes (e.g. RoleHierarchyVoter) expect at least
	 * one role, so we give a user with no granted roles this one which gets
	 * past that restriction but doesn't grant anything. */
	private static final List<? extends GrantedAuthority> NO_ROLES = [GormUserDetailsService.NO_ROLE]

	GrailsApplication grailsApplication
	BruteForceLoginLockService bruteForceLoginLockService

	private String usernamePropertyName
	private Class<?> userClass

    private Closure<User> lazyUser = { ->
        if (!springSecurityService.isLoggedIn()) {
            logger.warn 'User is not logged in; throwing'
            throw new AccessDeniedException('User is not logged in')
        }

        this
    }
    @Lazy private User delegate = lazyUser()

    Long getId() {
        delegate.id
    }

    @Override
	String getRealName() {
		delegate.realName
	}

    @Override
    String getUsername() {
        delegate.username
    }

    @Override
    boolean canPerform(ProtectedOperation protectedOperation, ProtectedResource protectedResource) {
        delegate.canPerform protectedOperation, protectedResource
    }

    @Transactional(readOnly=true, noRollbackFor=[IllegalArgumentException, UsernameNotFoundException])
	UserDetails loadUserByUsername(String username, boolean loadRoles = true) throws UsernameNotFoundException {
		try {
			loadUserByProperty usernamePropertyName, username, loadRoles, true
		}
		catch (UsernameNotFoundException e) {
			String[] splitUsername = username.split('@')
			if (splitUsername.length != 2) {
				throw e
			}

			loadUserByProperty usernamePropertyName, splitUsername[0], loadRoles, true
		}
	}

	@Transactional(readOnly=true, noRollbackFor=[IllegalArgumentException, UsernameNotFoundException])
	UserDetails loadUserByProperty(String property, String value, boolean loadRoles,
	                               boolean ignoreCase = false) throws UsernameNotFoundException {

		logger.info 'Attempting to find user for {} = {}', property, value

		def user = userClass.createCriteria().get {
			eq property, value, [ignoreCase: ignoreCase]

			if (loadRoles) {
				createAlias 'authorities', 'a', CriteriaSpecification.LEFT_JOIN
			}
		}

		if (!user) {
			logger.warn 'User not found with {} = {}', property, value
			throw new UsernameNotFoundException('User not found')
		}

		Collection<GrantedAuthority> authorities = loadRoles ?
				user.authorities*.authority.collect { String name -> new SimpleGrantedAuthority(name) } : []

		if (loadRoles) {
			logger.debug 'Roles for user {} are: {}', user.username, authorities.join(', ') ?: '(none)'
		}

		new AuthUserDetails(user.username, user.passwd, user.enabled,
				true, true, !bruteForceLoginLockService.isLocked(user.username),
				authorities ?: NO_ROLES, user.id, user.userRealName, user.email)
	}

	void afterPropertiesSet() {
		logger.info 'SpringSecurityUtils.securityConfig.userLookup {} ', SpringSecurityUtils.securityConfig.userLookup

		logger.info 'Loading usernamePropertyName object from {}', SpringSecurityUtils.securityConfig.userLookup.usernamePropertyName
		usernamePropertyName = SpringSecurityUtils.securityConfig.userLookup.usernamePropertyName

		logger.info 'Loading userClass object from {}', SpringSecurityUtils.securityConfig.userLookup.userDomainClassName
		userClass = grailsApplication.getDomainClass(SpringSecurityUtils.securityConfig.userLookup.userDomainClassName).clazz
	}
}
