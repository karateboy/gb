package models

object CoordinateTransform {
  val a = 6378137.0;
  val b = 6356752.314245;
  val lon0 = 121 * Math.PI / 180;
  val k0 = 0.9999;
  val dx = 250000;

  //給WGS84經緯度度分秒轉成TWD97坐標
  def lonlat_To_twd97(lonD: Int, lonM: Int, lonS: Int, latD: Int, latM: Int, latS: Int): (Double, Double) =
    {
      val RadianLon = lonD + lonM.toDouble / 60 + lonS.toDouble / 3600
      val RadianLat = latD + latM.toDouble / 60 + latS.toDouble / 3600
      lonlat_To_twd97(RadianLon, RadianLat)
    }

  //給WGS84經緯度弧度轉成TWD97坐標
  def lonlat_To_twd97(lonP: Double, latP: Double): (Double, Double) =
    {
      var lon = (lonP / 180) * Math.PI;
      var lat = (latP / 180) * Math.PI;

      //---------------------------------------------------------
      val e = Math.pow((1 - Math.pow(b, 2) / Math.pow(a, 2)), 0.5);
      val e2 = Math.pow(e, 2) / (1 - Math.pow(e, 2));
      val n = (a - b) / (a + b);
      val nu = a / Math.pow((1 - (Math.pow(e, 2)) * (Math.pow(Math.sin(lat), 2))), 0.5);
      val p = lon - lon0;
      val A = a * (1 - n + (5 / 4) * (Math.pow(n, 2) - Math.pow(n, 3)) + (81 / 64) * (Math.pow(n, 4) - Math.pow(n, 5)));
      val B = (3 * a * n / 2.0) * (1 - n + (7 / 8.0) * (Math.pow(n, 2) - Math.pow(n, 3)) + (55 / 64.0) * (Math.pow(n, 4) - Math.pow(n, 5)));
      val C = (15 * a * (Math.pow(n, 2)) / 16.0) * (1 - n + (3 / 4.0) * (Math.pow(n, 2) - Math.pow(n, 3)));
      val D = (35 * a * (Math.pow(n, 3)) / 48.0) * (1 - n + (11 / 16.0) * (Math.pow(n, 2) - Math.pow(n, 3)));
      val E = (315 * a * (Math.pow(n, 4)) / 51.0) * (1 - n);

      val S = A * lat - B * Math.sin(2 * lat) + C * Math.sin(4 * lat) - D * Math.sin(6 * lat) + E * Math.sin(8 * lat);

      //計算Y值
      val K1 = S * k0;
      val K2 = k0 * nu * Math.sin(2 * lat) / 4.0;
      val K3 = (k0 * nu * Math.sin(lat) * (Math.pow(Math.cos(lat), 3)) / 24.0) * (5 - Math.pow(Math.tan(lat), 2) + 9 * e2 * Math.pow((Math.cos(lat)), 2) + 4 * (Math.pow(e2, 2)) * (Math.pow(Math.cos(lat), 4)));
      val y = K1 + K2 * (Math.pow(p, 2)) + K3 * (Math.pow(p, 4));

      //計算X值
      val K4 = k0 * nu * Math.cos(lat);
      val K5 = (k0 * nu * (Math.pow(Math.cos(lat), 3)) / 6.0) * (1 - Math.pow(Math.tan(lat), 2) + e2 * (Math.pow(Math.cos(lat), 2)));
      val x = K4 * p + K5 * (Math.pow(p, 3)) + dx;

      (x, y)
    }

  def tWD97_To_lonlat(pX: Double, pY: Double) =
    {

      var x = pX
      var y = pY
      val dy = 0;
      val e = Math.pow((1 - Math.pow(b, 2) / Math.pow(a, 2)), 0.5);

      x -= dx;
      y -= dy;

      // Calculate the Meridional Arc
      val M = y / k0;

      // Calculate Footprint Latitude
      val mu = M / (a * (1.0 - Math.pow(e, 2) / 4.0 - 3 * Math.pow(e, 4) / 64.0 - 5 * Math.pow(e, 6) / 256.0));
      val e1 = (1.0 - Math.pow((1.0 - Math.pow(e, 2)), 0.5)) / (1.0 + Math.pow((1.0 - Math.pow(e, 2)), 0.5));

      val J1 = (3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32.0);
      val J2 = (21 * Math.pow(e1, 2) / 16 - 55 * Math.pow(e1, 4) / 32.0);
      val J3 = (151 * Math.pow(e1, 3) / 96.0);
      val J4 = (1097 * Math.pow(e1, 4) / 512.0);

      val fp = mu + J1 * Math.sin(2 * mu) + J2 * Math.sin(4 * mu) + J3 * Math.sin(6 * mu) + J4 * Math.sin(8 * mu);

      // Calculate Latitude and Longitude

      val e2 = Math.pow((e * a / b), 2);
      val C1 = Math.pow(e2 * Math.cos(fp), 2);
      val T1 = Math.pow(Math.tan(fp), 2);
      val R1 = a * (1 - Math.pow(e, 2)) / Math.pow((1 - Math.pow(e, 2) * Math.pow(Math.sin(fp), 2)), (3.0 / 2.0));
      val N1 = a / Math.pow((1 - Math.pow(e, 2) * Math.pow(Math.sin(fp), 2)), 0.5);

      val D = x / (N1 * k0);

      // 計算緯度
      val Q1 = N1 * Math.tan(fp) / R1;
      val Q2 = (Math.pow(D, 2) / 2.0);
      val Q3 = (5 + 3 * T1 + 10 * C1 - 4 * Math.pow(C1, 2) - 9 * e2) * Math.pow(D, 4) / 24.0;
      val Q4 = (61 + 90 * T1 + 298 * C1 + 45 * Math.pow(T1, 2) - 3 * Math.pow(C1, 2) - 252 * e2) * Math.pow(D, 6) / 720.0;
      var lat = fp - Q1 * (Q2 - Q3 + Q4);

      // 計算經度
      val Q5 = D;
      val Q6 = (1 + 2 * T1 + C1) * Math.pow(D, 3) / 6;
      val Q7 = (5 - 2 * C1 + 28 * T1 - 3 * Math.pow(C1, 2) + 8 * e2 + 24 * Math.pow(T1, 2)) * Math.pow(D, 5) / 120.0;
      var lon = lon0 + (Q5 - Q6 + Q7) / Math.cos(fp);

      lat = (lat * 180) / Math.PI; //緯
      lon = (lon * 180) / Math.PI; //經

      (lon, lat)
    }
}