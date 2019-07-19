package controllers

import javax.inject.Inject
import models.Player
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
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
  private def deleteByName(name: String) = {

    implicit request: Request[Player] =>
      collection.flatMap(_.delete.one(request.body.name.equals(name)).map(_ => Ok))

  }

  def delete(name:String)= {
    implicit request: Request[Player] =>
      collection.flatMap(_.delete.one(request.body.name == name).map(_ => Ok))
  }

}