package lgfs.gfs

class Mutation(val mutationId: String, val type: Type, val offset: Int, val serialNumber: Int) {
    enum class Type {
        write,
        apppend
    }
}