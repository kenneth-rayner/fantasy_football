package models

import play.api.libs.json._


case class Player(_id: String, name: String,email:String, mobileNumber: String, value: Int,securityNumber:Int)

object Player {
  implicit lazy val format: OFormat[Player] = Json.format
}
