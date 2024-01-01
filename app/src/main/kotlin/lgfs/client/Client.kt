package lgfs.client

import Gfs
import Gfs.CreateFileReq
import MasterServiceGrpcKt
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import lgfs.api.MasterApi
import lgfs.gfs.FileMetadata
import org.apache.commons.cli.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class Client() {
    interface Command

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var gfsApi: MasterApi
    private val masterAddress = "172.20.128.2"
    private val testFilePath = "/home/jude/Documents/LGFS/.gitignore"
    private var masterApiStub: MasterServiceGrpcKt.MasterServiceCoroutineStub

    init {
        logger.info("GFS client has been initialized")

        val channel = ManagedChannelBuilder
            .forAddress(masterAddress, 7009)
            .usePlaintext()
            .build()
        masterApiStub = MasterServiceGrpcKt.MasterServiceCoroutineStub(channel)
        runBlocking {
            handleCommand()
        }
    }

    private suspend fun createFile(filePathStr: String) {
        val filePath = Path.of(testFilePath)
        if (Files.exists(filePath)) {
            try {
                logger.info("Sending create request to master")
                val file = java.io.File(filePath.toUri())
                val fileMetadata = FileMetadata(filePath.toString(), false, file.length())
                val res = masterApiStub.createFile(
                    CreateFileReq.newBuilder()
                        .setFileName(filePath.toString())
                        .setFileSize(file.length().toInt())
                        .build()
                )
                logger.info("created file at {} : {}", filePath, res.isSuccessful)
                uploadChunks(res.chunksList)
            } catch (_: IllegalArgumentException) {

            } catch (_: NullPointerException) {

            } catch (exception: StatusException) {
                logger.info("Failed to create file : $filePath")
                logger.error(exception.message)
            }
        } else {
            logger.info("File at: $filePathStr doesn't exist")
        }

    }

    private suspend fun deleteFile(filePathStr: String) {
        try {
            val res = masterApiStub.deleteFile(Gfs.DeleteFileReq.newBuilder().setFileName(testFilePath).build())
            if (res.code == Status.OK.code.value()) {
                logger.info("Successfully deleted file: {}", testFilePath)
            } else {
                logger.info("Failed to delete file: {}", testFilePath)
            }
        }catch (exception: StatusException) {
            logger.error("Failed to delete file: {} \n {}", testFilePath,exception.message)
        }

    }

    private suspend fun uploadChunks(chunks: List<Gfs.Chunk>) {
        var i = 0
        val l = chunks.size
        var k = 0;
        val chunkHandles = ArrayList<Long>()

        while (k < l) {
            chunkHandles.add(chunks[i].chunkHandle)
            i++
            if (i == 5 || k + 1 == l) {
                i = 0
                masterApiStub.getLease(Gfs.LeaseGrantReq.newBuilder().addAllChunkHandles(chunkHandles).build())
                chunkHandles.clear()
            }
            k++
        }
    }

    private suspend fun handleCommand() {
        val parser: CommandLineParser = DefaultParser()

        val options = Options();

        val createFile =
            Option.builder("c").longOpt("create").argName("path").numberOfArgs(Option.UNLIMITED_VALUES).hasArgs()
                .desc("create a new with the specified path").build()

        val deleteFile =
            Option.builder("d").longOpt("delete").argName("path").numberOfArgs(Option.UNLIMITED_VALUES).hasArgs()
                .desc("Delete the file at the provided path").build()



        options.addOption(createFile)
        options.addOption(deleteFile)

        while (true) {
            println("enter command :")
            val commands = readln().split(" ").toTypedArray()
            try {
                val cmdLine = parser.parse(options, commands)
                if (cmdLine.hasOption("create")) {
                    val filePath = cmdLine.getOptionValue("create")
                    createFile(filePath)
                } else if (cmdLine.hasOption(deleteFile.longOpt)) {
                    val filePath = cmdLine.getOptionValue(deleteFile.longOpt)
                    deleteFile(filePath)
                } else {
                    throw ParseException("Unrecognized option: ${commands[0]}")
                }
            } catch (exception: ParseException) {
                val formatter = HelpFormatter()
                println(exception.toString())
                formatter.printHelp("LGFS", options)
            }

        }
    }
}