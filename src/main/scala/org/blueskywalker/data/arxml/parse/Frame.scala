package org.blueskywalker.data.arxml.parse

object FrameType extends Enumeration {
  val CAN, ETHERNET, LIN_UNCONDITIONAL, None = Value
}

case class PduToFrame(name:String, endian: Endian.Value, pdu:String, startPos:Int)
case class Frame(ftype:FrameType.Value, name:String, length:Int, pdu:Seq[PduToFrame], desc:Option[String])
