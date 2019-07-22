package controllers

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._

class PlayerManagerControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {


  // TODO move this stuff to service layer, mock service call in controller
  "PlayerManagerController" should {
    "return Ok on a valid get" in {
      val testController = mock[PlayerManagerController]
      val getPlayer = testController.getPlayer("id").apply(FakeRequest(GET, "/"))

      status(getPlayer) mustBe OK
      contentType(getPlayer) mustBe Some("text/html")
    }

    "return 404 when player does not exist" in {
      ???
    }
  }

}
