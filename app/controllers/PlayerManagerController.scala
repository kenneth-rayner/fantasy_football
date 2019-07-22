package controllers

import javax.inject.Inject
import models.Player
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}


class PlayerManagerController @Inject()(cc: ControllerComponents, mongo: ReactiveMongoApi)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("players"))

  private def get(_id: String): Future[Option[Player]] =
    collection.flatMap(_.find(
      Json.obj("_id" -> _id),
      None
    ).one[Player])


  def getPlayerById(_id: String) = Action.async {
    implicit request: Request[AnyContent] =>

      get(_id).map(
        _.map(player => Ok(Json.toJson(player))).getOrElse(NotFound)
      )
  }

  def postNewPlayer = Action.async(parse.json[Player]) {
    implicit request: Request[Player] =>

      collection.flatMap(_.insert.one(request.body).map(_ => Ok))
  }

  def deletePlayerByBody = Action.async(parse.json[Player]) {
    implicit request: Request[Player] =>
      collection.flatMap(_.delete.one(request.body).map(_ => Ok))
  }

  def deletePlayerById(id: String) = Action.async {
    implicit request =>
      collection.flatMap(_.delete.one(Json.obj("_id" -> id)).map(_ => Ok("Success")))
  }

  def deletePlayerByName(name: String) = Action.async {
    implicit request =>
      collection.flatMap(_.delete.one(Json.obj("name" -> name)).map(_ => Ok("Success")))
  }

  def updatePlayerName(_id: String, newName: String) = Action.async {
    implicit request =>

      get(_id).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("_id" -> _id),
            u = Json.obj(
              "name" -> newName,
              "id" -> result._id,
              "value" -> result.value
            )
          ))
      ).map(_ => Ok("Success"))

  }

  def updateValue(_id: String, newValue: Int) = Action.async {
    implicit request =>

      get(_id).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("_id" -> _id),
            u = Json.obj(
              "name" -> result.name,
              "_id" -> result._id,
              "value" -> newValue
            )
          ))
      ).map(_ => Ok("Success"))
  }

  def increaseValue(_id: String, increase: Int) = Action.async {
    implicit request =>

      get(_id).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(
            q = Json.obj("_id" -> _id),
            u = Json.obj(
              "name" -> result.name,
              "_id" -> result._id,
              "value" -> (result.value + increase)
            )
          ))
      ).map(_ => Ok("Success"))
  }

  def decreaseValue(_id: String, decrease: Int) = Action.async {
    implicit request =>

      get(_id).map(_.get).map(
        result =>
          collection.flatMap(_.update.one(

            q = Json.obj("_id" -> _id),
            u = Json.obj(
              "name" -> result.name,
              "id" -> result._id,
              "value" -> (result.value - decrease)
            )

          ))
      ).map(_ => Ok("Success"))
  }
}