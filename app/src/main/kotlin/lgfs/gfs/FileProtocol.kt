package lgfs.gfs

import akka.actor.typed.ActorRef
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lgfs.network.JsonSerializable

@JsonSerialize()
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = [JsonSubTypes.Type(value = FileProtocol.LeaseGrantRes::class, name = "LeaseGrantRes")])
interface FileProtocol {
    class CreateFileReq(
        val reqId: String,
        val fileMetadata: FileMetadata,
        val replyTo: ActorRef<FileProtocol>
    ) : FileProtocol

    class CreateFileRes(val reqId: String, val successful: Boolean, val chunks: List<ChunkMetadata>?) :
        FileProtocol

    class DeleteFileReq(val reqId: String, val fileName: String, val replyTo: ActorRef<FileProtocol>) : FileProtocol
    class DeleteFileRes(val reqId: String, val fileName: String, val isDeleted: Boolean) : FileProtocol

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
    class LeaseGrantReq(val reqId: String, val chunkHandles: List<Long>, val replyTo: ActorRef<FileProtocol>) :
        FileProtocol

    class LeaseGrantMapRes(val reqId: String, val leases: HashMap<String, MutableList<Lease>>) : FileProtocol
    class LeaseGrantRes @JsonCreator constructor(val reqId: String, val leases: MutableList<Lease>) : FileProtocol,
        JsonSerializable
}