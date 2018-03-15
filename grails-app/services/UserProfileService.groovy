import grails.transaction.Transactional
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.transmart.searchapp.AuthUser
import grails.converters.JSON

@Transactional
class UserProfileService {

	def springSecurityService

	
	def getAuthUserLevel() {
		def roles = springSecurityService.getPrincipal().getAuthorities()

		if (roles.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))){
			return 99//UserLevel.ADMIN
		}
		if (roles.contains(new SimpleGrantedAuthority("ROLE_DATASET_EXPLORER_ADMIN"))){
			return 2//UserLevel.TWO
		}
		if (roles.contains(new SimpleGrantedAuthority("ROLE_STUDY_OWNER"))){
			return 1//UserLevel.ONE
		}
		else{
			return 0//UserLevel.ZERO
		}
		
	}

	// Test data
	public getUserDescription(AuthUser user) {
		
		def description = [:]
		try{

			if (user?.description) {
				description = JSON.parse(user.description)
			}
	
			description << [
				username: user.username,
				email: user.email?:user.username?:"",
				level: getAuthUserLevel(),
				type: user.getType(),
				id: user.id,
			]
		}
		catch(Exception ex){
			log.error("Exception occured", ex)
		}
		return description
	}


}
