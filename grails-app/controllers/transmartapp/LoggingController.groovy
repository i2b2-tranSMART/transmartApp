package transmartapp

import groovy.util.logging.Slf4j
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.transmart.plugin.custom.RequiresLevel
import org.transmart.plugin.custom.UserLevel

/**
 * @author <a href='mailto:burt_beckwith@hms.harvard.edu'>Burt Beckwith</a>
 */
@Slf4j('logger')
class LoggingController {

	private static final Map<String, Level> LOG_LEVELS = [
			ALL:   Level.ALL,
			TRACE: Level.TRACE,
			DEBUG: Level.DEBUG,
			INFO:  Level.INFO,
			WARN:  Level.WARN,
			ERROR: Level.ERROR,
			FATAL: Level.FATAL,
			OFF:   Level.OFF].asImmutable()

	@RequiresLevel(UserLevel.ADMIN)
	def index() {
		[allLevels: LOG_LEVELS.keySet()]
	}

	@RequiresLevel(UserLevel.ADMIN)
	def setLogLevel(String logger, String level) {
		LogManager.getLogger(logger).level = LOG_LEVELS[level]
		redirect action: 'index'
	}
}
