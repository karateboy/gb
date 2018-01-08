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

case class ContactInfo(contact: String, phone: String)
case class Builder(_id: String, addr: String, contactList: Seq[ContactInfo], state: Int = Builder.NoPhoneState) {
  def updateContact(contact: String, phone: String) {
    var updated = false
    val newList = if (contactList.exists { elm => elm.contact == contact })
      contactList map {
        contactInfo =>
          if (contactInfo.contact == contact && contactInfo.phone != phone) {
            updated = true
            ContactInfo(contact, phone)
          } else
            contactInfo
      }
    else {
      updated = true
      contactList :+ ContactInfo(contact, phone)
    }

    if (updated) {
      val bd = Builder(_id, addr, newList, Builder.HasPhoneState)
      Builder.upsert(bd)
    }
  }

  def updatePhone(phone: String) {
    val newList = contactList map { ci =>
      if (ci.phone.isEmpty())
        ContactInfo(ci.contact, phone)
      else
        ci
    }
    Builder.upsert(Builder(_id, addr, newList, Builder.HasPhoneState))
  }

  def hasPhone() = {
    contactList.exists { ci => !ci.phone.isEmpty() }
  }
}

object Builder {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[Builder], classOf[ContactInfo]), DEFAULT_CODEC_REGISTRY)

  val ColName = "builder"
  val collection = MongoDB.database.getCollection[Builder](ColName).withCodecRegistry(codecRegistry)

  implicit val ciWrite = Json.writes[ContactInfo]
  implicit val bdWrite = Json.writes[Builder]
  implicit val ciRead = Json.reads[ContactInfo]
  implicit val bdRead = Json.reads[Builder]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      waitReadyResult(f)
    }
  }

  val NoPhoneState = 0
  val HasPhoneState = 1
  val InvalidPhoneState = 2
  val CheckOutState = 3

  def initBuilder(_id: String, addr: String, representative: String, phone: String = "") = {
    val info = ContactInfo(representative.trim(), phone)
    val builder = Builder(_id, addr.trim(), Seq(info))
    builder
  }

  def get(_id: String) = collection.find(Filters.eq("_id", _id)).headOption()

  def upsert(builder: Builder) = {
    val f = collection.replaceOne(Filters.eq("_id", builder._id), builder, UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case x =>
        val newState = if (builder.hasPhone())
          BuildCaseState.checked.toString()
        else
          BuildCaseState.raw.toString()

        val f2 = BuildCase2.updateStateByBuilder(builder._id, newState)
        f2.onFailure(errorHandler)
    })
    f
  }

  def checkOut = {
    val f = collection.findOneAndUpdate(Filters.eq("state", NoPhoneState), Updates.set("state", CheckOutState)).toFuture()
    f.onFailure(errorHandler)
    f
  }
  
  def giveUp(_id:String) = {
    val f = collection.updateOne(Filters.eq("state", NoPhoneState), Updates.set("state", InvalidPhoneState)).toFuture()
    f.onFailure(errorHandler)
    f
  }
}