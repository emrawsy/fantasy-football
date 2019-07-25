package controllers

import models.CardId
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

class PlayerManagerControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  // TODO move this stuff to service layer, mock service call in controller
  "PlayerManagerController" should {
    val mockMongo = mock[DefaultDB]
    val mockCollection = mockMongo[BSONCollection]
    val cardId = CardId("id")

    "return Ok on a valid get" in {

      val testController = mock[PlayerManagerController]
      val getPlayer = testController.getPlayer(cardId).apply(FakeRequest(GET, "/"))

      status(getPlayer) mustBe OK
      contentType(getPlayer) mustBe ???
    }

    "return 404 when player does not exist" in {
      ???
    }
  }

}
