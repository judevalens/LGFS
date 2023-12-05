package lgfs.api.grpc

import GFSGrpcKt
import Gfs
import lgfs.api.GfsApi


class FileService(val gfsApi: GfsApi) : GFSGrpcKt.GFSCoroutineImplBase() {
    override suspend fun createFile(request: Gfs.CreateFileReq): Gfs.CreateFileRes {
        return super.createFile(request)
    }
}