package testUtils

import helpers.{Configs, MainTokens, Tokens, Utils}
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoToken, ErgoType, ErgoValue, InputBox, JavaHelpers, OutBox}
import rosen.bridge.Contracts
import scorex.util.encode.Base16

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

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

object Boxes {

  val networkConfig: (Tokens, MainTokens) = Utils.selectConfig("cardano", "testnet")
  val contracts = new Contracts(networkConfig)

  def getRandomHexString(length: Int = 64): String = {
    val r = new Random()
    val sb = new StringBuffer
    while ( {
      sb.length < length
    }) sb.append(Integer.toHexString(r.nextInt))
    sb.toString.substring(0, length)
  }

  def createBoxForUser(ctx: BlockchainContext, address: Address, amount: Long, tokens: ErgoToken*): InputBox = {
    createBoxCandidateForUser(ctx, address, amount, tokens: _*).convertToInputWith(getRandomHexString(), 0)
  }

  def createBoxForUser(ctx: BlockchainContext, address: Address, amount: Long): InputBox = {
    createBoxCandidateForUser(ctx, address, amount).convertToInputWith(getRandomHexString(), 0)
  }

  def createBoxCandidateForUser(ctx: BlockchainContext, address: Address, amount: Long, tokens: ErgoToken*): OutBox = {
    val txb = ctx.newTxBuilder();
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(ctx.newContract(address.asP2PK().script))
      .build()
  }

  def createBoxCandidateForUser(ctx: BlockchainContext, address: Address, amount: Long): OutBox = {
    val txb = ctx.newTxBuilder();
    txb.outBoxBuilder()
      .value(amount)
      .contract(ctx.newContract(address.asP2PK().script))
      .build()
  }

  def createCustomBox(ctx: BlockchainContext, contract: ErgoContract, amount: Long, tokens: ErgoToken*): InputBox = {
    val txb = ctx.newTxBuilder();
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(contract)
      .build().convertToInputWith(getRandomHexString(), 1)

  }

  def createCustomBox(ctx: BlockchainContext, contract: ErgoContract, amount: Long): InputBox = {
    val txb = ctx.newTxBuilder();
    txb.outBoxBuilder()
      .value(amount)
      .contract(contract)
      .build().convertToInputWith(getRandomHexString(), 1)

  }

  def createRepo(
                  ctx: BlockchainContext,
                  RWTCount: Long,
                  RSNCount: Long,
                  users: Seq[Array[Byte]],
                  userRWT: Seq[Long],
                  R7: Int
                ): OutBox = {
    val txB = ctx.newTxBuilder()
    val R4 = (Seq("ADA".getBytes()) ++ users).map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray
    val repoBuilder = txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .tokens(
        new ErgoToken(networkConfig._2.RepoNFT, 1),
        new ErgoToken(networkConfig._1.RWTId, RWTCount)
      )
      .contract(contracts.RWTRepo._1)
      .registers(
        ErgoValue.of(R4, ErgoType.collType(ErgoType.byteType())),
        ErgoValue.of(JavaHelpers.SigmaDsl.Colls.fromArray((Seq(0L) ++ userRWT).toArray), ErgoType.longType()),
        ErgoValue.of(JavaHelpers.SigmaDsl.Colls.fromArray(Array(100L, 51L, 0L, 9999L)), ErgoType.longType()),
        ErgoValue.of(R7)
      )
    if (RSNCount > 0) {
      repoBuilder.tokens(new ErgoToken(networkConfig._2.RSN, RSNCount))
    }
    //    println(repoBuilder.build().convertToInputWith(getRandomHexString(),0).toJson(true))
    repoBuilder.build()
  }

  def createPermitBox(ctx: BlockchainContext, RWTCount: Long, WID: Array[Byte], tokens: ErgoToken*): OutBox = {
    val txB = ctx.newTxBuilder()
    val tokensSeq = Seq(new ErgoToken(networkConfig._1.RWTId, RWTCount)) ++ tokens.toSeq
    txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.WatcherPermit._1)
      .tokens(tokensSeq: _*)
      .registers(
        ErgoValue.of(Seq(WID).map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray, ErgoType.collType(ErgoType.byteType())),
        // this value must exists in case of redeem commitment.
        ErgoValue.of(Seq(Array(0.toByte)).map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray, ErgoType.collType(ErgoType.byteType())),
      )
      .build()
  }

  def createFraudBox(ctx: BlockchainContext, WID: Array[Byte]): OutBox = {
    val txB = ctx.newTxBuilder()
    txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.Fraud._1)
      .tokens(new ErgoToken(networkConfig._1.RWTId, 1))
      .registers(
        ErgoValue.of(Seq(WID).map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray, ErgoType.collType(ErgoType.byteType())),
      )
      .build()
  }

  def createCommitment(ctx: BlockchainContext, WID: Array[Byte], RequestId: Array[Byte], commitment: Array[Byte]): OutBox = {
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.Commitment._1)
      .tokens(new ErgoToken(networkConfig._1.RWTId, 1))
      .registers(
        ErgoValue.of(Seq(WID).map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray, ErgoType.collType(ErgoType.byteType())),
        ErgoValue.of(Seq(RequestId).map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray, ErgoType.collType(ErgoType.byteType())),
        ErgoValue.of(commitment),
        ErgoValue.of(contracts.getContractScriptHash(contracts.WatcherPermit._1)),
      ).build()
  }

  def createTriggerEventBox(ctx: BlockchainContext, WID: Seq[Array[Byte]], commitment: Commitment): OutBox = {
    val R4 = WID.map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray
    val R5 = commitment.partsArray().map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item))
    val size = WID.length
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue * size)
      .contract(contracts.WatcherTriggerEvent._1)
      .tokens(new ErgoToken(networkConfig._1.RWTId, size))
      .registers(
        ErgoValue.of(R4, ErgoType.collType(ErgoType.byteType())),
        ErgoValue.of(R5, ErgoType.collType(ErgoType.byteType())),
        ErgoValue.of(contracts.getContractScriptHash(contracts.WatcherPermit._1))
      ).build()
  }

  def createGuardNftBox(ctx: BlockchainContext, guardPks: Array[Array[Byte]], requiredSign: Int, requiredUpdate: Int): OutBox = {
    val R4 = guardPks.map(item => JavaHelpers.SigmaDsl.Colls.fromArray(item)).toArray
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.guardSign._1)
      .tokens(new ErgoToken(networkConfig._2.GuardNFT, 1))
      .registers(
        ErgoValue.of(R4, ErgoType.collType(ErgoType.byteType())),
        ErgoValue.of(JavaHelpers.SigmaDsl.Colls.fromArray(Seq(requiredSign, requiredUpdate).toArray), ErgoType.integerType()),
      )
      .build()
  }

  def getTokenCount(TokenId: String, box: InputBox): Long = {
    val RWT = box.getTokens.asScala.filter(token => token.getId.toString == TokenId).toArray;
    if (RWT.length == 0) 0 else RWT(0).getValue
  }

  def calcTotalErgAndTokens(boxes: Seq[InputBox]): mutable.Map[String, Long] = {
    val tokens: mutable.Map[String, Long] = mutable.Map()
    val totalErg = boxes.map(item => item.getValue).sum
    boxes.foreach(box => {
      box.getTokens.forEach(token => {
        val tokenId = token.getId.toString
        tokens.update(tokenId, tokens.getOrElse(tokenId, 0L) + token.getValue)
      })
    });
    tokens.update("", totalErg);
    tokens
  }

}
