package network

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.cluster.Member
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory


class Manager(context: ActorContext<Command>) : AbstractBehavior<Manager.Command>(context) {
    interface Command {}

    class LaunchMaster(val name: String) : Command {
        fun test() {

        }
    }
    class LaunchChunk(val name: String) : Command {

    }

    companion object {
        private fun create(): Behavior<Manager.Command> {
            return Behaviors.setup {
                Manager(it)
            }
        }

        fun launch() {
            conf()
            val system = ActorSystem.create(Manager.create(), "lgfsCluster")
            val cluster: Cluster = Cluster.get(system)
            val member: Member = cluster.selfMember()
            if (member.hasRole("master")) {
                system.tell(LaunchMaster(Secrets.getSecrets().getName()))
            } else if (member.hasRole("chunk")) {
                system.tell(LaunchChunk(Secrets.getSecrets().getRole()))
            }
        }

        private fun conf() {
            val overrides: MutableMap<String, Any> = HashMap()
            overrides["akka.cluster.roles"] = listOf<String>(Secrets.getSecrets().getRole())
            val config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load())
        }
    }

    override fun createReceive(): Receive<Command> {
        val builder: ReceiveBuilder<Command> = newReceiveBuilder()
            .onMessage(
                LaunchMaster::class.java
            ) {
                context.spawn(Master.create(), it.name)
                Behaviors.same()
            }
            .onMessage(LaunchChunk::class.java) {
                context.spawn(Master.create(), it.name)
                Behaviors.same()
            }

        return builder.build()

    }
}