package org.blueskywalker.data.arxml.json


case class ValueLabel(value:Int, name:String)
case class JsonSignal(name:String, length:Int, start_bit:Int, endianness:String,
                      slope:Float, intercept:Float, unit:String,
                      notes:String, value_labels:Seq[ValueLabel])
case class JsonMessage(name:String, id:Int, interval:Int, length:Int, signals:Seq[JsonSignal])
case class VLAN(name:String, vlan:Int, endpoints:Option[Map[String,String]]=None,
                multicaset_address: Option[String]=None, messages:Seq[JsonMessage])
case class ArxmlJson(networks:Seq[VLAN])

