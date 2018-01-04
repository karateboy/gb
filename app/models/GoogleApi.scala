package models
import com.google.maps._

object GoogleApi {
  val context = new GeoApiContext.Builder()
    .apiKey("AIzaSyAF2H8azbXiecvCre_b1S8UGyb24aqjqj0")    
    .build()
    
  def queryAddr(addr:String)={
    val results =  GeocodingApi.geocode(context, addr).await()
    results.map { ret =>
      Seq(ret.geometry.location.lat, ret.geometry.location.lng)
    }
  }

}