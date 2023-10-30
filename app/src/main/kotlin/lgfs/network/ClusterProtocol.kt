package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import com.fasterxml.jackson.annotation.JsonCreator
import lgfs.gfs.ChunkServerStat

interface ClusterProtocol {
    class Handshake(listing: Receptionist.Listing) : ClusterProtocol, JsonSerializable
    class MasterUP @JsonCreator constructor(val masterRef: ActorRef<ClusterProtocol>) : ClusterProtocol,
        JsonSerializable

    class ChunkUp @JsonCreator constructor(
        val chunkRef: ActorRef<ClusterProtocol>,
        val serverHostName: String,
        val stat: ChunkServerStat
    ) : ClusterProtocol, JsonSerializable

    class ChunkInventory @JsonCreator constructor(val serverHostName: String, val chunkIds: MutableList<Long>) :
        ClusterProtocol, JsonSerializable

    class RequestChunkInventory @JsonCreator constructor() : ClusterProtocol, JsonSerializable
    class NoOp() : ClusterProtocol, JsonSerializable
}