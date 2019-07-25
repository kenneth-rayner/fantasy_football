package controllers

import java.time.LocalDateTime

import controllers.repositories.{PlayerRepository, SessionRepository}
import javax.inject.Inject
import models.{CardId, Player, UserSession}
import play.api.Configuration
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class PlayerManagerController @Inject()(cc: ControllerComponents, config: Configuration,
                                        playerRepository: PlayerRepository,sessionRepository: SessionRepository)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

  val ttl: Int = config.get[Int]("session.ttl")
  private val index = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some("session-index"),
    options = BSONDocument("expireAfterSeconds" -> ttl)
  )
//  sessionCollection.map(_.indexesManager.ensure(index))


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

//  private def playerCollection: Future[JSONCollection] =
//    mongo.database.map(_.collection[JSONCollection]("players"))
//
//  private def sessionCollection: Future[JSONCollection] =
//    mongo.database.map(_.collection[JSONCollection]("session"))
//
//  def createNewSession(session: UserSession) = {
//    sessionCollection.flatMap(
//      _.insert.one(session))
//  }

  def present(_id: CardId) = Action.async {
    implicit request =>
      playerRepository.getPlayerById(_id).flatMap{
      case Some(player) =>
              sessionRepository.getSession(_id).flatMap {
                case Some(_) =>
                  sessionRepository.deleteSessionById(_id).map(_ => Ok(s"Goodbye ${player.name}"))
                case None =>
                  sessionRepository.createNewSession(UserSession(_id.idNumber, LocalDateTime.now)).map(_ => Ok(s"Hello ${player.name}"))
              }
      case None => Future.successful(BadRequest("Please register"))
          }
  }

  //GET
//  private def get(_id: String): Future[Option[Player]] =
//    playerCollection.flatMap(_.find(
//      Json.obj("_id" -> _id),
//      None
//    ).one[Player])
//
//  //GET
//  private def getSession(_id: CardId): Future[Option[UserSession]] = {
//    sessionCollection.flatMap(_.find(
//      Json.obj("_id" -> _id.idNumber),
//      None
//    ).one[UserSession])
//  }
//
//  //POST
//  def deleteSessionById(_id: CardId): Future[WriteResult] = {
//    sessionCollection.flatMap(
//      _.delete.one(Json.obj("_id" -> _id.idNumber))
//    )
//  }

  //GET
  def getPlayerById(_id: CardId):Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>

      playerRepository.getPlayerById(_id).map {
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
//  def getBalanceById(_id: CardId) = Action.async {
//    implicit request: Request[AnyContent] =>
//
//      get(_id.idNumber).map {
//        case Some(player) => Ok(Json.toJson(player.balance))
//        case None => NotFound("Player not found!")
//      } recoverWith {
//        case _: JsResultException =>
//          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
//        case e =>
//          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
//      }
//  }

  //POST
//  def addNewPlayer = Action.async(parse.json) {
//    implicit request =>
//      playerCollection.flatMap(
//        _.insert.one(request.body.as[Player]).map(
//          _ => Ok("Success")
//        )
//      ) recoverWith {
//        case _: JsResultException =>
//          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
//        case _: DatabaseException =>
//          Future.successful(BadRequest(s"Could not parse Json to Player model. Duplicate key error!"))
//        case e =>
//          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
//      }
//  }


  def addNewPlayer: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      playerRepository.addNewPlayer(request.body.as[Player]).map(
        _ => Ok("Success")
      ) recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case _: DatabaseException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Duplicate key error!"))
        case e =>
          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }

  def deletePlayer(_id: CardId) = Action.async {
    implicit request =>
    playerRepository.deletePlayerById(_id).map(
      result =>
        result.value match {
          case Some(_) => Ok("Success")
          case _ => NotFound("Player not found")
        }
    ) recoverWith {
      case e =>
        Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
    }
  }



//  //POST
//  def deletePlayerById(_id: CardId) = Action.async {
//
//    implicit request =>
//      get(_id.idNumber).flatMap {
//        case None => Future.successful(NotFound("Player not found"))
//        case Some(player) =>
//          playerCollection.flatMap(
//            _.delete.one(Json.obj("_id" -> _id.idNumber)).map(
//              _ => Ok("Success")
//            )
//          )
//      }
//  }



  //POST
//  def updatePlayerName(_id: CardId, newName: String): Action[AnyContent] = Action.async {
//    playerCollection.flatMap {
//      result =>
//
//        val selector: JsObject = Json.obj("_id" -> _id.idNumber)
//        val modifier: JsObject = Json.obj("$set" -> Json.obj("name" -> newName))
//        val newUpdate: Future[Option[Player]] = findAndUpdate(result, selector, modifier).map(_.result[Player])
//
//        newUpdate.map {
//          case Some(player) =>
//            Ok(s"Success! updated ${player._id}'s name to ${player.name}")
//          case _ =>
//            NotFound("No player with that id exists in records")
//        }
//    }
//  }
//
//  //POST
//  def decreaseBalance(_id: CardId, decrease: Int): Action[AnyContent] = Action.async {
//    get(_id.idNumber).flatMap {
//      case Some(player) =>
//        if (player.balance < decrease)
//          Future.successful(Ok("balance not high enough"))
//        else {
//          playerCollection.flatMap(_.update.one(
//            Json.obj("_id" -> _id.idNumber),
//            Json.obj("_id" -> player._id, "name" -> player.name,"email" -> player.email,"mobileNumber" -> player.mobileNumber,
//          "balance" -> (player.balance - decrease),"securityNumber" -> player.securityNumber))
//          ).map {
//            _ => Ok("Document updated!")
//          }.recoverWith {
//            case e =>
//              Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
//          }
//        }
//      case None => Future.successful(NotFound("Player not found!"))
//    } recoverWith {
//      case _: JsResultException =>
//        Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
//      case e =>
//        Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
//    }
//  }
//
//  //POST
//  def increaseBalance(_id: CardId, increase: Int): Action[AnyContent] = Action.async {
//    get(_id.idNumber).flatMap {
//      case Some(player) =>
//        if (increase <= 0)
//          Future.successful(Ok("Minimum increase must be greater than zero"))
//        else {
//          playerCollection.flatMap(_.update.one(
//            Json.obj("_id" -> _id.idNumber),
//            Json.obj("_id" -> player._id, "name" -> player.name,"email" -> player.email,"mobileNumber" -> player.mobileNumber,
//            "balance" -> (player.balance + increase),"securityNumber" -> player.securityNumber))
//          ).map {
//            _ => Ok("Document updated!")
//          }.recoverWith {
//            case e =>
//              Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
//          }
//        }
//      case None => Future.successful(NotFound("Player not found!"))
//    } recoverWith {
//      case _: JsResultException =>
//        Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
//      case e =>
//        Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
//    }
//  }
}