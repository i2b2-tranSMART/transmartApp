package com.recomdata.security

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.util.logging.Slf4j
import org.restlet.security.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.transmart.plugin.custom.UserLevel
import org.transmart.plugin.shared.security.AuthUserDetails
import org.transmart.plugin.shared.security.Roles
import us.monoid.json.JSONArray
import us.monoid.json.JSONObject
import us.monoid.web.Resty

@Slf4j('logger')
class PSAMATokenAuthenticator implements Authentication, CredentialsContainer {

    SpringSecurityService springSecurityService

    Object credentials
    Object username
    Object principal
    Object details
    Collection<GrantedAuthority> authorities = []
    Boolean authenticated = false

    String oauth_service_url
    String oauth_service_token
    String oauth_application_id
    String errorMessage

    public PSAMATokenAuthenticator(String userToken, String oauth_application_id, String oauth_service_url, String oauth_service_token) {
        logger.debug '_constructor Starting'
        this.oauth_service_url = oauth_service_url
        this.oauth_service_token = oauth_service_token
        this.oauth_application_id = oauth_application_id

        logger.debug '_constructor serviceURL: {}', this.oauth_service_url
        logger.debug '_constructor serviceToken: {}', this.oauth_service_token
        logger.debug '_constructor oauthApplication: {}', this.oauth_application_id
        logger.debug '_constructor userToken: {}', userToken

        try {
            JSONObject userObject = getUserByToken(userToken)
            logger.debug '_constructor userObject: {}', userObject

            if (!userObject.getBoolean('active')) {
                this.errorMessage = 'You cannot be authenticated with the PSAMA service.'
            } else {
                logger.debug '_constructor User is an `active` user in PSAMA'
            }

            // Build SpringSecurity Principal object from userObject
            long userId = 6666

            // Build privileges list, if any exists
            Collection<GrantedAuthority> tmRoleList = []
            if (userObject.getJSONArray("privileges").length() == 0) {
                throw new RuntimeException('The user has no privilege to access this application')
            } else {
                // Convert OAuthProvider privileges to i2b2/tranSmart auhtorities
                JSONArray pa = userObject.getJSONArray("privileges")
                for(int i=0;i<pa.length();i++) {
                    if (!pa.getString(i).startsWith('ROLE_')) {
                        // Only use privileges that does not start with ROLE (don't ask!!!)
                        tmRoleList.add(new SimpleGrantedAuthority('ROLE_' + pa.getString(i)))
                    }
                }
                logger.debug '_constructor `tmRoleList` collection is now {}', tmRoleList
            }

            AuthUserDetails usrdtls = new AuthUserDetails(
                    userObject.getString('sub'),
                    'psama',
                    true,
                    true,
                    true,
                    true,
                    tmRoleList,
                    userId,
                    userObject.getString('name')?:userObject.getString('sub'),
                    userObject.getString('email')?:'no_email_in_profile')

            logger.debug '_constructor principal auth {}', usrdtls.authorities
            logger.debug '_constructor principal {}', usrdtls

            this.principal = usrdtls
            this.credentials = userToken
            this.authenticated = true
            this.authorities = tmRoleList
            logger.debug '_constructor finished authenticating by user'
        } catch (Exception e) {
            logger.error '_constructor Exception {}', e.getMessage()
            this.errorMessage = e.getMessage()
        }
        logger.debug '_constructor Finished'
    }

    public String whyDeniedToAuthenticate() {
        return this.errorMessage
    }

    @Override
    boolean isAuthenticated() {
        return authenticated
    }

    @Override
    void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated
    }


    @Override
    String getName() {
        return this.username
    }

    @Override
    void eraseCredentials() {
        if (this.credentials instanceof CredentialsContainer) {
            ((CredentialsContainer)this.credentials).eraseCredentials();
        }
    }

    JSONObject getUserByToken(String userToken) {
        logger.debug 'getUserByToken() starting, with userToken:{}', userToken

        // Using a service (based on the configured URL) for token introspection
        String tokenintrospectionURL = this.oauth_service_url + '/picsureauth/token/inspect' //?applicationId='+this.oauth_application_id
        logger.debug 'getUserByToken() token introspection url: {}', tokenintrospectionURL

        JSONObject tokenInfo = null
        try {
            Resty resty = new Resty()
            logger.debug 'getUserByToken() using service token {}', this.oauth_service_token
            resty.withHeader('Authorization','Bearer ' + this.oauth_service_token)

            logger.debug 'getUserByToken() using user token {}', userToken
            tokenInfo = resty.json(tokenintrospectionURL, Resty.content(new JSONObject(token: userToken))).toObject()

        } catch (Exception e) {
            logger.error 'getUserByToken() exception: {}', e.getMessage()
            this.errorMessage = e.getMessage()
        }

        logger.debug 'getUserByToken() response {}', tokenInfo
        return tokenInfo
    }

}
