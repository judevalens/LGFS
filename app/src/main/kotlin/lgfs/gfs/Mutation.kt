package lgfs.gfs

class Mutation (val mutationId : String, type: Type, offset: Int) {
    enum class Type {
        write,
        apppend
    }
}