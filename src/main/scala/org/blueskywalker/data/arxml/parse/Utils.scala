package org.blueskywalker.data.arxml.parse

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

class Utils {
/*
  def getEThernetPhysicalChannel(): Seq[Network] = {
    val result = new ArrayBuffer[Network]()
    val channels = topology \\ "ETHERNET-PHYSICAL-CHANNEL"
    channels.foreach {
      (n: Node) =>
        val name = getName(n)
        val vlan = getHeadText(n \\ "VLAN-IDENTIFIER").get.toInt
        val ip4s = new ArrayBuffer[Endpoint]()
        println((n \\ "NETWORK-ENDPOINTS").length)
        (n \ "IPV-4-CONFIGURATION").foreach {
          (s: Node) =>
            val addr = (s \ "IPV-4-ADDRESS").text
            val gateway = (s \ "DEFAULT-GATEWAY").text
            val addrSrc = (s \ "IPV-4-ADDRESS-SOURCE").text
            val mask = (s \ "NETWORK-MASK").text
          //            ip4s += Endpoint(addr, addrSrc, gateway, mask)
        }

        val addrs = new ArrayBuffer[SocketAddress]()
        (n \\ "SOCKET-ADDRESS").foreach {
          (s: Node) =>
            val name = getName(n)
            val portNode = (s \\ "PORT-NUMBER")
            val port = if (portNode.length > 0) portNode.head.text.toInt else 0
            val assingedNode = (s \\ "DYNAMICALLY-ASSIGNED")
            val assinged = if (assingedNode.length > 0) assingedNode.head.text.toBoolean else false
            addrs += SocketAddress(name, port, assinged)
        }
        var pdu_ref = HashMap[String, Int]()
        (n \\ "SOCKET-CONNECTION-IPDU-IDENTIFIER").foreach {
          (s: Node) =>
            val idNode = (s \ "HEADER-ID")
            val id: Option[Int] = if (idNode.length > 0) Some(idNode.head.text.toInt) else None
            val refNode = (s \ "PDU-TRIGGERING-REF")
            val ref: Option[String] = if (refNode.length > 0) Some(refNode.head.text.split("/").last) else None

            if (id.isDefined && ref.isDefined) {
              pdu_ref += (ref.get -> id.get)
            }
        }
        val pdus = new ArrayBuffer[PduRef]()
        (n \\ "PDU-TRIGGERING").foreach {
          (s: Node) =>
            val name = getName(s)
            val ref = (s \ "I-PDU-REF").head.text
            pdus += PduRef(name, ref)
            val id = pdu_ref get name
            if (id != None)
              idMap += (id.get -> ref.substring("/Communication/PDUs/".length))
        }
        result += Network(name, vlan, ip4s.toSeq, addrs.toSeq, pdus.toSeq, new ArrayBuffer[Message])
    }
    result.toSeq
  }
*/
}
