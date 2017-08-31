package models
import com.google.maps._

object GoogleApi {
  val context = new GeoApiContext.Builder()
    .apiKey("AIzaSyAi9hG7X74_CL-3i_6utBMNKzrRKOqwo98")
    .build()
    
  def queryAddr(addr:String)={
    val results =  GeocodingApi.geocode(context, addr).await()
    results.map { ret =>
      Seq(ret.geometry.location.lat, ret.geometry.location.lng)
    }
  }

}