package org.blueskywalker.data.arxml.parse

import scala.collection.mutable.ArrayBuffer

object Endian extends Enumeration {
  val Big, Little = Value
}

case class DataUnit(name:String, unit:String)

class Signal(val name:String, val ref:String, val startBit:Int, val endian:Endian.Value, var len:Int=0, var slope:Float=0,
             var intercept: Float=0, var min:Option[Float]=None, var max:Option[Float]=None,
             var unit:Option[String]=None, var receiverNode:Option[Seq[String]]=None, var isSigned:Boolean=false,
             var desc:Option[String]=None, var valueDesc: Option[Map[Int, String]]=None)

class Message(val name:String, var id:Int=0, val signals:Seq[Signal],
              val ecu:ArrayBuffer[String]=new ArrayBuffer[String](), var desc:Option[String]=None)
