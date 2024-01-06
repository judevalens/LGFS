package lgfs.network

class ServerAddress(val hostName: String, val akkaPort: Int, val apiPort: Int, val dataPort: Int) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ServerAddress

		if (hostName != other.hostName) return false
		if (akkaPort != other.akkaPort) return false
		if (apiPort != other.apiPort) return false
		if (dataPort != other.dataPort) return false

		return true
	}

	override fun hashCode(): Int {
		var result = hostName.hashCode()
		result = 31 * result + akkaPort
		result = 31 * result + apiPort
		result = 31 * result + dataPort
		return result
	}

	override fun toString(): String {
		return hostName
	}
}