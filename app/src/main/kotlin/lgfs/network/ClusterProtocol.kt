package lgfs.network

import akka.actor.typed.receptionist.Receptionist
import akka.cluster.ClusterEvent
import com.fasterxml.jackson.annotation.JsonCreator
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileProtocol

interface ClusterProtocol {
    class Handshake(listing: Receptionist.Listing) : ClusterProtocol,
        JsonSerializable

    class MasterUP @JsonCreator constructor(val serverAddress: ServerAddress, val instanceId: String) : ClusterProtocol,
        JsonSerializable

    class ChunkUp @JsonCreator constructor(
        val serverAddress: ServerAddress, val chunkServerState: ChunkServerState, val instanceId: String
    ) : ClusterProtocol,
        JsonSerializable

    class ListingRes(val listing: Receptionist.Listing) : ClusterProtocol

    class ChunkInventory @JsonCreator constructor(
        val serverHostName: String,
        val chunkIds: MutableList<Long>
    ) : ClusterProtocol,
        JsonSerializable

    class RequestChunkInventory @JsonCreator constructor() : ClusterProtocol,
        JsonSerializable

    class NoOp() : ClusterProtocol,
        JsonSerializable

    class ClusterMemberShipEvent(val event: ClusterEvent.MemberEvent) : ClusterProtocol

    class AdaptedLeaseGrantRes(val leaseGrantRes: FileProtocol.LeaseGrantMapRes)

    /**
     * Sent from an actor on master and forwarded to the chunk service
     */
    class ForwardToChunkService @JsonCreator constructor(
        val serverAddress: ServerAddress, val fileProtocolMsg: FileProtocol
    ) : ClusterProtocol,
        JsonSerializable
}