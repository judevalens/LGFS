package lgfs.api.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MasterAPI(private val fileService: MasterServiceImpl) {

}