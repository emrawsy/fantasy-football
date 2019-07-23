package controllers

import javax.inject.Inject
import models.Player
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
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

      get(id).map(_.map { player => Ok(Json.toJson(player))
        }.getOrElse(NotFound("Player does not exist"))
      ) recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  def getAmount(id: String) = Action.async {
    implicit request: Request[AnyContent] =>

      get(id).map(
        _.map {
          player =>
            Ok(Json.toJson(player.value))
        }.getOrElse(NotFound)
      )
  }

  //create new instance of a player
  def postNewPlayer = Action.async(parse.json[Player]) {
    implicit request =>
      collection.flatMap(_.insert.one(request.body).map(_ => Ok)).recoverWith {
        case e => Future.successful(BadRequest(s"Failed with exception: $e"))
      }
  }

  //delete a player found by ID
  def deletePlayerById(id: String) = Action.async {
    implicit request =>
      collection.flatMap(_.delete.one(Json.obj("id" -> id)).map(_ => Ok("Delete by ID successful"))).recoverWith {
        case e => Future.successful(BadRequest(s"Failed with exception: $e"))
      }
  }

  //update existing player name
  def updatePlayerName(id: String, newName: String) = Action.async {
    implicit request =>
      get(id).flatMap {
        case Some(player) => if (player.id != id)
          Future.successful(Ok("Incorrect player"))
        else {
          collection.flatMap(_.update.one(
            Json.obj("id" -> id),
            Json.obj("id" -> player.id, "name" -> newName, "value" -> player.value)
          )).map {
            _ => Ok("Name updated")
          }.recoverWith {
            case e => Future.successful(BadRequest(s"Something has gone wrong, failed with exception: $e"))
          }
        }
        case None => Future.successful(NotFound("Player not found!"))
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  //decrease amount player is worth
  def decreaseAmount(id: String, amount: BigDecimal): Action[AnyContent] = Action.async {
    get(id).flatMap {
      case Some(player) => if (player.value < amount)
          Future.successful(Ok("balance not high enough"))
        else {
          collection.flatMap(_.update.one(
            Json.obj("id" -> id),
            Json.obj("id" -> player.id, "name" -> player.name, "value" -> (player.value - amount)))
          ).map {
            _ => Ok("Document updated!")
          }.recoverWith {
            case e =>
              Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
          }
        }
      case None => Future.successful(NotFound("Player not found!"))
    } recoverWith {
      case _: JsResultException =>
        Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
      case e =>
        Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
    }
  }

  //increase value of existing player
  def increaseAmount(id: String, amount: BigDecimal): Action[AnyContent] = Action.async {
    get(id).flatMap {
      case Some(player) => if (amount < 0)
        Future.successful(BadRequest(s"Amount cannot be negative"))
      else {
        collection.flatMap(_.update.one(
          Json.obj("id" -> id),
          Json.obj("id" -> player.id, "name" -> player.name, "value" -> (player.value + amount))
        )).map {
          _ => Ok("Document updated!")
        }.recoverWith {
          case e => Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
        }
      }
      case None => Future.successful(NotFound("Player not found!"))
    } recoverWith {
      case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to player model. Incorrect data!"))
      case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
    }
  }

  //decrease value of existing player
  //  def makeTransaction(id: String, transactionAmount: BigDecimal) = Action.async {
  //    implicit request =>
  //      if(transactionAmount<0) {
  //        Future.successful(Ok("Transaction can't be negative"))
  //      } else {
  //        val player = get(id).map(_.get)
  //        player.map(
  //          playerInfo =>
  //            if (playerInfo.value < transactionAmount)
  //              Future.successful(Ok("balance not high enough"))
  //            else {
  //              collection.flatMap(_.update.one(
  //                Json.obj("id" -> id),
  //                Json.obj("id" -> playerInfo.id, "name" -> playerInfo.name, "value" -> (playerInfo.value - transactionAmount))
  //              ))
  //              Future.successful(Ok("done thwe thing"))
  //            }
  //        )
  //      }
  //  }

  //increase value of existing player
  //  def increaseBalance(id: String, incrementAmount: BigDecimal) = Action.async {
  //    implicit request =>
  //      if (incrementAmount < 0) {
  //        Future.successful(Ok("transaction can't be negative"))
  //      } else {
  //        get(id).map(_.get).map(
  //          result => {
  //            collection.flatMap(_.update.one(
  //              Json.obj("id" -> id),
  //              Json.obj("id" -> result.id, "name" -> result.name, "value" -> (result.value + incrementAmount))
  //            ))
  //          }
  //        ).map(_ => Ok("Top-up complete: money added to account"))
  //      }
  //  }
}
