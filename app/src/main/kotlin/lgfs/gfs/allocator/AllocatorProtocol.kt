package lgfs.gfs.allocator

import akka.actor.typed.ActorRef
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileMetadata
import java.util.ArrayList

interface AllocatorProtocol {
    class ChunkAllocationReq(val fileMetadata: FileMetadata, val replyTo: ActorRef<ChunkAllocationRes>) :
        AllocatorProtocol

    class ChunkAllocationRes(val isSuccessful: Boolean, val replicationLocations: ArrayList<ChunkMetadata>?) :
        AllocatorProtocol

    class UpdateServerState(val state: ChunkServerState) : AllocatorProtocol
}