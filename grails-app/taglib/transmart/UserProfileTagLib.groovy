package transmart

class UserProfileTagLib {

    def userProfileService

    def userProfile = { attrs ->
        if (userProfileService) {
            out << "<li><a href=\"" + g.createLink(controller:"userProfile", action:"index") + "\">User profile</a></li>"
        }
        else {
            out << ""
        }
    }

}