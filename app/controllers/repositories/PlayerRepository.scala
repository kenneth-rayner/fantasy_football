package controllers.repositories

import javax.inject.Inject
import models.{CardId, Player}
import play.api.Configuration
import play.api.libs.json.{JsObject, JsResultException, Json}
import play.api.mvc.{AbstractController, ControllerComponents, _}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.{ExecutionContext, Future}


class PlayerRepository@Inject()(cc: ControllerComponents,
                                config: Configuration,
                                mongo:ReactiveMongoApi                           )
                               (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def playerCollection: Future[JSONCollection] = {
    mongo.database.map(_.collection[JSONCollection]("players"))
  }

  private def findAndUpdate(collection: JSONCollection, selection: JsObject,
                            modifier: JsObject): Future[FindAndModifyCommand.Result[collection.pack.type]] = {
    collection.findAndUpdate(
      selector = selection,
      update = modifier,
      fetchNewObject = true,
      upsert = false,
      sort = None,
      fields = None,
      bypassDocumentValidation = false,
      writeConcern = WriteConcern.Default,
      maxTime = None,
      collation = None,
      arrayFilters = Seq.empty
    )
  }

  //GET
  def getPlayerById(_id: CardId): Future[Option[Player]] = {
    playerCollection.flatMap(_.find(
      Json.obj("_id" -> _id.idNumber),
      None
    ).one[Player])
  }

  //PUT
  def addNewPlayer(newPlayer: Player): Future[WriteResult] = {
    playerCollection.flatMap(
      _.insert.one(newPlayer)
    )
  }

  //DELETE
  def deletePlayerById(_id: CardId) = {
    playerCollection.flatMap(
      _.findAndRemove(Json.obj("_id" -> _id.idNumber), None, None, WriteConcern.Default,
        None, None, Seq.empty)
    )
  }

  //UPDATE
  def updateName(_id: CardId, newData: String): Future[Option[Player]] = {
    playerCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> _id.idNumber)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("name" -> newData))
        findAndUpdate(result, selector, modifier).map(_.result[Player])
    }
  }

  def increaseBalance(_id: CardId, increase: Int): Future[Option[Player]] = {
    playerCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> _id.idNumber)
        val modifier: JsObject = Json.obj("$inc" -> Json.obj("balance" ->  increase))
        findAndUpdate(result, selector, modifier).map(_.result[Player])


    }
  }
  def decreaseBalance(_id: CardId, decrease: Int): Future[Option[Player]] = {
    playerCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> _id.idNumber)
        val modifier: JsObject = Json.obj("$inc" -> Json.obj("balance" ->  -decrease))
        findAndUpdate(result, selector, modifier).map(_.result[Player])
    }
  }

}

