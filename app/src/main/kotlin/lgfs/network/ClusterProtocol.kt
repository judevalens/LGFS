package lgfs.network

import akka.actor.typed.receptionist.Receptionist
import akka.cluster.ClusterEvent
import com.fasterxml.jackson.annotation.JsonCreator
import lgfs.gfs.ChunkServerState

interface ClusterProtocol {
    class Handshake(listing: Receptionist.Listing) : ClusterProtocol, JsonSerializable
    class MasterUP @JsonCreator constructor(val serverHostName: String, val instanceId: String) : ClusterProtocol,
        JsonSerializable

    class ChunkUp @JsonCreator constructor(
        val serverHostName: String,
        val chunkServerState: ChunkServerState,
        val instanceId: String
    ) : ClusterProtocol, JsonSerializable

    class ListingRes(val listing: Receptionist.Listing) : ClusterProtocol

    class ChunkInventory @JsonCreator constructor(val serverHostName: String, val chunkIds: MutableList<Long>) :
        ClusterProtocol, JsonSerializable

    class RequestChunkInventory @JsonCreator constructor() : ClusterProtocol, JsonSerializable
    class NoOp() : ClusterProtocol, JsonSerializable

    class ClusterMemberShipEvent(val event: ClusterEvent.MemberEvent) : ClusterProtocol
}