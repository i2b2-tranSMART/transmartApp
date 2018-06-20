import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.transaction.TransactionStatus
import org.springframework.util.Assert
import org.transmart.plugin.auth0.Auth0Config
import org.transmart.plugin.shared.SecurityService
import org.transmart.plugin.shared.UtilService
import org.transmart.plugin.shared.security.Roles
import org.transmart.searchapp.AuthUser
import org.transmart.searchapp.Role
import org.transmartproject.db.log.AccessLogService
import org.transmartproject.security.OAuth2SyncService

import javax.servlet.ServletContext
import java.util.logging.Level

@Slf4j('logger')
class BootStrap {

	AccessLogService accessLogService
	Auth0Config auth0Config
	GrailsApplication grailsApplication
	OAuth2SyncService OAuth2SyncService
	SecurityContextPersistenceFilter securityContextPersistenceFilter
	SecurityService securityService
	SpringSecurityService springSecurityService
	UtilService utilService

	def init = { ServletContext servletContext ->
		configureJwt()
		configureGroovySqlLogging()
		configureSecurity()
		checkConfigFine()
		fixupConfig servletContext
		forceMarshallerRegistrarInitialization()
		autoCreateAdmin()
	}

	private void fixupConfig(ServletContext servletContext) {
		ConfigObject config = grailsApplication.config

		// rScriptDirectory
		def val = config.com.recomdata.transmart.data.export.rScriptDirectory
		if (val) {
			logger.warn 'com.recomdata.transmart.data.export.rScriptDirectory should not be explicitly set, value "{}" ignored', val
		}

		File tsAppRScriptsDir = null

		String basePath = [
				servletContext.getRealPath('/'),
				servletContext.getRealPath('/') + '../',
				servletContext.getResource('/')?.file,
				'webapps' + servletContext.contextPath,
				'web-app/'
		].find { String s ->
			s && (tsAppRScriptsDir = new File(s, 'dataExportRScripts')).isDirectory()
		}

		if (!tsAppRScriptsDir || !tsAppRScriptsDir.isDirectory()) {
			throw new RuntimeException('Could not determine proper for com.recomdata.transmart.data.export.rScriptDirectory')
		}
		config.com.recomdata.transmart.data.export.rScriptDirectory = tsAppRScriptsDir.canonicalPath

		logger.info 'com.recomdata.transmart.data.export.rScriptDirectory = {}',
				config.com.recomdata.transmart.data.export.rScriptDirectory

		// set RModules.external=true in config in cases like running
		// in Docker where a localhost temp directory isn't needed
		if (config.RModules.external != true) {
			// RModules.pluginScriptDirectory
			File rScriptsDir
			val = config.RModules.pluginScriptDirectory
			if (val) {
				rScriptsDir = new File(val.toString())
			}
			else {
				File rdcModulesDir = GrailsPluginUtils.getPluginDirForName('rdc-rmodules')?.file
				if (!rdcModulesDir) {
					// it actually varies...
					rdcModulesDir = GrailsPluginUtils.getPluginDirForName('rdcRmodules')?.file
				}
				if (!rdcModulesDir) {
					String version = grailsApplication.mainContext.pluginManager.allPlugins.find {
						it.name == 'rdc-rmodules' || it.name == 'rdcRmodules'
					}.version
					rdcModulesDir = new File(basePath + '/plugins', 'rdc-rmodules-' + version)
				}
				if (!rdcModulesDir) {
					throw new RuntimeException('Could not determine directory for rdc-rmodules plugin')
				}

				rScriptsDir = new File(rdcModulesDir, 'Rscripts')
				if (!rScriptsDir || !rScriptsDir.isDirectory()) {
					rScriptsDir = new File(rdcModulesDir, 'web-app/Rscripts')
				}
				config.RModules.pluginScriptDirectory = rScriptsDir.canonicalPath
			}

			Assert.isTrue rScriptsDir.isDirectory(), 'RModules.pluginScriptDirectory value "' +
					config.RModules.pluginScriptDirectory + '" is not a directory'

			String pluginScriptDirectory = config.RModules.pluginScriptDirectory
			if (!pluginScriptDirectory.endsWith('/')) {
				pluginScriptDirectory += '/'
			}
			logger.info 'RModules.pluginScriptDirectory = {}', pluginScriptDirectory
		}

		// At this point we assume c.RModules exists
		if (!config.RModules.containsKey('host')) {
			config.RModules.host = '127.0.0.1'
			logger.info 'RModules.host fixed to localhost'
		}
		if (!config.RModules.containsKey('port')) {
			config.RModules.port = 6311
			logger.info 'RModules.port fixed to default'
		}

		// Making sure we have default timeout and heartbeat values
		// At this point we assume c.recomdata exists
		if (!config.com.recomdata.containsKey('sessionTimeout')) {
			config.com.recomdata.sessionTimeout = 3600
		}
		if (!config.com.recomdata.containsKey('heartbeatLaps')) {
			config.com.recomdata.heartbeatLaps = 60
		}
	}

