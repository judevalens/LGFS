package lgfs.gfs.allocator

import akka.actor.typed.ActorRef
import lgfs.gfs.*

interface AllocatorProtocol {
    class ChunkAllocationReq(val fileMetadata: FileMetadata, val replyTo: ActorRef<ChunkAllocationRes>) :
        AllocatorProtocol

    class ChunkAllocationRes(val isSuccessful: Boolean, val replicationLocations: ArrayList<ChunkMetadata>?) :
        AllocatorProtocol

    class UpdateServerState(val state: ChunkServerState) : AllocatorProtocol
    class LeaseGrantReq(val reqId: String, val chunkHandles: List<Long>, val replyTo: ActorRef<FileProtocol>) : AllocatorProtocol
    class LeaseGrantRes(val reqId: String, val leases: ArrayList<Lease>) : AllocatorProtocol
}