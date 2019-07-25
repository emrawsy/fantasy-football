package repositories

import models.{Player, UpsertAction}

import scala.concurrent.Future

class DefaultPlayerRepository extends PlayerRepository {

  def get(id:String): Future[Player] = ???

  def put(player: Player): Future[UpsertAction] = ???
}

trait PlayerRepository {

  def get(id: String): Future[Player]

  def put(player: Player): Future[UpsertAction]

}
