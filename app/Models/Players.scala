package Models

import play.api.libs.json.Writes
import play.api.libs.json._
import play.api.libs.functional.syntax._


case class Players(id: String, name: String, value: Int)

object Players {
  implicit val writes: Writes[Players] =
     ((__ \ "id").write[String] and
      (__ \ "name").write[String] and
      (__ \ "value").write[Int]
     )(unlift(Players.unapply))
}
