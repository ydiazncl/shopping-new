package com.inviqa.shoppingcart.impl

import akka.Done
import com.inviqa.shoppingcart.api
import com.inviqa.shoppingcart.api.{AddToCartRequest, ShoppingcartService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

/**
  * Implementation of the ShoppingcartService.
  */
class ShoppingcartServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends ShoppingcartService {

    override def hello(id: String) = ServiceCall { _ =>
        // Look up the ShoppingCart entity for the given ID.
        val ref = persistentEntityRegistry.refFor[ShoppingcartEntity](id)

        // Ask the entity the Hello command.
        ref.ask(Hello(id))
    }

    override def addToCart(id: String): ServiceCall[AddToCartRequest, Done] = ServiceCall { request =>
        val ref = persistentEntityRegistry.refFor[ShoppingcartEntity](id)

        ref.ask(AddToCartCommand(request.product))
    }


    override def useGreeting(id: String) = ServiceCall { request =>
        // Look up the ShoppingCart entity for the given ID.
        val ref = persistentEntityRegistry.refFor[ShoppingcartEntity](id)

        // Tell the entity to use the greeting message specified.
        ref.ask(UseGreetingMessage(request.message))
    }


    override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
        TopicProducer.singleStreamWithOffset {
            fromOffset =>
                persistentEntityRegistry.eventStream(ShoppingcartEvent.Tag, fromOffset)
                    .map(ev => (convertEvent(ev), ev.offset))
        }

    private def convertEvent(helloEvent: EventStreamElement[ShoppingcartEvent]): api.GreetingMessageChanged = {
        helloEvent.event match {
            case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
        }
    }
}
