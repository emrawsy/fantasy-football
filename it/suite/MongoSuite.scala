package suite

import com.typesafe.config.ConfigFactory
import play.api.{Application, Configuration}
import reactivemongo.api._
import repositories.PlayerRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls

object MongoSuite {

  private lazy val config = Configuration(ConfigFactory.load(System.getProperty("config.resource")))

  private lazy val parsedUri = Future.fromTry {
    MongoConnection.parseURI(config.get[String]("mongodb.uri"))
  }

  lazy val connection =
    parsedUri.map(MongoDriver().connection)
}

trait MongoSuite {
  self: TestSuite =>

  def started(app: Application): Future[_] = {

    val playerRepository = app.injector.instanceOf[PlayerRepository]

    val services = Seq(playerRepository.started)

    Future.sequence(services)
  }

  def database: Future[DefaultDB] = {
    for {
      uri        <- MongoSuite.parsedUri
      connection <- MongoSuite.connection
      database   <- connection.database(uri.db.get)
    } yield database
  }
}
