object test {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(57); 
  println("Welcome to the Scala worksheet");$skip(99); 
  //val bedRegEx = """\u7ba1(\d+)\u5e8a""".r
  val bedRegEx = """\u7ba1(\d+)\u5e8a""".r.unanchored;System.out.println("""bedRegEx  : scala.util.matching.UnanchoredRegex = """ + $show(bedRegEx ));$skip(46); 
  
  val sample = """
  53
25
(含插2管10床)
  """;System.out.println("""sample  : String = """ + $show(sample ));$skip(95); val res$0 = 

  sample match {
    case bedRegEx(bed) =>
      Some(bed.toInt)
    case _ =>
      None
  };System.out.println("""res0: Option[Int] = """ + $show(res$0))}
}
