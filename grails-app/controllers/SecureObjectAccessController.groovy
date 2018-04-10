import command.SecureObjectAccessCommand
import grails.plugin.springsecurity.SpringSecurityService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmart.plugin.shared.security.Roles
import org.transmart.searchapp.AuthUser
import org.transmart.searchapp.Principal
import org.transmart.searchapp.SecureAccessLevel
import org.transmart.searchapp.SecureObject
import org.transmart.searchapp.SecureObjectAccess
import org.transmartproject.db.log.AccessLogService

@Slf4j('logger')
class SecureObjectAccessController {

	static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']
	static defaultAction = 'list'

	@Autowired private AccessLogService accessLogService
	@Autowired private SpringSecurityService springSecurityService

	def list(Integer max) {
		if (!max) {
			params.max = 10
		}
		[secureObjectAccessInstanceList: SecureObjectAccess.list(params)]
	}

	def show(SecureObjectAccess secureObjectAccess) {
		if (secureObjectAccess) {
			[secureObjectAccessInstance: secureObjectAccess]
		}
		else {
			flash.message = "SecureObjectAccess not found with id ${params.id}"
			redirect action: 'list'
		}
	}

	def delete(SecureObjectAccess secureObjectAccess) {
		if (secureObjectAccess) {
			secureObjectAccess.delete()
			flash.message = "SecureObjectAccess ${params.id} deleted"
		}
		else {
			flash.message = "SecureObjectAccess not found with id ${params.id}"
		}
		redirect action: 'list'
	}

	def edit(SecureObjectAccess secureObjectAccess) {
		if (secureObjectAccess) {
			[secureObjectAccessInstance: secureObjectAccess]
		}
		else {
			flash.message = "SecureObjectAccess not found with id ${params.id}"
			redirect action: 'list'
		}
	}

	def update(SecureObjectAccess secureObjectAccess) {
		if (secureObjectAccess) {
			secureObjectAccess.properties = params
			if (!secureObjectAccess.hasErrors() && secureObjectAccess.save()) {
				flash.message = "SecureObjectAccess ${params.id} updated"
				redirect action: 'show', id: secureObjectAccess.id
			}
			else {
				render view: 'edit', model: [secureObjectAccessInstance: secureObjectAccess]
			}
		}
		else {
			flash.message = "SecureObjectAccess not found with id ${params.id}"
			redirect action: 'edit', id: params.id
		}
	}

	def create() {
		[secureObjectAccessInstance: new SecureObjectAccess(params)]
	}

	def save() {
		SecureObjectAccess secureObjectAccess = new SecureObjectAccess(params)
		if (!secureObjectAccess.hasErrors() && secureObjectAccess.save()) {
			flash.message = "SecureObjectAccess ${secureObjectAccess.id} created"
			redirect action: 'show', id: secureObjectAccess.id
		}
		else {
			render view: 'create', model: [secureObjectAccessInstance: secureObjectAccess]
		}
	}

	def manageAccessBySecObj(String accesslevelid) {
		SecureObject secureObj
		if (params.secureobjectid != null) {
			secureObj = SecureObject.get(params.secureobjectid)
		}
		if (secureObj == null) {
			secureObj = SecureObject.list(sort: 'displayName', order: 'asc', max: 1)[0]
		}

		SecureAccessLevel access = SecureAccessLevel.findByAccessLevelName('VIEW')
		if (accesslevelid != null) {
			access = SecureAccessLevel.get(accesslevelid)
		}

		String searchtext = params.searchtext ?: ''
		List<SecureObjectAccess> secureObjectAccessList = getSecureObjAccessList(secureObj, access)
		List<Principal> userwithoutaccess = getPrincipalsWithoutAccess(secureObj, access, searchtext)

		logger.debug 'accesslist: {}, noaccess: {}, sec: {}',
				secureObjectAccessList, userwithoutaccess, secureObj

		render view: 'managePrincipalAccess', model: [
				secureObjectInstance  : secureObj,
				secureObjectAccessList: secureObjectAccessList,
				userwithoutaccess     : userwithoutaccess,
				accesslevelid         : access?.id]
	}

	def addPrincipalToAccessList(SecureObjectAccessCommand fl) {
		SecureObject secureObj = null
		StringBuilder msg = new StringBuilder(' Grant new access permission: ')

		if (params.secureobjectid != null) {
			secureObj = SecureObject.get(params.secureobjectid)
		}

		SecureAccessLevel access = SecureAccessLevel.findByAccessLevelName('VIEW')
		def accessid = params.accesslevelid
		if (accessid != null) {
			access = SecureAccessLevel.get(accessid)
		}

		String searchtext = params.searchtext
		if (fl.groupstoadd) {
			for (Principal r in Principal.getAll(fl.groupstoadd*.toLong()).findAll()) {
				addAccess r, secureObj, access
				msg << '<User:' << r.name << ', Permission:' << access.accessLevelName
				msg << ', Study:' << secureObj.bioDataUniqueId << '>'
			}
		}
		accessLogService.report 'ADMIN', msg.toString()
		List<SecureObjectAccess> secureObjectAccessList = getSecureObjAccessList(secureObj, access)
		List<Principal> userwithoutaccess = getPrincipalsWithoutAccess(secureObj, access, searchtext)

		render template: 'addremovePrincipal', model: [
				secureObjectAccessList: secureObjectAccessList,
				userwithoutaccess     : userwithoutaccess]
	}

