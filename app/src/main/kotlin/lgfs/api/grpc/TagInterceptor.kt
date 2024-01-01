package lgfs.api.grpc

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.kotlin.CoroutineContextServerInterceptor
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.coroutines.CoroutineContext

class TagInterceptor(val context: CoroutineContext) : CoroutineContextServerInterceptor() {
    class ReqIdKey(val keyStr: String) : CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<ReqIdKey>

        override val key: CoroutineContext.Key<ReqIdKey> = Key
    }
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        val REQ_ID_KEY: Metadata.Key<String> = Metadata.Key.of("req_id", Metadata.ASCII_STRING_MARSHALLER)
    }

    override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
        //logger.info("Intercepting grpc call, {}", headers)
        return context.plus(ReqIdKey(UUID.randomUUID().toString()))
    }
}


