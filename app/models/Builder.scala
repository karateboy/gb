package models
import play.api._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import play.api.Play.current
import com.github.nscala_time.time.Imports._
import java.io._
import java.nio.file.Files
import java.nio.file._
import java.util.Date
import org.mongodb.scala.model._
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.bson._
import MongoDB._
import scala.util._

case class Builder(_id: String, addr: String, contact: String, phone: String,
                   var state: Int = Builder.NoPhoneState, var editor: Option[String] = None) {
  def updateContact(newContact: String, newPhone: String) =
    Builder(_id, addr, newContact.trim(), newPhone.trim())
}

object Builder {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Builder]), DEFAULT_CODEC_REGISTRY)

  val ColName = "builder"
  val collection = MongoDB.database.getCollection[Builder](ColName).withCodecRegistry(codecRegistry)

  implicit val bdWrite = Json.writes[Builder]
  implicit val bdRead = Json.reads[Builder]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)
      val cif = collection.createIndex(Indexes.ascending("state", "editor")).toFuture()
      cif.onFailure(errorHandler)
    }
  }

  val NoPhoneState = 0
  val HasPhoneState = 1
  val InvalidPhoneState = 2
  val CheckOutState = 3

  def initBuilder(_id: String, addr: String, contact: String, phone: String = "") = {
    if (phone.trim().isEmpty())
      Builder(_id, addr.trim(), contact.trim, "", NoPhoneState)
    else
      Builder(_id, addr.trim(), contact.trim, phone.trim, HasPhoneState)
  }

  def get(_id: String) = collection.find(Filters.eq("_id", _id)).headOption()

  def upsert(builder: Builder) = {
    val f = collection.replaceOne(Filters.eq("_id", builder._id), builder, UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case x =>
        if (!builder.phone.isEmpty()) {
          BuildCaseState.HasPhoneState.toString()
          val f2 = BuildCase2.updateStateByBuilder(builder._id, BuildCaseState.HasPhoneState.toString())
          f2.onFailure(errorHandler)
        }
    })
    f
  }

  import scala.concurrent._
  def checkOut(editor: String) = {
    val editingF = collection.find(Filters.and(Filters.eq("state", CheckOutState), Filters.eq("editor", editor))).toFuture()
    editingF.onFailure(errorHandler)
    val ff =
      for (editing <- editingF) yield {
        if (!editing.isEmpty)
          Future {
            editing.head
          }
        else {
          import com.mongodb.client.model.ReturnDocument.AFTER
          val f = collection.findOneAndUpdate(Filters.eq("state", NoPhoneState),
            Updates.combine(Updates.set("state", CheckOutState), Updates.set("editor", editor)),
            FindOneAndUpdateOptions().returnDocument(AFTER)).toFuture()
          f.onFailure(errorHandler)
          f
        }
      }
    ff flatMap { x => x }
  }

  def checkIn(editor: String, builder: Builder) = {
    builder.editor = None
    if (isVaildPhone(builder.phone)) {
      builder.state = Builder.HasPhoneState
      UsageRecord.addBuilderUsage(editor, builder._id)
    } else
      builder.state = Builder.InvalidPhoneState

    Builder.upsert(builder)
  }
}