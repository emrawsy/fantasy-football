package controllers

import java.time.LocalDateTime

import javax.inject.Inject
import models.{CardId, Player, UserSession}
import play.api.Configuration
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import repositories.{PlayerRepository, SessionRepository}

import scala.concurrent.{ExecutionContext, Future}

class PlayerManagerController @Inject()(cc: ControllerComponents,
                                        playerRepo: PlayerRepository,
                                        sessionRepo: SessionRepository,
                                        config: Configuration)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {


  //checks to see if player id is valid and if already has a session
  def present(id: CardId) = Action.async {
    implicit request =>
      playerRepo.get(id).flatMap {
        case Some(player) =>
          sessionRepo.get(id).flatMap {
            case Some(_) => sessionRepo.delete(id).map(_ => Ok(s"Goodbye ${player.name}"))
            case None => sessionRepo.put(UserSession(id.id, LocalDateTime.now)).map(_ => Ok(s"Welcome ${player.name}"))
          }
        case None => Future.successful(NotFound("Please register fren"))
      } recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data! ${e.getMessage}"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  def findPlayer(id: CardId): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      playerRepo.get(id).map {
        case Some(player) => Ok(Json.toJson(player))
        case None => NotFound("Please register")
      }.recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  def getAmountById(id: CardId) = Action.async {
    implicit request: Request[AnyContent] =>
      playerRepo.get(id).map {
        case Some(player) => Ok(Json.toJson(player.value))
        case None => NotFound("This player does not exist")
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  //create new instance of a player
  def postNewPlayer() = Action.async(parse.json) {
    implicit request =>
      playerRepo.put(request.body.as[Player]).map(
        _ => Ok("New player added!")
      ) recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  //delete a player found by ID
  def deletePlayerById(id: CardId) = Action.async {
    implicit request =>
      playerRepo.delete(id).map(
        result =>
          result.value match {
            case Some(_) => Ok("Sucessfully deleted")
            case _ => NotFound("Player not found and therefore cannot be deleted")
          }) recoverWith {
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  def updatePlayer(id: CardId, field: String, newData: String): Action[AnyContent] = Action.async {
    implicit request =>
      playerRepo.replace(id, field, newData).map {
        case Some(player) =>
          Ok(s"Success! updated player with id ${player.id}'s $field to $newData")
        case _ =>
          NotFound("No player with that id exists in records")
      }
  }

  def decreaseAmount(id: CardId, money: BigDecimal) = Action.async {
    implicit request =>
      playerRepo.get(id).flatMap {
        case Some(player) if (player.value < money) => Future.successful(BadRequest("Balance is not high enough"))
        case Some(player) if (player.value >= money)=>
          playerRepo.decrease(id, player.value, money).map {
            case Some(_) => Ok(s"Transaction complete: balance is now ${player.value-money}")
            case None => NotFound("No balance for user")
          }
        case _ => Future.successful(NotFound("User not found"))
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }

  def increaseAmount(id: CardId, money: BigDecimal) = Action.async {
    implicit request =>
      playerRepo.get(id).flatMap {
        case Some(player) if (player.value < 0) => Future.successful(BadRequest("Should not be minus"))
        case Some(player) if (player.value >= 0)=>
          playerRepo.increase(id, player.value, money).map {
            case Some(_) => Ok(s"Transaction complete: balance is now ${player.value+money}")
            case None    => NotFound("No balance for user")
          }
        case _ => Future.successful(NotFound("User not found"))
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }
}
