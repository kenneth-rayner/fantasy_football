package controllers

import Models.Players
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

class PlayerManagerController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {


  def getPlayer(id: String) = Action {
    implicit request: Request[AnyContent] =>

      val player = new Players("aaa", "aaa",12)
      Ok(Json.toJson(player))
  }
}
