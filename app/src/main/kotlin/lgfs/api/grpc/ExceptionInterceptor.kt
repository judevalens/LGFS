package lgfs.api.grpc

import io.grpc.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext


/**
 * Log all exceptions thrown from gRPC endpoints, and adjust Status for known exceptions.
 */
class ExceptionInterceptor(val context: CoroutineContext) : ServerInterceptor {
	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

	/**
	 * When closing a gRPC call, extract any error status information to top-level fields. Also
	 * log the cause of errors.
	 */
	private class ExceptionTranslatingServerCall<ReqT, RespT>(
		delegate: ServerCall<ReqT, RespT>
	) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
		private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

		override fun close(status: Status, trailers: Metadata) {
			if (status.isOk) {
				return super.close(status, trailers)
			}
			val cause = status.cause
			var newStatus = status
			logger.error("grpc error: {}", cause?.message)
			if (status.code == Status.Code.UNKNOWN) {
				val translatedStatus = when (cause) {
					is IllegalArgumentException -> Status.INVALID_ARGUMENT
					is IllegalStateException -> Status.FAILED_PRECONDITION
					else -> Status.UNKNOWN
				}
				newStatus = translatedStatus.withDescription(cause?.message).withCause(cause)
			}

			super.close(newStatus, trailers)
		}
	}

	override fun <ReqT : Any, RespT : Any> interceptCall(
		call: ServerCall<ReqT, RespT>,
		headers: Metadata,
		next: ServerCallHandler<ReqT, RespT>
	): ServerCall.Listener<ReqT> {
		return next.startCall(ExceptionTranslatingServerCall(call), headers)
	}

}


