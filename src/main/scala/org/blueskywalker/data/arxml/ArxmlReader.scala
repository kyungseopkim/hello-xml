package org.blueskywalker.data.arxml

import java.nio.file.Paths


object ArxmlReader extends App {

  if (args.length < 1) {
    println("needs xml file path")
    System.exit(0)
  }

  val parser = new ArxmlParser(Paths.get(args(0)))

//  parser.parse()
  parser.getEThernetCluster()

//  def readISignal(nodes: NodeSeq): Array[ISignal] = {
//    val result = new ArrayBuffer[ISignal]()
//    nodes.foreach {
//      (n: Node) =>
//        val name: String = getName(n)
//        val desc: String = getHeadText(n \ "DESC")
//        val init: Int = getHeadText(n \\ "VALUE").toInt
//    }
//    result.toArray
//  }


//  val ecu = getEcu(topology)



  //  val cluster = arpackages \\ "ETHERNET-CLUSTER"
  //  val ethernet = cluster.filter((n:Node) => getHeadText(n \ nameTag) == "Ethernet_Cluster")
  //  val networks = readEThernetCluster(ethernet)
  //  networks.foreach(println)
  //  val iSignal = arpackages \\ "I-SIGNAL"

  //  println(iSignal.head)
  //  readISignal(iSignal)
}
