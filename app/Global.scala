import play.api._
import models._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    Logger.info("Application has started")
    super.onStart(app)
    MongoDB.init()

    for (grabed <- SysConfig.get(SysConfig.GrabWasteInfo)) {
      import org.mongodb.scala.bson._
      Logger.debug(s"grabed = ${grabed.asBoolean().getValue}")
      if (!grabed.asBoolean().getValue) {
        WasteWorker.start()(app.actorSystem)
        SysConfig.set(SysConfig.GrabWasteInfo, BsonBoolean(true))
      }
    }

    //ConvertWorker.start()(app.actorSystem)
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    MongoDB.cleanup
    super.onStop(app)
  }
}