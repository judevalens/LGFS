package lgfs.network

import org.json.JSONObject

class Secrets {
    private lateinit var configJson: JSONObject

    init {
        println("reading secrets............")

        javaClass.getResourceAsStream("/secrets.json")?.let {
            configJson = JSONObject(String(it.readAllBytes()))
        }
    }

    companion object {
        private val Secrets = Secrets()
        fun getSecrets(): Secrets {
            return Secrets
        }
    }

    fun getName(): String {
        return configJson.getString("lgfs_server_id")
    }

    fun getRole(): String {
        return configJson.getString("lgfs_server_role")
    }

    fun getHostName(): String {
        return configJson.getString("lgfs_server_id")
    }

    fun getHomeDir(): String {
        return "/app/lgfs_dir/"
    }
}