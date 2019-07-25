package repositories

import models.UpsertAction
import play.api.inject.guice.GuiceApplicationBuilder

class PlayerRepositorySpec extends FreeSpec with MongoSuite with GuiceOneAppPerSuite {

  private lazy val builder = new GuiceApplicationBuilder()

  "Player Respository" - {

    "Put" - {

      "must insert a player and return a result" - {

        val app = builder.build()

        running(app) {

          started(app).futureValue

          val playerRepository = app.injector.instanceOf[PlayerRepository]

          val playerToInsert = Player(id = "id", name = "name", amount = 120.00)

          playerRepository.put(playerToInsert) mustBe UpsertAction.Inserted
        }
      }
    }
  }
}