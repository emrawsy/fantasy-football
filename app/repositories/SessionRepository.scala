package repositories

import javax.inject.Inject
import models.{CardId, Player, UserSession}
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class SessionRepository @Inject()(mongo: ReactiveMongoApi,
                                  playerRepository: PlayerRepository,
                                  config: Configuration)(implicit ec: ExecutionContext) {

  //create session collection in MongoDB
  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("session"))

  val timeToLiveInSeconds: Int = config.get[Int]("session.ttl")

  private val index = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some("session-index"),
    options = BSONDocument("expireAfterSeconds" -> timeToLiveInSeconds)
  )
  collection.map(_.indexesManager.ensure(index))

  def get(id: CardId): Future[Option[UserSession]] =
    collection.flatMap(_.find(
      Json.obj("id" -> id.id),
      None
    ).one[UserSession])

  def delete(id: CardId) = {
    collection.flatMap(_.delete.one(Json.obj("id" -> id.id)))
  }

  def put(session: UserSession) =
    collection.flatMap(_.insert.one(session))



}
