package lgfs.gfs.chunk

import akka.actor.typed.ActorRef
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import lgfs.gfs.FileProtocol
import lgfs.network.Secrets
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

class TCPConnectionHandler(
	private val replyTo: ActorRef<FileProtocol>
) {
	interface State

	companion object {
		private val logger: Logger = LoggerFactory.getLogger(this::class.java)
	}


	@OptIn(DelicateCoroutinesApi::class)
	fun startDataServer() {
		val job = GlobalScope.launch {

			val server = ServerSocket(Secrets.getSecrets().getServerAddress().dataPort)
			while (true) {
				logger.info("Waiting for connection to chunk data port")
				val connection = server.accept()
				launch {
					listening(connection)
				}
			}
		}
	}

	private fun listening(connection: Socket) {
		logger.info("New tcp connection from: {}", connection.remoteSocketAddress.toString())
		val inputStream = connection.getInputStream()
		val payloads = ArrayList<Pair<ByteArray, ByteArray>>()
		while (true) {
			val nPayloadBuff = inputStream.readNBytes(2)
			val nPayload = ByteBuffer.allocate(2).put(nPayloadBuff).rewind().getShort()

			for (i in 0 until nPayload) {
				val payloadIdLenBuff = inputStream.readNBytes(4)
				val payloadIdLen = ByteBuffer.allocate(4).put(payloadIdLenBuff).rewind().getInt()
				val payloadId = connection.getInputStream().readNBytes(payloadIdLen)

				val payloadLenByte = connection.getInputStream().readNBytes(4)
				val payloadLen = ByteBuffer.allocate(4).put(payloadLenByte).rewind().getInt()
				val payload = connection.getInputStream().readNBytes(payloadLen)
				payloads.add(Pair(payloadId, payload))
				logger.info("got payload: {}, with len : {}", String(payloadId), payloadLen)
			}

		}
	}
}