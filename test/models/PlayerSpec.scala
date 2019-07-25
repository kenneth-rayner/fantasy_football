package models

import org.scalatest._
import play.api.libs.json.{JsValue, Json}

class PlayerSpec extends WordSpec with OptionValues with MustMatchers {

  "Player" must {
    "Deserialize correctly" in {

      val test = Json.parse(
        """
          |{
          | "_id" : "1",
          | "name": "Test",
          | "email": "Test@Test.com",
          | "mobileNumber": "123456",
          | "balance": 123,
          | "securityNumber": 123
          |}
        """.stripMargin
      )

      test.as[Player] mustBe Player(
        _id = "1",
        name = "Test",
        email = "Test@Test.com",
        mobileNumber = "123456",
        balance = 123,
        securityNumber = 123
      )
    }
    "Serialize correctly" in {

      val test:JsValue = Json.parse(
        s"""
          |{
          | "_id" : "1",
          | "name": "Test",
          | "email": "Test@Test.com",
          | "mobileNumber": "123456",
          | "balance": 123,
          | "securityNumber": 123
          |}
        """.stripMargin
      )

      val blah = Player(
        "1",
        "Test",
        "Test@Test.com",
        "123456",
        123,
        123
        )
          Json.toJson[Player](blah)mustBe test
    }

  }

}
