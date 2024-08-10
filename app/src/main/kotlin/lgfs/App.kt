/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package lgfs

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import kotlinx.coroutines.runBlocking
import lgfs.network.Manager
import org.slf4j.LoggerFactory


fun main() {
    val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val nettyLogger: Logger = loggerContext.getLogger("io.grpc.netty")
    nettyLogger.level = Level.INFO
    val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
    rootLogger.level = Level.INFO
    if (System.getenv()["NODE_TYPE"].equals("server")) {
        runBlocking {
            val manager = Manager.launch()
        }
    }else {
        val client = lgfs.client.Client()
    }
}
