package controllers

import java.time.LocalDateTime

import javax.inject.Inject
import models.{CardId, Player, UserSession}
import play.api.Configuration
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class PlayerManagerController @Inject()(cc: ControllerComponents,
                                        mongo: ReactiveMongoApi,
                                        config: Configuration)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

  val timeToLiveInSeconds: Int = config.get[Int]("session.ttl")

  private val index = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some("session-index"),
    options = BSONDocument("expireAfterSeconds" -> timeToLiveInSeconds)
  )
  sessionCollection.map(_.indexesManager.ensure(index))

  private def getSession(id: CardId): Future[Option[UserSession]] =
    sessionCollection.flatMap(_.find(
      Json.obj("id" -> id.id),
      None
    ).one[UserSession])

  def deleteSessionById(id: CardId) = {
    sessionCollection.flatMap(_.delete.one(Json.obj("id" -> id.id)))
  }

  def createNewSession(session: UserSession) =
    sessionCollection.flatMap(_.insert.one(session))


  //create session collection in MongoDB
  private def sessionCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("session"))

  //checks to see if player id is valid and if already has a session
  def present(id: CardId) = Action.async {
    implicit request =>
      get(id).flatMap {
        optionalPlayer =>
          optionalPlayer.map {
            player: Player =>
              getSession(id).flatMap {
                session =>
                  if (session.isDefined) {
                    deleteSessionById(id).map(
                      _ => Ok(s"Goodbye ${player.name}")
                    )
                  } else {
                    createNewSession(UserSession(id.id, LocalDateTime.now)).map(
                      _ => Ok(s"Welcome ${player.name}")
                    )
                  }
              }
          } getOrElse {
            Future.successful(NotFound("Please register fren"))
          }
      }
  }

  //create the collection in MongoDB
  private def playerCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("players"))

  private def get(id: CardId): Future[Option[Player]] =
    playerCollection.flatMap(_.find(
      Json.obj("id" -> id.id),
      None
    ).one[Player])

  //find player in database
  def getPlayer(id: CardId) = Action.async {
    implicit request: Request[AnyContent] =>

      get(id).map(_.map { player => Ok(Json.toJson(player))
      }.getOrElse(NotFound("Player does not exist"))
      ) recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong, failed with the following exception: $e"))
      }
  }

  def getAmount(id: CardId) = Action.async {
    implicit request: Request[AnyContent] =>

      get(id).map(
        _.map {
          player =>
            Ok(Json.toJson(player.amount))
        }.getOrElse(NotFound)
      )
  }

  //create new instance of a player
  def postNewPlayer = Action.async(parse.json[Player]) {
    implicit request =>

            playerRepository.put(request.body) map {

              Ok()
            }
//      playerCollection.flatMap(_.insert.one(request.body).map(_ => Ok)).recoverWith {
//        case e => Future.successful(BadRequest(s"Failed with exception: $e"))
//      }
  }

  //delete a player found by ID
  def deletePlayerById(id: String) = Action.async {
    implicit request =>
      playerCollection.flatMap(_.delete.one(Json.obj("id" -> id)).map(_ => Ok("Delete by ID successful"))).recoverWith {
        case e => Future.successful(BadRequest(s"Failed with exception: $e"))
      }
  }

  //update existing player name
  def updatePlayerName(id: CardId, newName: String) = Action.async {
    implicit request =>
      get(id).flatMap {
        case Some(player) => if (player.id != id)
          Future.successful(Ok("Incorrect player"))
        else {
          playerCollection.flatMap(_.update.one(
            Json.obj("id" -> id.id),
            Json.obj("id" -> player.id, "name" -> newName, "value" -> player.amount)
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
  def decreaseAmount(id: CardId, amount: BigDecimal): Action[AnyContent] = Action.async {
    get(id).flatMap {
      case Some(player) => if (player.amount < amount)
        Future.successful(Ok("balance not high enough"))
      else {
        playerCollection.flatMap(_.update.one(
          Json.obj("id" -> id.id),
          Json.obj("id" -> player.id, "name" -> player.name, "value" -> (player.amount - amount)))
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
  def increaseAmount(id: CardId, amount: BigDecimal): Action[AnyContent] = Action.async {
    get(id).flatMap {
      case Some(player) => if (amount < 0)
        Future.successful(BadRequest(s"Amount cannot be negative"))
      else {
        playerCollection.flatMap(_.update.one(
          Json.obj("id" -> id.id),
          Json.obj("id" -> player.id, "name" -> player.name, "value" -> (player.amount + amount))
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
}
