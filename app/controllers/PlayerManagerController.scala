package controllers

import javax.inject.Inject
import models.Player
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request, Result}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.FindAndModifyCommand
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class PlayerManagerController @Inject()(cc: ControllerComponents, mongo: ReactiveMongoApi)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

//  private def findAndUpdate(collection: JSONCollection, selection: JsObject, modifier: JsObject): Future[FindAndModifyCommand.Result[collection.pack.type]] = {
//    collection.findAndUpdate(
//      selector = selection,
//      update = modifier,
//      fetchNewObject = true,
//      upsert = false,
//      sort = None,
//      fields = None,
//      bypassDocumentValidation = false,
//      writeConcern = WriteConcern.Default,
//      maxTime = None,
//      collation = None,
//      arrayFilters = Seq.empty
//    )
//  }

  //create the collection in MongoDB
  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("players"))

  private def get(id: String): Future[Option[Player]] =
    collection.flatMap(_.find(
      Json.obj("id" -> id),
      None
    ).one[Player])

  //find player in database
  def getPlayer(id: String) = Action.async {
    implicit request: Request[AnyContent] =>

      get(id).map(
        _.map {
          player =>
            Ok(Json.toJson(player))
        }.getOrElse(NotFound)
      )
  }

  //create new instance of a player
  def postNewPlayer = Action.async(parse.json[Player]) {
    implicit request =>
      collection.flatMap(_.insert.one(request.body).map(_ => Ok))
  }

  //delete a player
  def deletePlayer = Action.async(parse.json[Player]) {
    implicit request =>
      collection.flatMap(_.delete.one(request.body).map(_ => Ok))
  }

  //delete a player found by ID
  def deletePlayerById(id: String) = Action.async {
    implicit request =>
      collection.flatMap(_.delete.one(Json.obj("id" -> id)).map(_ => Ok("Delete by ID successful")))
  }

  //update existing player name
  def updatePlayerName(id: String, newName: String) = Action.async {
    implicit request =>
      get(id).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            Json.obj("id" -> id),
            Json.obj("id" -> result.id, "name" -> newName, "value" -> result.value)
          ))
      ).map(_ => Ok("update complete"))
  }

  //decrease value of existing player
  def makeTransaction(id: String, transactionAmount: BigDecimal) = Action.async {
    implicit request =>
      if(transactionAmount<0) {
        Future.successful(Ok("Transaction can't be negative"))
      } else {

        val player = get(id).map(_.get)
        player.map(
          playerInfo =>
            if (playerInfo.value < transactionAmount)
              Future.successful(Ok("balance not high enough"))
            else {
              collection.flatMap(_.update.one(
                Json.obj("id" -> id),
                Json.obj("id" -> playerInfo.id, "name" -> playerInfo.name, "value" -> (playerInfo.value - transactionAmount))
              ))
              Future.successful(Ok("done thwe thing"))
            }
        )
      }
  }

  //increase value of existing player
  def increaseBalance(id: String, incrementAmount: BigDecimal) = Action.async {
    implicit request =>
      if(incrementAmount<0) {
        Future.successful(Ok("transaction can't be negative"))
      } else {
        get(id).map(_.get).map(
          result => {
              collection.flatMap(_.update.one(
                Json.obj("id" -> id),
                Json.obj("id" -> result.id, "name" -> result.name, "value" -> (result.value + incrementAmount))
              ))
          }
        ).map(_ => Ok("Top-up complete: money added to account"))
      }
  }
}
