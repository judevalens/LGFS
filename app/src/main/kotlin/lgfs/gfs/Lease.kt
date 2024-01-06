package lgfs.gfs

import lgfs.network.ServerAddress

class Lease(
	val primary: ServerAddress, val chunkMetadata: ChunkMetadata, val ts: Long, val duration: Long, val replicas:
	List<ServerAddress>
) {
	fun isValid(): Boolean {
		return true
	}

}