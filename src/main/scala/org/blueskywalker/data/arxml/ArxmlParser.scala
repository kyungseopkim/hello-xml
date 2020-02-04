package org.blueskywalker.data.arxml

import java.nio.file.Path

import org.blueskywalker.data.arxml.json.VLAN
import org.blueskywalker.data.arxml.parse.{CompuMethod, CompuRationalCoeffs, CompuScale, DataUnit, ECU, Endian, Frame, FrameType, Endpoint, ISignal, Message, Network, PDUGroup, PduRef, PduToFrame, Signal, SocketAddress}

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.{Node, NodeSeq, XML}
import util.control.Breaks.{break, breakable}

class ArxmlParser(xmlPath: Path) {
  private val nameTag = "SHORT-NAME"
  private val xml: scala.xml.Elem = XML.loadFile(xmlPath.toFile)
  private val topology = getPackage(xml, "Topology").getOrElse(xml)
  private val communication = getPackage(xml, "Communication").getOrElse(xml)
  private var idMap: Map[Int, String] = new HashMap[Int, String]()

  private def getHeadText(items: NodeSeq): Option[String] = if (items.length > 0) Some(items.head.text) else None

  private def getName(item: Node): String = (item \ nameTag).head.text

  private def getPackage(root: Node, name: String): Option[Node] = {
    val packages = root \\ "AR-PACKAGE"
    packages.foreach {
      (n: Node) =>
        if (getName(n) == name)
          return Some(n)
    }
    return None
  }

