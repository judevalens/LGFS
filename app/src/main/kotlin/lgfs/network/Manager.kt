package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.Member
import akka.cluster.typed.Cluster
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import lgfs.api.GfsApi
import lgfs.client.Client
import lgfs.gfs.StatManager
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


class Manager(context: ActorContext<Command>) : AbstractBehavior<Manager.Command>(context) {
    interface Command {}

    private val test_path = "/app/conf/test.txt"

    class LaunchMaster(val name: String) : Command {
        fun test() {

        }
    }

    class LaunchChunk(val name: String) : Command {
    }

    lateinit var masterActor: ActorRef<ClusterProtocol>
        private set
    lateinit var masterExecutor: ActorRef<FileProtocol>
        private set
    lateinit var statManager: ActorRef<StatManager.Command>
        private set
    private val fileSystem: lgfs.gfs.FileSystem = lgfs.gfs.FileSystem()

    class Handshake(listing: Receptionist.Listing) {

    }

    companion object {
        private lateinit var system: ActorSystem<Manager.Command>
        private lateinit var cluster: Cluster
        private fun create(): Behavior<Command> {
            return Behaviors.setup {
                Manager(it)
            }
        }

        fun launch() {
            val manager = create()
            system = ActorSystem.create(manager, "lgfsCluster", conf())
            cluster = Cluster.get(system)
            val member: Member = cluster.selfMember()
            println("roles: ${member.roles.first()}, address: ${member.address().host}:${member.address().port}")
            if (member.hasRole("master")) {
                system.tell(LaunchMaster(Secrets.getSecrets().getName()))
            } else if (member.hasRole("chunk")) {
                system.tell(LaunchChunk(Secrets.getSecrets().getRole()))
            }
        }

        private fun conf(): Config {
            val overrides: MutableMap<String, Any> = HashMap()
            //overrides["akka.cluster.roles"] = listOf(Secrets.getSecrets().getRole())
            val confPath = System.getenv()["CONF_PATH"]

            confPath?.let {
                val confStream = FileInputStream(confPath)
                val rawConf = String(confStream.readAllBytes())
                println(rawConf)
                val parsedConf = ConfigFactory.parseString(rawConf)
                val config = ConfigFactory.load(parsedConf).withFallback(ConfigFactory.load())
                println(confPath + ", exits:  ${Files.exists(Paths.get(confPath))}}")
                println(parsedConf.getString("akka.remote.artery.canonical.port"))
                println("is resolved: ${config.isResolved}")
                return config
            }
            exitProcess(2)
        }
    }

    override fun createReceive(): Receive<Command> {
        val builder: ReceiveBuilder<Command> = newReceiveBuilder()
            .onMessage(
                LaunchMaster::class.java
            ) { msg ->
                statManager = context.spawn(StatManager.create(), "stat_manager")
                masterActor = context.spawn(Master.create(), msg.name)
                masterExecutor =
                    context.spawn(MasterExecutor.create(statManager, fileSystem), "master_executor_service")
                launchClientAPI()
                Behaviors.same()
            }
            .onMessage(LaunchChunk::class.java) { msg ->
                context.spawn(ChunkServer.create(), msg.name)
                Behaviors.same()
            }
        return builder.build()
    }

    private fun launchClientAPI() {
        val masterApi: GfsApi = GfsApi(masterExecutor, system)
        val client = Client(masterApi)
        client.createFile(test_path)
    }

    fun isMasterActorUp(): Boolean {
        return this::masterActor.isInitialized
    }
}