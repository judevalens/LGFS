package lgfs.gfs.chunk

import akka.actor.typed.ActorRef
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lgfs.gfs.FileProtocol.Mutation
import lgfs.gfs.Lease
import lgfs.network.JsonSerializable
import lgfs.network.ServerAddress

@JsonSerialize()
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = [JsonSubTypes.Type(value = Command.LeaseGrantRes::class, name = "LeaseGrantRes")])
interface Command {
    class CommitMutationReq(
        val reqId: String,
        val clientId: String,
        val chunkHandle: Long,
        val replicas: List<ServerAddress>
    ) : Command

    class CommitMutationReqs(val reqId: String, val commitReqs: List<CommitMutationReq>) : Command
    class AddMutationsReq(val reqId: String, val mutations: List<Mutation>, val replyTo: ActorRef<Command>) :
        Command
    class LeaseGrantMapRes(val reqId: String, val leases: HashMap<ServerAddress, MutableList<Lease>>) : Command
    class LeaseGrantRes @JsonCreator constructor(val reqId: String, val leases: MutableList<Lease>) : Command,
        JsonSerializable
}