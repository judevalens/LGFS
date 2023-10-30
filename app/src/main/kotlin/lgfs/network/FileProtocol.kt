package lgfs.network

import akka.actor.typed.ActorRef
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileMetadata

interface FileProtocol {
    class CreateFileReq(
        val reqId: String,
        val fileMetadata: FileMetadata,
        val replyTo: ActorRef<FileProtocol>
    ) : FileProtocol

    class CreateFileRes(val reqId: String, val successful: Boolean, val chunks: List<ChunkMetadata>) :
        FileProtocol

    class ChunkWriteReq(val reqId: String) : FileProtocol
}