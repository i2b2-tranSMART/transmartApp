package org.transmartproject.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import grails.plugin.springsecurity.SpringSecurityService
import groovy.transform.CompileStatic
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails

/**
 * @author <a href='mailto:burt_beckwith@hms.harvard.edu'>Burt Beckwith</a>
 */
@CompileStatic
class SecurityService implements InitializingBean {

	static transactional = false

	@Autowired private SpringSecurityService springSecurityService

	@Value('${grails.plugin.springsecurity.auth0.clientSecret:}')
	private String clientSecret

	private Algorithm algorithm

	String jwtToken() {
		Date now = new Date()
		JWT.create()
				.withExpiresAt(now + 1) // 'exp'
				.withIssuedAt(now) // 'iat'
				.withIssuer(getClass().name) // 'iss'
				.withClaim('email', ((UserDetails) springSecurityService.principal).username)
				.sign(algorithm)
	}

	void afterPropertiesSet() {
		algorithm = Algorithm.HMAC256(clientSecret)
	}
}
