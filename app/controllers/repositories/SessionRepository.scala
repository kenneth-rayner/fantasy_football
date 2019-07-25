package controllers.repositories

import java.time.LocalDateTime

import javax.inject.Inject
import models.{CardId, Player, UserSession}
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.{ExecutionContext, Future}

class SessionRepository @Inject()(mongo:ReactiveMongoApi,config: Configuration,
                                  playerRepository: PlayerRepository)(implicit ec: ExecutionContext) {

  private def sessionCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("session"))

  val ttl: Int = config.get[Int]("session.ttl")


  def createNewSession(session: UserSession) = {
    sessionCollection.flatMap(
      _.insert.one(session))
  }

  def getSession(_id: CardId): Future[Option[UserSession]] = {
    sessionCollection.flatMap(_.find(
      Json.obj("_id" -> _id.idNumber),
      None
    ).one[UserSession])
  }

  def deleteSessionById(_id: CardId): Future[WriteResult] = {
    sessionCollection.flatMap(
      _.delete.one(Json.obj("_id" -> _id.idNumber))
    )
  }


}





