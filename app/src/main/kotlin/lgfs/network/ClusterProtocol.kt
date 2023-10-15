package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist

interface ClusterProtocol {

    class Handshake(listing: Receptionist.Listing) : ClusterProtocol
    class MasterUP(val masterRef: ActorRef<ClusterProtocol>) : ClusterProtocol
    class ChunkUp(val chunkRef: ActorRef<ClusterProtocol>, val serverHostName: String) : ClusterProtocol

    class ChunkInventory(val serverHostName: String, val chunkIds: MutableList<Long>) : ClusterProtocol
    class RequestChunkInventory() : ClusterProtocol

    class NoOp() : ClusterProtocol
}