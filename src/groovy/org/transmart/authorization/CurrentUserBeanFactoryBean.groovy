package org.transmart.authorization

import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.transform.CompileStatic
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.Assert
import org.transmart.plugin.shared.SecurityService
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

import javax.annotation.PostConstruct

/**
 * Should be request scoped!
 */
@CompileStatic
class CurrentUserBeanFactoryBean implements FactoryBean<User> {

	@Autowired SecurityService securityService
	@Autowired UsersResource usersResource

    @Value('${org.transmart.security.oauthEnabled:true}')
    private boolean oauth_enabled

    final boolean singleton = true

	final Class<?> objectType = User

	private User user

	@PostConstruct
	void fetchUser() {

		Assert.state securityService != null, 'securityService not injected'
		Assert.state SpringSecurityUtils.securityConfig.active as boolean, 'Spring Security not active'
		Assert.state securityService.loggedIn(), 'User is not logged in'

        // If oauth is enabled, the user is NOT from the database, but rather from an OAuth provider. There is
        // no need to look it up in the database, and rather take it from the SpringSecurity context.
		if (oauth_enabled) {
			// No need to fetch from the database, just return the principal
			user = securityService.principal()
		} else {
			// Fetch the user from the database
			user = usersResource.getUserFromUsername(securityService.currentUsername())
		}
	}

	User getObject() {
		user
	}
}
