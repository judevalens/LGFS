package lgfs.gfs

class MutationHolder {
    val lease: Lease = TODO()
    val mutations = HashMap<String, MutableList<Mutation>>()

    fun addMutation( clientId: String,  mutationId: String) {

    }
}