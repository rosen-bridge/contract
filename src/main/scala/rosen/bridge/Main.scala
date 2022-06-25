package rosen.bridge

import helpers.{Configs, Utils}
import org.ergoplatform.appkit._
import scorex.util.encode.Base16
import special.collection.Coll

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.collection.mutable


class Commitment {
  var fromChain: String = "ADA"
  var toChain: String = "ERG"
  var fromAddress: String = ""
  var toAddress: String = ""
  var amount: Long = 100000
  var fee: Long = 2520
  var sourceChainTokenId: Array[Byte] = Base16.decode(Boxes.getRandomHexString()).get
  var targetChainTokenId: Array[Byte] = Base16.decode(Boxes.getRandomHexString()).get
  var sourceTxId: Array[Byte] = Base16.decode(Boxes.getRandomHexString()).get
  var sourceBlockId: Array[Byte] = Base16.decode(Boxes.getRandomHexString()).get

  def requestId(): Array[Byte] = {
    scorex.crypto.hash.Blake2b256(this.sourceTxId)
  }

  def partsArray(): Array[Array[Byte]] = {
    Array(
      this.sourceTxId,
      this.fromChain.getBytes(),
      this.toChain.getBytes(),
      this.fromAddress.getBytes(),
      this.toAddress.getBytes(),
      ByteBuffer.allocate(8).putLong(this.amount).array(),
      ByteBuffer.allocate(8).putLong(this.fee).array(),
      this.sourceChainTokenId,
      this.targetChainTokenId,
      this.sourceBlockId
    )
  }

  def hash(WID: Array[Byte]): Array[Byte] = {
    scorex.crypto.hash.Blake2b256(Array(
      this.sourceTxId,
      this.fromChain.getBytes(),
      this.toChain.getBytes(),
      this.fromAddress.getBytes(),
      this.toAddress.getBytes(),
      ByteBuffer.allocate(8).putLong(this.amount).array(),
      ByteBuffer.allocate(8).putLong(this.fee).array(),
      this.sourceChainTokenId,
      this.targetChainTokenId,
      this.sourceBlockId,
      WID
    ).reduce((a, b) => a ++ b))
  }
}

object Main {

  def createJson(): Unit = {

  }

  def main(args: Array[String]): Unit = {

  }
}
