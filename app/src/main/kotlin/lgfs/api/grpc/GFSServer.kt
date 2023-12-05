package lgfs.api.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GFSServer(private val fileService: FileService) {
    val port = 7009;
    val server: Server = ServerBuilder
        .forPort(port)
        .addService(fileService)
        .build()

    fun startServer() {
       runBlocking{
           launch {
               
           }
       }
        server.start()
    }
}