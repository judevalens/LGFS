package lgfs.gfs

import akka.actor.typed.ActorRef

interface FileProtocol {
    class CreateFileReq(
        val reqId: String,
        val fileMetadata: FileMetadata,
        val replyTo: ActorRef<FileProtocol>
    ) : FileProtocol

    class CreateFileRes(val reqId: String, val successful: Boolean, val chunks: List<ChunkMetadata>?) :
        FileProtocol

    class ChunkWriteReq(val reqId: String) : FileProtocol

    class Mutation(
        val clientId: String,
        val chunkHandle: Long,
        val mutationId: String,
        val primary: String,
        val replicas: String,
        val serial: Int,
        val offset: Int
    ) : FileProtocol

    class CommitMutation(val clientId: String, val chunkHandle: Long, val replicas: List<String>) : FileProtocol
    class Mutations(val mutations: List<Mutation>) : FileProtocol
    class LeaseGrantReq(val reqId: String, val chunkHandles: List<Long>, val replyTo: ActorRef<FileProtocol>) : FileProtocol
    class LeaseGrantRes(val leases: List<Lease>) : FileProtocol
}