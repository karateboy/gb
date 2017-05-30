object test {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  //val bedRegEx = """\u7ba1(\d+)\u5e8a""".r
  val bedRegEx = """\u7ba1(\d+)\u5e8a""".r.unanchored
                                                  //> bedRegEx  : scala.util.matching.UnanchoredRegex = 蝞�(\d+)摨�
  
  val sample = """
  53
25
(含插2管10床)
  """                                             //> sample  : String = "
                                                  //|   53
                                                  //| 25
                                                  //| (����2蝞�10摨�)
                                                  //|   "

  sample match {
    case bedRegEx(bed) =>
      Some(bed.toInt)
    case _ =>
      None
  }                                               //> res0: Option[Int] = Some(10)
}