	def removePrincipalFromAccessList(SecureObjectAccessCommand fl) {
		StringBuilder msg = new StringBuilder(' Revoke access permission: ')

		SecureObject secureObj
		if (params.secureobjectid != null) {
			secureObj = SecureObject.get(params.secureobjectid)
		}

		SecureAccessLevel access = SecureAccessLevel.findByAccessLevelName('VIEW')
		def accessid = params.accesslevelid
		if (accessid != null) {
			access = SecureAccessLevel.get(accessid)
		}

		String searchtext = params.searchtext

		if (fl.groupstoremove) {
			for (SecureObjectAccess r in SecureObjectAccess.getAll(fl.groupstoremove*.toLong()).findAll()) {
				r.delete(flush: true)
				msg << '<User:' << r.principal.name << ', Permission:' << r.accessLevel.accessLevelName
				msg  << ', Study:' << r.secureObject.bioDataUniqueId << '>'
			}
		}

		accessLogService.report 'ADMIN', eventMessage: msg.toString()

		List<SecureObjectAccess> secureObjectAccessList = getSecureObjAccessList(secureObj, access)
		List<Principal> userwithoutaccess = getPrincipalsWithoutAccess(secureObj, access, searchtext)

		render template: 'addremovePrincipal', model: [
				secureObjectAccessList: secureObjectAccessList,
				userwithoutaccess     : userwithoutaccess]
	}

	def manageAccess() {
		def pid = params.currentprincipalid
		SecureAccessLevel access = SecureAccessLevel.findByAccessLevelName('VIEW')

		def accessid = params.accesslevelid
		if (accessid != null) {
			access = SecureAccessLevel.get(accessid)
		}

		Principal principal
		if (pid != null) {
			principal = Principal.get(pid)
		}

		List<SecureObjectAccess> secureObjectAccessList = getSecureObjAccessListForPrincipal(principal, access)
		List<SecureObject> objectswithoutaccess = getObjsWithoutAccessForPrincipal(principal, '')

		render view: 'manageAccess', model: [principalInstance     : principal,
		                                     accessLevelList       : SecureAccessLevel.listOrderByAccessLevelValue(),
		                                     secureObjectAccessList: secureObjectAccessList,
		                                     objectswithoutaccess  : objectswithoutaccess,
		                                     accesslevelid         : access?.id]
	}

	def accessLevelChange() {}

	def listAccessForPrincipal() {
		Principal principalInstance = Principal.get(params.currentprincipalid)
		def accesslevelid = params.accesslevelid
		SecureAccessLevel access = SecureAccessLevel.findByAccessLevelName('VIEW')
		if (accesslevelid != null) {
			access = SecureAccessLevel.get(accesslevelid)
		}
		accesslevelid = access.id
		if (!principalInstance) {
			flash.message = 'Please select a user/group.'
			render template: 'addremoveAccess', model: [
					principalInstance     : principalInstance,
					secureObjectAccessList: [],
					objectswithoutaccess  : []]
			return
		}

		String searchtext = params.searchtext
		List<SecureObjectAccess> secureObjectAccessList = getSecureObjAccessListForPrincipal(principalInstance, access)
		List<SecureObject> objectswithoutaccess = getObjsWithoutAccessForPrincipal(principalInstance, searchtext)
		render template: 'addremoveAccess', model: [principalInstance     : principalInstance,
		                                            secureObjectAccessList: secureObjectAccessList,
		                                            objectswithoutaccess  : objectswithoutaccess,
		                                            accesslevelid         : accesslevelid]
	}

	def addSecObjectsToPrincipal(SecureObjectAccessCommand fl) {
		StringBuilder msg = new StringBuilder(' Grant new access permission: ')

		Principal principal = Principal.get(params.currentprincipalid)
		SecureAccessLevel access = SecureAccessLevel.get(params.accesslevelid)
		if (principal && access && fl.sobjectstoadd) {
			for (SecureObject r in SecureObject.getAll(fl.sobjectstoadd*.toLong()).findAll()) {
				addAccess principal, r, access
				msg << '<User:' << principal.name << ', Permission:' << access.accessLevelName
				msg << ', Study:' << r.bioDataUniqueId << '>'
			}

			accessLogService.report 'ADMIN', msg.toString()
		}

		String searchtext = params.searchtext
		List<SecureObjectAccess> secureObjAccessList = getSecureObjAccessListForPrincipal(principal, access)
		List<SecureObject> objectswithoutaccess = getObjsWithoutAccessForPrincipal(principal, searchtext)
		render template: 'addremoveAccess', model: [
				principalInstance     : principal,
				secureObjectAccessList: secureObjAccessList,
				objectswithoutaccess  : objectswithoutaccess]
	}