  def getEThernetCluster(): Seq[Network] = {
    val result = new ArrayBuffer[Network]()

    val clusters = getPackage(topology, "Clusters").get
    val cluster = (clusters \\ "ETHERNET-CLUSTER")
    cluster.foreach {
      (n: Node) =>
        if (getName(n) == "Ethernet_Cluster") {
          (n \\ "ETHERNET-PHYSICAL-CHANNEL").foreach {
            (chan: Node) =>
              val name = getName(chan)
              val vlan = getHeadText(chan \\ "VLAN-IDENTIFIER").get.toInt
              val ip4s = new ArrayBuffer[Endpoint]()

              (chan \\ "NETWORK-ENDPOINT").foreach {
                (ep: Node) =>
                  val name = getName(ep)
                  val priorityText = getHeadText(ep \ "PRIORITY")
                  val priority = if (priorityText==None) None else Some(priorityText.get.toInt)
                  val addr = (ep \\ "IPV-4-ADDRESS").text
                  val gateway = (ep \\ "DEFAULT-GATEWAY").text
                  val addrSrc = (ep \\ "IPV-4-ADDRESS-SOURCE").text
                  val mask = (ep \\ "NETWORK-MASK").text
                  ip4s += Endpoint(name, priority, addr, addrSrc, gateway, mask)
              }

              val addrs = new ArrayBuffer[SocketAddress]()
              (chan \\ "SOCKET-ADDRESS").foreach {
                (s: Node) =>
                  val name = getName(n)
                  val portNode = (s \\ "PORT-NUMBER")
                  val port = if (portNode.length > 0) portNode.head.text.toInt else 0
                  val assingedNode = (s \\ "DYNAMICALLY-ASSIGNED")
                  val assinged = if (assingedNode.length > 0) assingedNode.head.text.toBoolean else false
                  addrs += SocketAddress(name, port, assinged)
              }

              var pdu_ref = HashMap[String, Int]()
              (chan \\ "SOCKET-CONNECTION-IPDU-IDENTIFIER").foreach {
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
              (chan \\ "PDU-TRIGGERING").foreach {
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
        }
    }
    result.toSeq
  }

  private def getEcu(): Seq[ECU] = {
    val hc = getPackage(topology, "HardwareComponents")
    val ecus = new ArrayBuffer[ECU]()
    if (hc.isDefined) {
      val instances = hc.get \\ "ECU-INSTANCE"
      instances.foreach {
        (n: Node) =>
          val name = getName(n)
          val pdus = (n \\ "ASSOCIATED-COM-I-PDU-GROUP-REF").map(_.text).map(_.substring("/Communication/PDUs/".length))
          val vlans = (n \\ "VLAN-REF").map(_.text).map(_.substring("/Topology/Clusters/Ethernet_Cluster/".length))
          ecus += ECU(name, pdus, vlans)
      }
    }
    ecus.toSeq
  }

  def getMessage(): Seq[Message] = {
    val pdu = getPackage(communication, "PDUs").get
    val messages = new ArrayBuffer[Message]()

    (pdu \\ "I-SIGNAL-I-PDU").foreach {
      (n: Node) =>
        val name = getName(n)
        val lengthNodeSeq = (n \ "LENGTH")
        val length = if (lengthNodeSeq.length == 0) 0 else getHeadText(lengthNodeSeq).get.toInt
        val signals = new ArrayBuffer[Signal]()
        (n \\ "I-SIGNAL-TO-I-PDU-MAPPING").foreach {
          (s: Node) =>
            breakable {
              val signalRef = (s \ "I-SIGNAL-REF")
              val name = if (signalRef.length == 0) getName(s) else getHeadText(signalRef).get.substring("/Communication/Signals/".length)
              val byteOrder = (s \ "PACKING-BYTE-ORDER")
              if (byteOrder.length == 0) break
              val endian = if (getHeadText(byteOrder).get == "MOST-SIGNIFICANT-BYTE-LAST") Endian.Little else Endian.Big
              var startBit = getHeadText(s \ "START-POSITION").get.toInt
              if (endian == Endian.Big) {
                startBit = startBit - (startBit % 8) + 7 - (startBit % 8)
              }
              val ref = getHeadText(signalRef).get.substring("/Communication/Signals/".length)
              signals += new Signal(name, ref, startBit, endian)
            }
        }
        messages += new Message(name, signals = signals.toSeq)
    }
    messages.toSeq
  }


  def getPduGroup(): Seq[PDUGroup] = {
    val pduGroup = new ArrayBuffer[PDUGroup]()
    (communication \\ "I-SIGNAL-I-PDU-GROUP").foreach {
      (n: Node) =>
        breakable {
          val direction = getHeadText(n \ "COMMUNICATION-DIRECTION").get
          if (direction == "IN") break
          val name = getName(n)
          val pdu = (n \\ "I-SIGNAL-I-PDU-REF").map(_.text.substring("/Communication/PDUs/".length))
          pduGroup += PDUGroup(name, pdu.toSeq)
        }
    }
    pduGroup.toSeq
  }

  def getFrame(): Seq[Frame] = {
    val frames = new ArrayBuffer[Frame]()
    val frame = getPackage(communication, "Frames")

    if (frame.isDefined) {
      val elements = (frame.get \ "ELEMENTS")
      elements.foreach {
        (n: Node) =>
          n.child.foreach {
            (s: Node) =>
              breakable {
                val kind = s.label match {
                  case "CAN-FRAME" => FrameType.CAN
                  case "ETHERNET-FRAME" => FrameType.ETHERNET
                  case "LIN-UNCONDITIONAL-FRAME" => FrameType.LIN_UNCONDITIONAL
                  case _ => FrameType.None
                }
                if (kind == FrameType.None)
                  break
                val name = getName(s)
                val length = getHeadText(s \ "FRAME-LENGTH").get.toInt
                val desc = getHeadText(s \ "DESC")
                val pdu = (s \\ "PDU-TO-FRAME-MAPPING").map {
                  (m: Node) =>
                    val name = getName(m)
                    val endian = if (getHeadText((m \ "PACKING-BYTE-ORDER")).get == "MOST-SIGNIFICANT-BYTE-LAST")
                      Endian.Little else Endian.Big
                    val pdu = getHeadText(m \ "PDU-REF").get
                    val start = getHeadText(m \ "START-POSITION").get.toInt
                    PduToFrame(name, endian, pdu, start)
                }
                frames += Frame(kind, name, length, pdu, desc)
              }
          }
      }
    }
    frames.toSeq
  }

  def getISignal(): Seq[ISignal] = {
    val signals = new ArrayBuffer[ISignal]()
    (communication \\ "I-SIGNAL").foreach {
      (n: Node) =>
        val name = getName(n)
        val desc = getHeadText(n \ "DESC")
        val length = getHeadText(n \ "LENGTH").get.toInt
        val initNode = getHeadText(n \ "VALUE")
        val init = if (initNode != None) initNode.get.toInt else 0
        val refNode = getHeadText(n \\ "COMPU-METHOD-REF")
        val ref = if (refNode != None) refNode.get.substring("/DataTypes/CompuMethods/".length) else ""
        val dataType = getHeadText(n \\ "BASE-TYPE-REF") match {
          case Some(v) => v.contains("SINT")
          case _ => false
        }
        signals += new ISignal(name, length, if (desc == None) null else desc.get,
          init, new ArrayBuffer[String](), dataType, ref)
    }
    signals.toSeq
  }

  def getTrigger(signal: Map[String, ISignal]): Unit = {
    (topology \\ "I-SIGNAL-TRIGGERINGS").foreach {
      (n: Node) =>
        breakable {
          val ref = (n \\ "I-SIGNAL-REF")
          if (ref.length == 0) break
          val sigRef = getHeadText(ref).get.substring("/Communication/Signals/".length)
          val ports = (n \\ "I-SIGNAL-PORT-REF")
          if (ports.length == 0) break
          ports.foreach {
            (s: Node) =>
              val str = getHeadText(s).get.trim
              val tokens = str.substring(1).split("/")
              if (tokens.last.substring(tokens.last.length - 3) == "_in") {
                signal(sigRef).receivers += tokens(2)
              }
          }
        }
    }
  }

  def getUnit(): Seq[DataUnit] = {
    val dataType = getPackage(xml, "DataTypes").get
    val unit = getPackage(dataType, "Units").get
    val units = new ArrayBuffer[DataUnit]()
    (unit \ "UNIT").foreach {
      (n: Node) =>
        val name = getName(n)
        val unit = getHeadText(n \ "DISPLAY-NAME").get
        units += DataUnit(name, unit)
    }
    units.toSeq
  }

  def getCompuMethod(): Seq[CompuMethod] = {

    def readFailureToZero(number: String): Double = try {
      number.toDouble
    } catch {
      case ex: NumberFormatException => 0
    }

    def noneExistsToZero(value: Option[String]) = value match {
      case Some(str) => readFailureToZero(str)
      case _ => 0
    }

    def getCompuScale(compuScale: Seq[Node]): Seq[CompuScale] = {
      val result = new ArrayBuffer[CompuScale]()
      (compuScale).foreach {
        (s: Node) =>
          (s \\ "COMPU-SCALE").foreach {
            (v: Node) =>
              val label: String = getHeadText(v \ "SHORT-LABEL").getOrElse("")
              val minV: Double = noneExistsToZero(getHeadText(v \ "LOWER-LIMIT"))
              val maxV: Double = noneExistsToZero(getHeadText(v \ "UPPER-LIMIT"))

              val numerators = (v \\ "V")
              val const = getHeadText(v \\ "VT").getOrElse("")
              var coeff: CompuRationalCoeffs = null
              if (numerators.length > 0) {
                val vals = numerators.map(getHeadText(_).get)
                coeff = CompuRationalCoeffs(vals(0).toDouble, vals(1).toDouble, vals(2).toInt, const)
              }
              result += CompuScale(label, minV, maxV, coeff)
          }
      }
      result.toSeq
    }

    val dataType = getPackage(xml, "DataTypes").get
    val methods = getPackage(dataType, "CompuMethods").get
    val categories = mutable.Map[String, Int]().withDefaultValue(0)
    val compuMethods = new ArrayBuffer[CompuMethod]()

    (methods \\ "COMPU-METHOD").foreach {
      (n: Node) =>
        breakable {
          val name = getName(n)
          val category = getHeadText(n \ "CATEGORY").get
          categories(category) += 1
          val refs = (n \ "UNIT-REF")
          val unitRef = if (refs.length > 0) getHeadText(refs).get.substring("/DataTypes/Units/".length) else ""
          if (category == "IDENTICAL") break
          val internal = getCompuScale(n \ "COMPU-INTERNAL-TO-PHYS")
          val phys = getCompuScale(n \ "COMPU-PHYS-TO-INTERNAL")

          compuMethods += CompuMethod(name, category, unitRef, internal, phys)
        }
    }
    compuMethods.toSeq
  }

  def parse(): Unit = {
    val vlans = getEThernetCluster()
    val messages = getMessage()
    val frames = getFrame()
    val ecus = getEcu()
    val isignal = getISignal()

    val msgMap = messages.map { m => m.name -> m }.toMap
    val pduMap = getPduGroup().map { pdu => pdu.name -> pdu.pdu }.toMap
    val signalMap = isignal.map { s => s.name -> s }.toMap
    val unitMap = getUnit().map { u => u.name -> u }.toMap
    val compuMap = getCompuMethod().map { c => c.name -> c }.toMap

    // assign ids to message
    idMap.keys.foreach { (id: Int) =>
      msgMap(idMap(id)).id = id
    }

    // assign ecp to message
    ecus.foreach {
      (e: ECU) =>
        e.pdu.foreach {
          (lpdu: String) =>
            if (pduMap contains lpdu) {
              pduMap(lpdu).foreach {
                (pdu: String) =>
                  val msg = msgMap get pdu
                  if (msg.isDefined) {
                    msg.get.ecu += e.name
                  }
              }
            }
        }
    }

    // assign desc to message
    frames.foreach { (f: Frame) =>
      f.pdu.foreach {
        (ref: PduToFrame) =>
          if (msgMap contains ref.pdu)
            msgMap(ref.pdu).desc = f.desc
      }
    }

    getTrigger(signalMap)

    // set signals to message
    messages.foreach {
      (m: Message) =>
        m.signals.foreach {
          (s: Signal) =>
            val key = signalMap(s.ref).compuRef
            if (compuMap contains key) {
              val compu = compuMap(key)
              val unitRef = unitMap get compu.unitRef
              s.unit = if (unitRef == None) None else Some(unitRef.get.unit)
              if (compu.internal.length > 0 &&
                (compu.category == "SCALE_LINEAR_AND_TEXTTABLE" || compu.category == "LINEAR")) {
                s.min = Some(compu.internal.head.minimum.toFloat)
                s.max = Some(compu.internal.head.maximum.toFloat)
                s.slope = (compu.internal.head.coeff.v1 / compu.internal.head.coeff.denominator).toFloat
                s.intercept = (compu.internal.head.coeff.v0 / compu.internal.head.coeff.denominator).toFloat
              }
              if (compu.internal.length > 1) {
                val applyPdu = if (compu.category == "TEXTTABLE") compu.internal else compu.internal.slice(1, compu.internal.length)
                val valueDesc = new mutable.HashMap[Int, String]()
                applyPdu.foreach {
                  (cs: CompuScale) =>
                    valueDesc += (cs.minimum.toInt -> cs.coeff.const)
                }
                s.valueDesc = Some(valueDesc.toMap)
              }

            }
            if (signalMap contains key) {
              s.len = signalMap(key).bitLength
              s.receiverNode = Some(signalMap(key).receivers.toSeq)
              s.isSigned = signalMap(key).isSigned
            }
        }
    }

    // set message to vlan
    vlans.foreach {
      (n: Network) =>
        n.pdu.foreach {
          (pdu: PduRef) =>
            val key = pdu.ref.substring("/Communication/PDUs/".length)
            if (msgMap contains key) {
              n.msg += msgMap(key)
            }
        }
    }

    // form json object
    val jsonVlans = vlans.map {
      (network: Network) =>
        if (network.endpoint.nonEmpty) {
          network.endpoint.map {
            (info: Endpoint) =>
            info.name
          }
        }
      //        VLAN(network.name,network.vlan, )
    }
  }

}
