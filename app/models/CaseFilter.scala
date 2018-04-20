package models

object CaseFilter extends Enumeration {
  val Ownerless = Value
  val MyCase = Value
  val AllCase = Value
  val SubmittedByMe = Value
  val SubmittedCases = Value
  val ClosedByMe = Value
  val ClosedList = Value
  val EscalatedByMe = Value
  val EscalatedList = Value
}

abstract class FilterInterface {
  
}

object CaseState extends Enumeration {
  val Unknown = Value
  val Escalated = Value
  val Closed = Value
}