package models

import play.api.libs.json._


case class Player(id: String, name: String, value: Int)

object Player {
  implicit lazy val format: OFormat[Player] = Json.format
}
