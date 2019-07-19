package controllers

import javax.inject.Inject
import models.Player
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}


class PlayerManagerController @Inject()(cc: ControllerComponents, mongo: ReactiveMongoApi)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("players"))

  private def get(name: String): Future[Option[Player]] =
    collection.flatMap(_.find(
      Json.obj("name" -> name),
      None
    ).one[Player])


  def getPlayer(name: String) = Action.async {
    implicit request: Request[AnyContent] =>

      get(name).map(
        _.map(player => Ok(Json.toJson(player))).getOrElse(NotFound)
      )
  }

  def postNewPlayer = Action.async(parse.json[Player]) {
    implicit request: Request[Player] =>

      collection.flatMap(_.insert.one(request.body).map(_ => Ok))
  }

  def deletePlayer = Action.async(parse.json[Player]) {
    implicit request: Request[Player] =>
      collection.flatMap(_.delete.one(request.body).map(_ => Ok))
  }

  def deletePlayerByName(name: String) = Action.async {
    implicit request =>
      collection.flatMap(_.delete.one(Json.obj("name" -> name)).map(_ => Ok("Success")))
  }

  def updatePlayerByName(name: String) = Action.async {
    implicit request =>
      collection.flatMap(_.update.one(
        q = Json.obj("name" -> name),
        u = Json.obj("name" -> "Ken")
      ).map(_ => Ok("Success")))
  }

  def updateName(name: String, newName: String) = Action.async {
    implicit request =>

      get(name).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("name" -> name),
            u = Json.obj(
              "name" -> newName,
              "id" -> result.id,
              "value" -> result.value
            )
          ))
      ).map(_ => Ok("Success"))

  }

  def updateValue(name: String, newValue: Int) = Action.async {
    implicit request =>

      get(name).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("name" -> name),
            u = Json.obj(
              "name" -> result.name,
              "id" -> result.id,
              "value" -> newValue
            )
          ))
      ).map(_ => Ok("Success"))
  }

  def increaseValue(name: String, increase: Int) = Action.async {
    implicit request =>

      get(name).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("name" -> name),
            u = Json.obj(
              "name" -> result.name,
              "id" -> result.id,
              "value" -> (result.value + increase)
            )
          ))
      ).map(_ => Ok("Success"))
  }

  def decreaseValue(name: String, decrease: Int) = Action.async {
    implicit request =>

      get(name).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("name" -> name),
            u = Json.obj(
              "name" -> result.name,
              "id" -> result.id,
              "value" -> (result.value - decrease)
            )
          ))
      ).map(_ => Ok("Success"))
  }
}