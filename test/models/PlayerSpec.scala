package models

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json

class PlayerSpec extends FreeSpec with MustMatchers {

  "Player model" - {

    val worthOfPlayer = 1000.00

    "must serialise into JSON" in {

      val player = Player(
        id = "id",
        name = "name",
        amount = worthOfPlayer
      )

      val expectedJson = Json.obj(
        "id" -> "id",
        "name" -> "name",
        "amount" -> worthOfPlayer
      )

      Json.toJson(player) mustEqual expectedJson
    }
    "must deserialise from Json" in {

      val json = Json.obj(
        "id" -> "id",
        "name" -> "name",
        "amount" -> worthOfPlayer
      )

      val expectedPlayer = Player(
        id = "id",
        name = "name",
        amount = worthOfPlayer
      )

      json.as[Player] mustEqual expectedPlayer
    }
  }
}
