package lgfs.gfs

import akka.actor.typed.ActorRef
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lgfs.network.JsonSerializable
import lgfs.network.ServerAddress

@JsonSerialize()
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = [JsonSubTypes.Type(value = FileProtocol.LeaseGrantRes::class, name = "LeaseGrantRes")])
interface FileProtocol {
	class CreateFileReq(
		val reqId: String, val fileMetadata: FileMetadata, val replyTo: ActorRef<FileProtocol>
	) : FileProtocol

	class CreateFileRes(val reqId: String, val successful: Boolean, val chunks: List<ChunkMetadata>?) : FileProtocol

	class DeleteFileReq(val reqId: String, val fileName: String, val replyTo: ActorRef<FileProtocol>) : FileProtocol
	class DeleteFileRes(val reqId: String, val fileName: String, val isDeleted: Boolean) : FileProtocol

	class ChunkWriteReq(val reqId: String) : FileProtocol

	class Mutation(
		val clientId: String,
		val chunkMetadata: ChunkMetadata,
		val mutationId: String,
		val primary: ServerAddress,
		val replicas: List<ServerAddress>,
		val serial: Int,
		val offset: Int
	) : FileProtocol

	class CommitMutationReq(val reqId: String, val clientId: String, val chunkHandle: Long, val replicas: List<ServerAddress>) :
		FileProtocol

	class CommitMutationReqs(val reqId: String, val commitReqs: List<CommitMutationReq>) : FileProtocol
	class AddMutationsReq(val reqId: String, val mutations: List<Mutation>, val replyTo: ActorRef<FileProtocol>) : FileProtocol
	class LeaseGrantReq(
		val reqId: String, val chunkMetadataList: List<ChunkMetadata>, val replyTo: ActorRef<FileProtocol>
	) : FileProtocol

	class LeaseGrantMapRes(val reqId: String, val leases: HashMap<ServerAddress, MutableList<Lease>>) : FileProtocol
	class LeaseGrantRes @JsonCreator constructor(val reqId: String, val leases: MutableList<Lease>) : FileProtocol,
		JsonSerializable
}