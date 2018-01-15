package models
import play.api._
import scala.concurrent.ExecutionContext.Implicits.global

object MongoDB {
  import org.mongodb.scala._

  val url = Play.current.configuration.getString("my.mongodb.url")
  val dbName = Play.current.configuration.getString("my.mongodb.db")

  val mongoClient: MongoClient = MongoClient(url.get)
  val database: MongoDatabase = mongoClient.getDatabase(dbName.get);
  def init() {
    val f = database.listCollectionNames().toFuture()
    val colFuture = f.map { colNames =>
      SysConfig.init(colNames)
      User.init(colNames)
      Identity.init(colNames)
      Order.init(colNames)
      WasteCode.init(colNames)
      CareHouse.init(colNames)
      Builder.init(colNames)
      WorkPoint.init(colNames)
      BuildCase2.init(colNames)
      DumpSite.init(colNames)
      //OilUser.init(colNames)
      //RecyclePlant.init(colNames)
      //Purifier.init(colNames)
      //Textile.init(colNames)
      //Burner.init(colNames)
      UsageRecord.init(colNames)
    }
    //Program need to wait before init complete
    import scala.concurrent.Await
    import scala.concurrent.duration._
    import scala.language.postfixOps

    Await.result(colFuture, 30 seconds)
  }

  def cleanup = {
    mongoClient.close()
  }

  import play.api.libs.json._
  import org.mongodb.scala.bson._
  implicit val objWrites = new Writes[ObjectId] {
    def writes(id: ObjectId) = JsString(id.toHexString())
  }

  implicit val objReads: Reads[ObjectId] = new Reads[ObjectId] {
    def reads(json: JsValue): JsResult[ObjectId] = {
      val ret = json.validate[String]
      ret.fold(err => {
        JsError(err)
      },
        hexStr => {
          JsSuccess(new ObjectId(hexStr))
        })
    }
  }
}