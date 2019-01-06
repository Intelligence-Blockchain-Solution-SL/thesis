package es.ibs.util.di

import java.lang.reflect.Method

import scala.reflect.ClassTag

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.name.Names
import com.google.inject.util.Providers
import com.google.inject.{AbstractModule, Binder, Injector, Provider}
import javax.inject.Inject
import net.codingwell.scalaguice.ScalaModule

trait AkkaGuiceSupport {
  self: AbstractModule with ScalaModule =>

  private def accessBinder: Binder = {
    val method: Method = classOf[AbstractModule].getDeclaredMethod("binder")
    method.setAccessible(true)
    method.invoke(this).asInstanceOf[Binder]
  }

  /**
    * Create a provider for an actor implemented by the given class, with the given name.
    *
    * This will instantiate the actor using Play's injector, allowing it to be dependency injected itself.  The returned
    * provider will provide the ActorRef for the actor, allowing it to be injected into other components.
    *
    * Typically, you will want to use this in combination with a named qualifier, so that multiple ActorRefs can be
    * bound, and the scope should be set to singleton or eager singleton.
    * *
    * @param name The name of the actor.
    * @param props A function to provide props for the actor. The props passed in will just describe how to create the
    *              actor, this function can be used to provide additional configuration such as router and dispatcher
    *              configuration.
    * @tparam T The class that implements the actor.
    * @return A provider for the actor.
    */
  def providerOf[T <: Actor : ClassTag : Manifest](name: String, props: Props => Props = identity): Provider[ActorRef] =
    new ActorRefProvider(name, props)

  /**
    * Bind an actor.
    *
    * This will cause the actor to be instantiated by Guice, allowing it to be dependency injected itself.  It will
    * bind the returned ActorRef for the actor will be bound, qualified with the passed in name, so that it can be
    * injected into other components.
    *
    * @param name The name of the actor.
    * @param props A function to provide props for the actor. The props passed in will just describe how to create the
    *              actor, this function can be used to provide additional configuration such as router and dispatcher
    *              configuration.
    * @tparam T The class that implements the actor.
    */
  def bindActor[T <: Actor : Manifest](name: String, props: Props => Props = identity)(implicit ct: ClassTag[T]): ScopedBindingBuilder = {
    val _name = if (name.isEmpty) ct.runtimeClass.getSimpleName else name

    bind[T]

    accessBinder.bind(classOf[ActorRef])
      .annotatedWith(Names.named(_name))
      .toProvider(Providers.guicify(providerOf[T](_name, props)))
  }

  def bindActor[T <: Actor : Manifest]: ScopedBindingBuilder = bindActor("")

  /** Bind an actor factory.
    *
    * This is useful for when you want to have child actors injected, and want to pass parameters into them, as well as
    * have Guice provide some of the parameters.  It is intended to be used with Guice's AssistedInject feature.
    *
    * Let's say you have an actor that looks like this:
    *
    * {{{
    * class MyChildActor @Inject() (db: Database, @Assisted id: String) extends Actor {
    *  ...
    * }
    * }}}
    *
    * So `db` should be injected, while `id` should be passed.  Now, define a trait that takes the id, and returns
    * the actor:
    *
    * {{{
    * trait MyChildActorFactory {
    *  def apply(id: String): Actor
    * }
    * }}}
    *
    * Now you can use this method to bind the child actor in your module:
    *
    * {{{
    *  class MyModule extends AbstractModule with AkkaGuiceSupport {
    *    def configure = {
    *      bindActorFactory[MyChildActor, MyChildActorFactory]
    *    }
    *  }
    * }}}
    *
    * Now, when you want an actor to instantiate this as a child actor, inject `MyChildActorFactory`:
    *
    * {{{
    * class MyActor @Inject() (myChildActorFactory: MyChildActorFactory) extends Actor with ActorInject {
    *
    *  def receive {
    *    case CreateChildActor(id) =>
    *      val child: ActorRef = injectActor(myChildActoryFactory(id))
    *      sender() ! child
    *  }
    * }
    * }}}
    *
    * @tparam ActorClass The class that implements the actor that the factory creates
    * @tparam FactoryClass The class of the actor factory */
  def bindActorFactory[ActorClass <: Actor, FactoryClass: ClassTag](implicit ct: ClassTag[ActorClass]): Unit = {
    accessBinder.install(new FactoryModuleBuilder()
      .implement(classOf[Actor], ct.runtimeClass.asInstanceOf[Class[_ <: Actor]])
      .build(implicitly[ClassTag[FactoryClass]].runtimeClass))
  }

  /**
    * Provider for creating actor refs
    */
  final private class ActorRefProvider[T <: Actor : Manifest](name: String, props: Props => Props) extends Provider[ActorRef] {
    import net.codingwell.scalaguice.InjectorExtensions._

    @Inject private var actorSystem: ActorSystem = _
    @Inject private var injector: Injector = _
    lazy val get: ActorRef = {
      val creation = Props(injector.instance[T])
      actorSystem.actorOf(props(creation), name)
    }
    protected def _fake_(): Unit = {
      // fake to avoid "private var _ in class _ is never updated: consider using immutable val"
      actorSystem = null
      injector = null
    }
  }
}
