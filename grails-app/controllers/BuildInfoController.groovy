class BuildInfoController {

	private static final List<String> buildInfoProperties = [
			'scm.version',
			'build.date',
			'build.timezone',
			'build.java',
			'env.os',
			'env.username',
			'env.computer',
			'env.proc.type',
			'env.proc.cores'].asImmutable()

	def index() {
		List<String> customProperties = [] + buildInfoProperties
		if (grailsApplication.config.buildInfo.exclude) {
			customProperties.removeAll grailsApplication.config.buildInfo.exclude
		}
		if (grailsApplication.config.buildInfo.include) {
			customProperties.addAll grailsApplication.config.buildInfo.include
		}

		[buildInfoProperties: customProperties.sort()]
	}
}
