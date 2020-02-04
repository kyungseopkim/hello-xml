package org.blueskywalker.data.arxml.parse

import scala.collection.mutable.ArrayBuffer

class ISignal(val name:String,
              val bitLength: Int,
              val desc: String,
              val init: Int,
              var receivers:ArrayBuffer[String],
              val isSigned: Boolean,
              val compuRef: String)

case class CompuRationalCoeffs(v0: Double, v1: Double, denominator:Int, const:String)
case class CompuScale(label:String, minimum:Double, maximum:Double, coeff:CompuRationalCoeffs)
case class CompuMethod(name:String, category:String, unitRef:String, internal:Seq[CompuScale], phys:Seq[CompuScale])