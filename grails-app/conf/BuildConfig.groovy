import grails.util.Environment

def forkSettingsRun   = [minMemory: 1536, maxMemory: 4096, maxPerm: 384, debug: false]
def forkSettingsOther = [minMemory:  256, maxMemory: 1024, maxPerm: 384, debug: false]

def dm
try {
	Class dmClass = new GroovyClassLoader().parseClass(
			new File('../transmart-dev/DependencyManagement.groovy'))
	dm = dmClass?.newInstance()
}
catch (ignored) {}

grails.project.dependency.resolver = 'maven'
grails.project.fork = [test: forkSettingsOther, run: forkSettingsRun, war: false, console: forkSettingsOther]
grails.project.source.level = 1.7
grails.project.target.level = 1.7
grails.project.war.file = "target/${appName}.war"
grails.project.work.dir = 'target'
grails.servlet.version = '3.0'

grails.project.dependency.resolution = {
	inherits 'global'
	log 'warn'
	checksums true
	legacyResolve false

	if (!dm) {
		repositories {
			mavenLocal()
			grailsCentral()
			mavenCentral()
			mavenRepo 'http://ec2-35-170-59-132.compute-1.amazonaws.com:8080/artifactory/libs-snapshots'
			mavenRepo 'http://ec2-35-170-59-132.compute-1.amazonaws.com:8080/artifactory/libs-releases'
			mavenRepo 'http://ec2-35-170-59-132.compute-1.amazonaws.com:8080/artifactory/plugins-releases'
			mavenRepo 'http://ec2-35-170-59-132.compute-1.amazonaws.com:8080/artifactory/plugins-snapshots'
			mavenRepo 'https://repo.transmartfoundation.org/content/repositories/public/'
		}
	}
	else {
		dm.configureRepositories delegate
	}

	dependencies {
		compile 'axis:axis:1.4' // for GeneGo web services
		compile 'com.google.guava:guava:18.0'
		compile 'commons-net:commons-net:3.3' // used for ftp transfers
		compile 'net.sf.ehcache:ehcache:2.9.0'
		compile 'net.sf.opencsv:opencsv:2.3'
		compile 'org.apache.commons:commons-math:2.2' //>2MB lib briefly used in ChartController
		compile 'org.apache.httpcomponents:httpclient:4.4.1'
		compile 'org.apache.httpcomponents:httpcore:4.4.1'
		compile 'org.apache.lucene:lucene-core:2.4.0'
		compile 'org.apache.lucene:lucene-demos:2.4.0'
		compile 'org.apache.lucene:lucene-highlighter:2.4.0'
		compile 'org.apache.solr:solr-core:5.4.1'
		compile 'org.apache.solr:solr-solrj:5.4.1'
		compile 'org.codehaus.groovy.modules.http-builder:http-builder:0.5.2', {
			excludes 'groovy', 'nekohtml', 'httpclient', 'httpcore'
		}
		compile 'org.rosuda:Rserve:1.7.3'
		compile('org.springframework.security.extensions:spring-security-saml2-core:1.0.0.RELEASE') {
			excludes 'bcprov-jdk15', 'spring-security-config', 'spring-security-core', 'spring-security-web', 'xercesImpl'
		}
		compile 'org.transmartproject:transmart-core-api:18.1-SNAPSHOT'

		// you can remove whichever you're not using
		runtime 'org.postgresql:postgresql:9.3-1100-jdbc4'
		runtime 'com.oracle:ojdbc7:12.1.0.1'

		test 'org.gmock:gmock:0.9.0-r435-hyve2', { transitive = false }
		test 'org.grails:grails-datastore-test-support:1.0-grails-2.3'
		test 'org.hamcrest:hamcrest-core:1.3'
		test 'org.hamcrest:hamcrest-library:1.3'
	}

	plugins {
		build ':release:3.1.2'
		build ':tomcat:7.0.54'

		compile ':cache-ehcache:1.0.5'
		compile ':codenarc:0.21'
		compile ':hibernate:3.6.10.19'
		compile ':quartz:1.0.2'
		compile ':rest-client-builder:2.1.1'
		compile ':spring-security-core:2.0.0'
		compile ':spring-security-kerberos:1.0.0'
		compile ':spring-security-ldap:2.0.0'
		compile ':spring-security-oauth2-provider:2.0-RC5'

		runtime ':jquery-ui:1.10.4'
		runtime ':jquery:1.11.1'
		runtime ':prototype:1.0'
		runtime ':resources:1.2.1'

		//test ":code-coverage:1.2.6" // Doesn't work with forked tests yet

		String tmVersion = '18.1-SNAPSHOT'
		if (!dm) {
			compile ':biomart-domain:'            + tmVersion
			compile ':blend4j-plugin:'            + tmVersion
			compile ':dalliance-plugin:'          + tmVersion
			compile ':folder-management:'         + tmVersion
			compile ':rdc-rmodules:'              + tmVersion
			compile ':search-domain:'             + tmVersion
			compile ':spring-security-auth0:0.1-SNAPSHOT'
			compile ':transmart-core:'            + tmVersion
			compile ':transmart-fractalis:0.1-SNAPSHOT'
			compile ':transmart-gwas:'            + tmVersion
			compile ':transmart-java:'            + tmVersion
			compile ':transmart-legacy-db:'       + tmVersion
			compile ':transmart-metacore-plugin:' + tmVersion
			compile ':transmart-mydas:'           + tmVersion
			compile ':transmart-rest-api:'        + tmVersion
			compile ':transmart-shared:'          + tmVersion
			compile ':transmart-xnat-importer:'   + tmVersion
			compile ':xnat-viewer:'               + tmVersion
			test ':transmart-core-db-tests:'      + tmVersion
		}
		else {
			dm.internalDependencies delegate
		}
	}
}

dm?.with {
	configureInternalPlugin 'compile', 'rdc-rmodules'
	configureInternalPlugin 'runtime', 'transmart-core'
	configureInternalPlugin 'test',    'transmart-core-db-tests'
	configureInternalPlugin 'compile', 'transmart-gwas'
	configureInternalPlugin 'compile', 'transmart-java'
	configureInternalPlugin 'compile', 'biomart-domain'
	configureInternalPlugin 'compile', 'search-domain'
	configureInternalPlugin 'compile', 'folder-management'
	configureInternalPlugin 'compile', 'transmart-legacy-db'
	configureInternalPlugin 'runtime', 'dalliance-plugin'
	configureInternalPlugin 'runtime', 'transmart-mydas'
	configureInternalPlugin 'runtime', 'transmart-rest-api'
	configureInternalPlugin 'runtime', 'blend4j-plugin'
	configureInternalPlugin 'runtime', 'transmart-metacore-plugin'
}

dm?.inlineInternalDependencies grails, grailsSettings

// Use new NIO connector in order to support sendfile
// This is a lovely thought, but with Tomcat running Grails 2.3.6+ NIO does not function in run-war mode
// Official bug number : GRAILS-11376
if (!Environment.isWarDeployed()) {
	grails.tomcat.nio = true
}

codenarc.reports = {
	TransmartAppReport('html') {
		outputFile = 'CodeNarc-transmartApp-Report.html'
		title = 'transmartApp Report'
	}
}

// vim: set et ts=4 sw=4:
