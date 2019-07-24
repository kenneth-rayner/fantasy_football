package controllers

import java.time.LocalDateTime

import javax.inject.Inject
import models.{Player, UserSession}
import play.api.libs.json.{JsObject, JsResultException, JsString, Json}
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection._
import views.html.helper.options

import scala.concurrent.{ExecutionContext, Future}

class PlayerManagerController @Inject()(cc: ControllerComponents, mongo: ReactiveMongoApi)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val index = Index(
    key     = Seq("lastUpdated" -> IndexType.Ascending),
    name    = Some("locks-index"),
    options = BSONDocument("expireAfterSeconds" -> 900)
  )
  sessionCollection.map(_.indexesManager.ensure(index))


  private def findAndUpdate(collection: JSONCollection, selection: JsObject, modifier: JsObject): Future[FindAndModifyCommand.Result[collection.pack.type]] = {
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

  private def playerCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("players"))

  private def sessionCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("session"))

  def createNewSession(session:UserSession)= {
      sessionCollection.flatMap(
        _.insert.one(session))
  }
  
  def present(_id:String)= Action.async  {
    implicit request =>

      get(_id).flatMap(
       player =>
      if(player.isDefined){
        getSesssion(_id).flatMap {

          case Some(session) =>
            deleteSessionById(_id).map(_ => Ok("Goodbye..."))
          case None =>
            createNewSession(UserSession(_id,LocalDateTime.now)).map(_ => Ok("Hello ..."))
        }
      } else {
        Future.successful(BadRequest("playerNotFound"))

      }
      )
  }

//GET
  private def get(_id: String): Future[Option[Player]] =
    playerCollection.flatMap(_.find(
      Json.obj("_id" -> _id),
      None
    ).one[Player])

  private def getSesssion(_id: String): Future[Option[UserSession]] =
    sessionCollection.flatMap(_.find(
      Json.obj("_id" -> _id),
      None
    ).one[UserSession])

  //GET
  def getPlayerById(_id: String): Action[AnyContent] = Action.async{
    implicit request: Request[AnyContent] =>

        get(_id).map {
          case Some(player) => Ok(Json.toJson(player))
          case None => NotFound("Player not found")
        } recoverWith {
          case _: JsResultException =>
            Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
          case e =>
            Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
        }
      }

  //GET
  def getBalanceById(_id: String) = Action.async {
    implicit request: Request[AnyContent] =>

      get(_id).map {
        case Some(player) => Ok(Json.toJson(player.value))
        case None => NotFound("Player not found!")
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e =>
          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }
  //POST
  def addNewPlayer = Action.async(parse.json) {
    implicit request =>
      playerCollection.flatMap(
        _.insert.one(request.body.as[Player]).map(
          _ => Ok("Success")
        )
      ) recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case _: DatabaseException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Duplicate key error!"))
        case e =>
          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }
  def deleteSessionById(_id:String): Future[WriteResult] ={
      sessionCollection.flatMap(
        _.delete.one(Json.obj("_id" -> _id))
      )
  }
  //POST
  def deletePlayerById(_id: String) = Action.async {

    implicit request =>
      get(_id).flatMap {
        case None => Future.successful(NotFound("Player not found"))
        case Some(player) =>
          playerCollection.flatMap(
            _.delete.one(Json.obj("_id" -> _id)).map(
              _ => Ok("Success")
            )
          )
      }
  }

  //POST
  def updatePlayerName(_id: String, newName: String): Action[AnyContent] = Action.async {
    playerCollection.flatMap {
      result =>

        val selector: JsObject = Json.obj("_id" -> _id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("name" -> newName))
        val newUpdate: Future[Option[Player]] = findAndUpdate(result, selector, modifier).map(_.result[Player])

        newUpdate.map {
          case Some(player) =>
            Ok(s"Success! updated ${player._id}'s name to ${player.name}")
          case _ =>
            NotFound("No player with that id exists in records")
        }
    }
  }

  //POST
  def decreaseValue(_id: String, amount: Int): Action[AnyContent] = Action.async {
    get(_id).flatMap {
      case Some(player) =>
        if (player.value < amount)
          Future.successful(Ok("balance not high enough"))
        else {
          playerCollection.flatMap(_.update.one(
            Json.obj("_id" -> _id),
            Json.obj("_id" -> player._id, "name" -> player.name, "value" -> (player.value - amount)))
          ).map {
            _ => Ok("Document updated!")
          }.recoverWith {
            case e =>
              Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
          }
        }
      case None => Future.successful(NotFound("Player not found!"))
    } recoverWith {
      case _: JsResultException =>
        Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
      case e =>
        Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
    }
  }

  //POST
  def increaseValue(_id: String, amount: Int): Action[AnyContent] = Action.async {
    get(_id).flatMap {
      case Some(player) =>
        if (amount <= 0)
          Future.successful(Ok("Minimum increase must be greater than zero"))
        else {
          playerCollection.flatMap(_.update.one(
            Json.obj("_id" -> _id),
            Json.obj("_id" -> player._id, "name" -> player.name, "value" -> (player.value + amount)))
          ).map {
            _ => Ok("Document updated!")
          }.recoverWith {
            case e =>
              Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
          }
        }
      case None => Future.successful(NotFound("Player not found!"))
    } recoverWith {
      case _: JsResultException =>
        Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
      case e =>
        Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
    }
  }
}