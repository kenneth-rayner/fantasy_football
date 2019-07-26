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
import scala.util.Try

class PlayerManagerController @Inject()(cc: ControllerComponents,
                                        config: Configuration,
                                        playerRepository: PlayerRepository,
                                        sessionRepository: SessionRepository)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

  val ttl: Int = config.get[Int]("session.ttl")

  private val index = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some("session-index"),
    options = BSONDocument("expireAfterSeconds" -> ttl)
  )
  //  sessionCollection.map(_.indexesManager.ensure(index))


  def present(_id: CardId) = Action.async {
    implicit request =>
      playerRepository.getPlayerById(_id).flatMap {
        case Some(player) =>
          sessionRepository.getSession(_id).flatMap {
            case Some(_) =>
              sessionRepository.deleteSessionById(_id).map(_ => Ok(s"Goodbye ${player.name}"))
            case None =>
              sessionRepository.createNewSession(UserSession(_id.idNumber, LocalDateTime.now)).map(_ => Ok(s"Hello ${player.name}"))
          }
        case None => Future.successful(BadRequest("Please register"))
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e =>
          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }

  //GET
  def getPlayerById(_id: CardId): Action[AnyContent] = Action.async {
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
  def getBalance(_id: CardId) = Action.async {
    implicit request: Request[AnyContent] =>
      playerRepository.getPlayerById(_id).map {
        case Some(player) => Ok(Json.toJson(player.balance))
        case None => NotFound("Player not found!")
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e =>
          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }

  def addNewPlayer: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      (for {
        player <- Future.fromTry(Try{request.body.as[Player]})
        result <- playerRepository.addNewPlayer(player)
      } yield Ok("Sucess")).recoverWith {
        case e:JsResultException =>
          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e: DatabaseException =>
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

  //POST
  def updatePlayerName(_id: CardId, newData: String): Action[AnyContent] = Action.async {
    implicit request =>
      playerRepository.updateName(_id, newData).map {

        case Some(player) =>
          Ok(s"Success! updated player with id ${player._id}'s name to $newData")
        case _ =>
          NotFound("No player with that id exists in records")
      }
  }

  def increaseBalance(_id: CardId, increase: Int): Action[AnyContent] = Action.async {
    playerRepository.getPlayerById(_id).flatMap {
      case Some(player) =>
        increase match {

          case x if x <= 0 => Future.successful(Ok("Minimum increase must be greater than zero"))
          case _ =>
            playerRepository.getPlayerById(_id).flatMap {
              case Some(player) =>
                playerRepository.increaseBalance(_id, increase).map { _ => Ok("Document updated!")
                }.recoverWith {

                  case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
                  case _ => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
                }
            }
        }
    }
  }
  def decreaseBalance(_id: CardId, decrease: Int): Action[AnyContent] = Action.async {
    playerRepository.getPlayerById(_id).flatMap {
      case Some(player) =>
        decrease match {
          case x if x <= 0 => Future.successful(Ok("Minimum increase must be greater than zero"))
          case x if x > player.balance => Future.successful(Ok("Decrease cannot be greater than current balance"))
          case _ =>
            playerRepository.getPlayerById(_id).flatMap {
              case Some(player) =>
                playerRepository.decreaseBalance(_id, decrease).map {
                  case Some (_) => Ok("Document updated!")
                  case None => NotFound("Player not found")

                }.recoverWith {

                  case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
                  case _ => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
                }
            }
        }
    }
  }
}


  //POST
//    def decreaseBalance(_id: CardId, decrease: Int): Action[AnyContent] = Action.async {
//      get(_id.idNumber).flatMap {
//        case Some(player) =>
//          if (player.balance < decrease)
//            Future.successful(Ok("balance not high enough"))
//          else {
//            playerCollection.flatMap(_.update.one(
//              Json.obj("_id" -> _id.idNumber),
//              Json.obj("_id" -> player._id, "name" -> player.name,"email" -> player.email,"mobileNumber" -> player.mobileNumber,
//            "balance" -> (player.balance - decrease),"securityNumber" -> player.securityNumber))
//            ).map {
//              _ => Ok("Document updated!")
//            }.recoverWith {
//              case e =>
//                Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
//            }
//          }
//        case None => Future.successful(NotFound("Player not found!"))
//      } recoverWith {
//        case _: JsResultException =>
//          Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
//        case e =>
//          Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
//      }
//    }
  //
  //POST

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
//                case e =>
//                  Future.successful(BadRequest(s"Something has gone wrong on update! Failed with exception: $e"))
//              }
//            }
//          case None => Future.successful(NotFound("Player not found!"))
//        } recoverWith {

//              case e =>
//                Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
//            }
 //         }


