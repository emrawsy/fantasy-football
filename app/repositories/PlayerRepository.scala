package repositories

import javax.inject.Inject
import models.{CardId, Player}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class PlayerRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  private def findAndUpdate(collection: JSONCollection, selection: JsObject,
                            modifier: JsObject): Future[FindAndModifyCommand.Result[collection.pack.type]] = {
    collection.findAndUpdate(
      selector = selection,
      update = modifier,
      fetchNewObject = true,
      upsert = false,
      sort = None,
      fields = None,
      bypassDocumentValidation = false,
      writeConcern = WriteConcern.Default,
      maxTime = None,
      collation = None,
      arrayFilters = Seq.empty
    )
  }

  //create the collection in MongoDB
  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("players"))

  def get(id: CardId): Future[Option[Player]] =
    collection.flatMap(_.find(
      Json.obj("id" -> id.id),
      None
    ).one[Player])

  def replace(id: CardId, key: String, value: String): Future[Option[Player]] = {
    collection.flatMap {
      result =>
        val selector: JsObject = Json.obj("id" -> id.id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj(key -> value))
        findAndUpdate(result, selector, modifier).map(_.result[Player])
    }
  }

  def put(player: Player): Future[WriteResult] =
    collection.flatMap(_.insert.one(player))

  def delete(id: CardId) = {
    collection.flatMap(
      _.findAndRemove(Json.obj(
        "id" -> id.id), None, None, WriteConcern.Default, None, None, Seq.empty)
    )
  }

  def decrease(id: CardId, originalValue: BigDecimal, value: BigDecimal): Future[Option[Player]] = {
    collection.flatMap {
      result =>
        val selector: JsObject = Json.obj("id" -> id.id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("value" -> (originalValue - value)))
        findAndUpdate(result, selector, modifier).map(_.result[Player])
    }
  }

  def increase(id: CardId, originalValue: BigDecimal, value: BigDecimal): Future[Option[Player]] = {
    collection.flatMap {
      result =>
        val selector: JsObject = Json.obj("id" -> id.id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("value" -> (originalValue + value)))
        findAndUpdate(result, selector, modifier).map(_.result[Player])
    }
  }
}
