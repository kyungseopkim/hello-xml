package org.blueskywalker.data.arxml.parse

case class ECU(name:String, pdu:Seq[String], vlan:Seq[String])
case class PDUGroup(name:String, pdu:Seq[String])
