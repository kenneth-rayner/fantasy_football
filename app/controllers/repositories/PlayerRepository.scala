package controllers.repositories

import javax.inject.Inject
import models.{CardId, Player}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.{ExecutionContext, Future}


class PlayerRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  private def playerCollection: Future[JSONCollection] = {
    mongo.database.map(_.collection[JSONCollection]("players"))
  }

  def getPlayerById(_id: CardId): Future[Option[Player]] = {
    playerCollection.flatMap(_.find(
      Json.obj("_id" -> _id.idNumber),
      None
    ).one[Player])
  }

  def addNewPlayer(newPlayer: Player): Future[WriteResult] = {
    playerCollection.flatMap(
      _.insert.one(newPlayer)
    )
  }

  def deletePlayerById(_id: CardId) = {
    playerCollection.flatMap(
        _.findAndRemove(Json.obj("_id" -> _id.idNumber), None, None, WriteConcern.Default, None, None, Seq.empty)
    )
  }


}