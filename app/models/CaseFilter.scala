package models

object CaseFilter extends Enumeration {
  val Ownerless = Value
  val MyCase = Value
  val AllCase = Value
  val SubmittedByMe = Value
  val SubmittedCases = Value
}