	def removeSecObjectsFromPrincipal(SecureObjectAccessCommand fl) {

		StringBuilder msg = new StringBuilder(' Revoke access permission: ')

		Principal principal = Principal.get(params.currentprincipalid)
		SecureAccessLevel access = SecureAccessLevel.get(params.accesslevelid)
		if (principal && access && fl.sobjectstoremove) {
			for (SecureObjectAccess r in SecureObjectAccess.getAll(fl.sobjectstoremove*.toLong()).findAll()) {
				r.delete(flush: true)
				msg << '<User:' << r.principal.name << ', Permission:' << r.accessLevel.accessLevelName
				msg << ', Study:' << r.secureObject.bioDataUniqueId << '>'
			}

			accessLogService.report 'ADMIN', msg.toString()
		}

		String searchtext = params.searchtext
		List<SecureObjectAccess> secureObjAccessList = getSecureObjAccessListForPrincipal(principal, access)
		List<SecureObject> objectswithoutaccess = getObjsWithoutAccessForPrincipal(principal, searchtext)

		render template: 'addremoveAccess', model: [
				principalInstance     : principal,
				secureObjectAccessList: secureObjAccessList,
				objectswithoutaccess  : objectswithoutaccess]
	}

	private List<SecureObject> getObjsWithoutAccessForPrincipal(Principal principal, String insearchtext) {
		if (principal) {
			SecureObject.executeQuery '''
					FROM SecureObject s
					WHERE s.dataType='BIO_CLINICAL_TRIAL'
					AND s.id NOT IN (
						SELECT so.secureObject.id
						FROM SecureObjectAccess so
						WHERE so.principal =:p)
					and upper(s.displayName) like :dn
					ORDER BY s.displayName''',
					[p: principal, dn: '%' + insearchtext.toUpperCase() + '%']
		}
		else {
			[]
		}

	}

	private List<SecureObjectAccess> getSecureObjAccessListForPrincipal(principal, access) {
		if (principal) {
			SecureObjectAccess.executeQuery('''
					FROM SecureObjectAccess s
					WHERE s.principal =:p
					  and s.accessLevel=:ac
					ORDER BY s.principal.name''',
					[p: principal, ac: access])
		}
		else {
			[]
		}
	}

	private void addAccess(Principal principal, SecureObject secobject, SecureAccessLevel access) {
		new SecureObjectAccess(principal: principal, secureObject: secobject, accessLevel: access).save(flush: true)
	}

	private boolean isAllowOwn(id) {
		AuthUser authUser = AuthUser.get(id)
		for (role in authUser.authorities) {
			if (Roles.SPECTATOR.authority.equalsIgnoreCase(role.authority)) {
				return false
			}
		}
		true
	}

	private List<SecureAccessLevel> getAccessLevelList(id) {
		if (!isAllowOwn(id)) {
			SecureAccessLevel.executeQuery '''
							FROM SecureAccessLevel
							WHERE accessLevelName <>'OWN'
							ORDER BY accessLevelValue'''
		}
		else {
			SecureAccessLevel.listOrderByAccessLevelValue()
		}
	}

	def listAccessLevel() {
		render template: 'accessLevelList', model: [accessLevelList: getAccessLevelList(params.id)]
	}

	private List<SecureObjectAccess> getSecureObjAccessList(SecureObject secureObj, SecureAccessLevel access) {
		if (secureObj == null) {
			return []
		}

		SecureObjectAccess.executeQuery '''
			FROM SecureObjectAccess s
			WHERE s.secureObject = :so
			  AND s.accessLevel = :al
			ORDER BY s.principal.name''',
				[so: secureObj, al: access]
	}

	private List<Principal> getPrincipalsWithoutAccess(SecureObject secureObj, SecureAccessLevel access, String insearchtext) {
		if (secureObj == null) {
			return []
		}

		Principal.findAll '''
				from Principal g
				WHERE g.id NOT IN (
					SELECT so.principal.id
					from SecureObjectAccess so
					WHERE so.secureObject =:secObj
					  AND so.accessLevel =:al)
				AND upper(g.name) like :st
				ORDER BY g.name''',
				[secObj: secureObj, al: access, st: '%' + insearchtext.toUpperCase() + '%']
	}
}
