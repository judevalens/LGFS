package lgfs.client

import akka.actor.typed.javadsl.AskPattern
import lgfs.api.GfsApi
import lgfs.api.MasterAPI
import lgfs.gfs.FileMetadata
import lgfs.network.FileProtocol
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class Client(private val gfsApi: GfsApi) {
    interface Command

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("GFS client has been initialized")
    }

    fun createFile(filePathStr: String) {
        val filePath = Path.of(filePathStr)
        if (Files.exists(filePath)) {
            try {
                logger.info("Sending create request to master")
                val file = java.io.File(filePath.toUri())
                val fileMetadata = FileMetadata(filePathStr, false, file.length())
                val res = gfsApi.createFile(fileMetadata).toCompletableFuture().get() as FileProtocol.CreateFileRes
                logger.info("Create request is successful: ${res.successful}")
            } catch (_: IllegalArgumentException) {

            } catch (_: NullPointerException) {

            }
        }else {
            logger.info("File at: ${filePathStr} doesn't exist")
        }

    }
}