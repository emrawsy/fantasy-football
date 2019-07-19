package controllers

import javax.inject.Inject
import models.Player
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class PlayerManagerController @Inject()(cc: ControllerComponents, mongo: ReactiveMongoApi)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {
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

//  //update value of existing player
//  def updatePlayerValue(id: String, newValue: BigDecimal) = Action.async {
//    implicit request =>
//      get(id).map(_.get).map(
//        result =>
//          collection.flatMap(_.update.one(
//            Json.obj("id" -> id),
//            Json.obj("id" -> result.id, "name" -> result.name, "value" -> newValue)
//          ))
//      ).map(_ => Ok("update complete"))
//  }
}
