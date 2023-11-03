package lgfs.api.grpc

class GreetServer : GreeterGrpcKt.GreeterCoroutineImplBase() {
    override suspend fun sayHello(request: Hw.HelloRequest): Hw.HelloReply {
        return super.sayHello(request)
    }
}