package network

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.slf4j.LoggerFactory
import org.slf4j.Logger


class Master(context: ActorContext<Command>) : AbstractBehavior<Master.Command>(context) {
    interface Command {}
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        fun create(): Behavior<Command> {
            return Behaviors.setup {
                logger.info("created lgs master actor")
                Master(it)
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        TODO("Not yet implemented")
    }
}