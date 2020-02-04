package org.blueskywalker.data.arxml.parse

import scala.collection.mutable.ArrayBuffer

case class PduRef(name:String, ref:String)
case class SocketAddress(name:String, port:Int, assigned:Boolean)
case class Endpoint(name:String, priority:Option[Int]=None, addr:String="", addrSrc:String="", gateway:String="", mask:String="")
//case class EthernetCluster(name:String, vlan: Int, endpoints: String, multicastAddress: String, messages:Array[Message])
case class Network(name:String, vlan: Int, endpoint:Seq[Endpoint], addr:Seq[SocketAddress], pdu: Seq[PduRef], msg:ArrayBuffer[Message])