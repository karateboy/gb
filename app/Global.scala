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

    WasteWorker.start()(app.actorSystem)

    for (grabed <- SysConfig.get(SysConfig.GrabFactoryInfo)) {
      import org.mongodb.scala.bson._
      if (!grabed.asBoolean().getValue) {
        WasteWorker.grabFactoryInfo()(app.actorSystem)
        SysConfig.set(SysConfig.GrabFactoryInfo, BsonBoolean(true))
      }
    }

    for (grabed <- SysConfig.get(SysConfig.GrabProcessPlantInfo)) {
      import org.mongodb.scala.bson._
      if (!grabed.asBoolean().getValue) {
        WasteWorker.grabProcessPlantInfo()(app.actorSystem)
        SysConfig.set(SysConfig.GrabProcessPlantInfo, BsonBoolean(true))
      }
    }

    for (grabed <- SysConfig.get(SysConfig.ExportFactorySheet)) {
      import org.mongodb.scala.bson._
      if (!grabed.asBoolean().getValue) {
        WasteWorker.exportFactorSheet()(app.actorSystem)
        SysConfig.set(SysConfig.ExportFactorySheet, BsonBoolean(true))
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