	private void configureJwt() {
		UsernamePasswordAuthenticationToken.metaClass.getJwtToken = { -> securityService.jwtToken() }
		RememberMeAuthenticationToken.metaClass.getJwtToken = { -> securityService.jwtToken() }
	}

	private void configureGroovySqlLogging() {
		if (grailsApplication.config.grails.logging.jul.usebridge) {
			Sql.LOG.level = Level.FINE
		}
	}

	private void configureSecurity() {
		securityContextPersistenceFilter.forceEagerSessionCreation = true

		SpringSecurityUtils.clientRegisterFilter(
				'concurrentSessionFilter', SecurityFilterPosition.CONCURRENT_SESSION_FILTER)

		if (grailsApplication.config.org.transmart.security.samlEnabled) {
			SpringSecurityUtils.clientRegisterFilter(
					'metadataGeneratorFilter', SecurityFilterPosition.FIRST)
			SpringSecurityUtils.clientRegisterFilter(
					'samlFilter', SecurityFilterPosition.BASIC_AUTH_FILTER)
		}

		if ('clientCredentialsAuthenticationProvider' in
				grailsApplication.config.grails.plugin.springsecurity.providerNames) {
			OAuth2SyncService.syncOAuth2Clients()
		}
	}

	private void checkConfigFine() {
		if (!grailsApplication.config.org.transmart.configFine.is(true)) {
			logger.error 'Something wrong happened parsing the externalized ' +
					'Config.groovy, because we could not find the ' +
					'''configuration setting 'org.transmart.configFine' ''' +
					'set to true.\n' +
					'Tip: on ~/.grails/transmartConfig, run\n' +
					'''groovy -e 'new ConfigSlurper().parse(new File("Config.groovy").toURL())'\n''' +
					'to detect compile errors. Other errors can be detected ' +
					'with a breakpoing on the catch block in ConfigurationHelper::mergeInLocations().\n' +
					'Alternatively, you can change the console logging settings by editing ' +
					'$GRAILS_HOME/scripts/log4j.properties, adding a proper appender and log ' +
					'org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper at level WARN'
			throw new GrailsConfigurationException('Configuration magic setting not found')
		}
	}

	private void forceMarshallerRegistrarInitialization() {
		grailsApplication.mainContext.getBean 'marshallerRegistrarService'
	}

	private void autoCreateAdmin() {
		if (!auth0Config?.autoCreateAdmin) {
			logger.info 'Auth0 is disabled, or admin auto-create is disabled, not creating admin user'
			return
		}

		if (AuthUser.countByUsername('admin')) {
			logger.info 'admin auto-create is enabled but admin user exists, not creating admin user'
			return
		}

		String password = auth0Config.autoCreateAdminPassword
		if (!password) {
			logger.error 'admin auto-create is enabled but no password is specified, cannot create admin user'
			return
		}

		String email = auth0Config.autoCreateAdminEmail // can be null

		// don't double-hash
		boolean hashed = (password.length() == 59 || password.length() == 60) &&
				(password.startsWith('$2a$') || password.startsWith('$2b$') || password.startsWith('$2y$'))

		AuthUser admin = new AuthUser(email: email ?: null, enabled: true, name: 'System admin',
				passwd: hashed ? passwd : springSecurityService.encodePassword(password),
				uniqueId: 'admin', userRealName: 'System admin', username: 'admin')

		String errorMessage = createAdmin(admin)
		if (errorMessage) {
			accessLogService.report 'admin auto-create', errorMessage
		}
	}

	private String createAdmin(AuthUser admin) {
		AuthUser.withTransaction { TransactionStatus tx -> // TODO move to tx service
			if (admin.save(flush: true)) {
				Role.findByAuthority(Roles.ADMIN.authority).addToPeople admin
				logger.info 'auto-created admin user'
				accessLogService.report 'admin auto-create', 'created admin user'
				return null
			}

			tx.setRollbackOnly()
			String message = 'auto-create admin user failed: ' + utilService.errorStrings(admin)
			logger.error message
			return message
		}
	}
}
