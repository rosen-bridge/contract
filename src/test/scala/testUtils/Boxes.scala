package testUtils

import helpers.{Configs, Network, NetworkGeneral, Utils}
import org.ergoplatform.appkit.scalaapi.ErgoValueBuilder
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoToken, InputBox, OutBox}
import rosen.bridge.Contracts
import sigmastate.eval.Colls
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

  val networkConfig: (NetworkGeneral, Network) = Utils.selectConfig("cardano", "mainnet")
  val contracts = new Contracts(networkConfig._1, networkConfig._2)

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

  def createBoxForUser(ctx: BlockchainContext, address: Address, amount: Long, WID: Array[Byte], tokens: ErgoToken*): InputBox = {
    createBoxCandidateForUser(ctx, address, amount, WID, tokens: _*).convertToInputWith(getRandomHexString(), 0)
  }

  def createBoxCandidateForUser(ctx: BlockchainContext, address: Address, amount: Long, tokens: ErgoToken*): OutBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(ctx.newContract(address.asP2PK().script))
      .build()
  }

  def createBoxCandidateForUser(ctx: BlockchainContext, address: Address, amount: Long): OutBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .contract(ctx.newContract(address.asP2PK().script))
      .build()
  }

  def createBoxCandidateForUser(ctx: BlockchainContext, address: Address, amount: Long, WID: Array[Byte], tokens: ErgoToken*): OutBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(ctx.newContract(address.asP2PK().script))
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(WID)),
      )
      .build()
  }

  def createCustomBox(ctx: BlockchainContext, contract: ErgoContract, amount: Long, tokens: ErgoToken*): InputBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(contract)
      .build().convertToInputWith(getRandomHexString(), 1)

  }

  def createCustomBox(ctx: BlockchainContext, contract: ErgoContract, amount: Long): InputBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .contract(contract)
      .build().convertToInputWith(getRandomHexString(), 1)

  }


  def createWatcherCollateralBoxInput(ctx: BlockchainContext, erg: Long, rsn: Long, wid: Array[Byte], rwtCount: Long): InputBox = {
    Boxes.createWatcherCollateralBox(ctx, erg, rsn, wid, rwtCount).convertToInputWith(getRandomHexString(), 3)
  }

  def createWatcherCollateralBox(ctx: BlockchainContext, erg: Long, rsn: Long, wid: Array[Byte], rwtCount: Long): OutBox = {
    val builder = ctx.newTxBuilder().outBoxBuilder()
      .value(erg)
      .tokens(new ErgoToken(networkConfig._2.tokens.AwcNFT, 1))
      .contract(contracts.WatcherCollateral._1)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(wid)),
        ErgoValueBuilder.buildFor(rwtCount),
      )
    if (rsn > 0) builder.tokens(new ErgoToken(networkConfig._1.mainTokens.RSN, rsn))
    builder.build()
  }

  def createFakeWatcherCollateralBox(ctx: BlockchainContext, erg: Long, rsn: Long, wid: Array[Byte], rwtCount: Long): OutBox = {
    ctx.newTxBuilder().outBoxBuilder()
      .value(erg)
      .tokens(
        new ErgoToken(networkConfig._1.mainTokens.RSN, 1),
        new ErgoToken(networkConfig._1.mainTokens.RSN, rsn)
      )
      .contract(contracts.WatcherCollateral._1)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(wid)),
        ErgoValueBuilder.buildFor(rwtCount),
      ).build()
  }

  def createRepoWithTokens(
                            ctx: BlockchainContext,
                            RWTCount: Long,
                            RSNCount: Long,
                            awcCount: Long,
                            watcherCount: Long,
                            nftId: String,
                            rwtId: String,
                            awcId: String,
                            value: Long = Configs.minBoxValue,
                          ): OutBox = {
    val txB = ctx.newTxBuilder()
    val repoBuilder = txB.outBoxBuilder()
      .value(value)
      .tokens(
        new ErgoToken(nftId, 1),
        new ErgoToken(rwtId, RWTCount),
        new ErgoToken(networkConfig._1.mainTokens.RSN, RSNCount),
        new ErgoToken(awcId, awcCount)
      )
      .contract(contracts.RWTRepo._1)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray("ADA".getBytes())),
        ErgoValueBuilder.buildFor(watcherCount),
      )
    repoBuilder.build()
  }

  def createRepoConfigsWithList(
                                 ctx: BlockchainContext,
                                 nftId: String,
                                 configs: Array[Long]
                               ): OutBox = {
    val txB = ctx.newTxBuilder()
    val repoBuilder = txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .tokens(
        new ErgoToken(nftId, 1),
      )
      .contract(contracts.RepoConfig._1)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(configs)),
      )
    repoBuilder.build()
  }

  def createRepoConfigs(ctx: BlockchainContext, nft: String = networkConfig._2.tokens.RepoConfigNFT): OutBox = {
    createRepoConfigsWithList(ctx, nft, Array(10L, 51L, 0L, 9999L, 1e9.toLong, 100))
  }

  def createRepoConfigsInput(ctx: BlockchainContext): InputBox = {
    createRepoConfigs(ctx).convertToInputWith(Boxes.getRandomHexString(), 1)
  }

  def createRepo(
                  ctx: BlockchainContext,
                  RWTCount: Long,
                  RSNCount: Long,
                  AwcCount: Long,
                  watcherCount: Long,
                  value: Long = Configs.minBoxValue,
                ): OutBox = {
    createRepoWithTokens(ctx, RWTCount, RSNCount, AwcCount, watcherCount, networkConfig._1.mainTokens.RWTRepoNFT, networkConfig._2.tokens.RWTId, networkConfig._2.tokens.AwcNFT, value)
  }

  def createRepoInput(
                       ctx: BlockchainContext,
                       RWTCount: Long,
                       RSNCount: Long,
                       AwcCount: Long,
                       watcherCount: Long,
                       value: Long = Configs.minBoxValue,
                     ): InputBox = {
    createRepo(ctx, RWTCount, RSNCount, AwcCount, watcherCount, value).convertToInputWith(Boxes.getRandomHexString(), 0)
  }

  def createPermitBox(ctx: BlockchainContext, RWTCount: Long, WID: Array[Byte], tokens: ErgoToken*): OutBox = {
    val txB = ctx.newTxBuilder()
    val tokensSeq = Seq(new ErgoToken(networkConfig._2.tokens.RWTId, RWTCount)) ++ tokens.toSeq
    txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.WatcherPermit._1)
      .tokens(tokensSeq: _*)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(WID)),
        // this value must exists in case of redeem commitment.
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(Array(0.toByte)).map(item => Colls.fromArray(item)).toArray)),
      )
      .build()
  }

  def createInvalidMixedPermitBox(ctx: BlockchainContext, RWTCount: Long, WID: Array[Byte], tokens: ErgoToken*): OutBox = {
    val txB = ctx.newTxBuilder()
    val tokensSeq = Seq(
      new ErgoToken(networkConfig._1.mainTokens.RSN, RWTCount),
      new ErgoToken(networkConfig._2.tokens.RWTId, RWTCount),
    ) ++ tokens.toSeq
    txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.WatcherPermit._1)
      .tokens(tokensSeq: _*)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(WID).map(item => Colls.fromArray(item)).toArray)),
        // this value must exists in case of redeem commitment.
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(Array(0.toByte)).map(item => Colls.fromArray(item)).toArray)),
      )
      .build()
  }

  def createInvalidPermitBox(ctx: BlockchainContext, RWTCount: Long, WID: Array[Byte], tokens: ErgoToken*): OutBox = {
    val txB = ctx.newTxBuilder()
    val tokensSeq = Seq(new ErgoToken(networkConfig._1.mainTokens.RSN, RWTCount)) ++ tokens.toSeq
    txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.WatcherPermit._1)
      .tokens(tokensSeq: _*)
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(WID).map(item => Colls.fromArray(item)).toArray)),
        // this value must exists in case of redeem commitment.
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(Array(0.toByte)).map(item => Colls.fromArray(item)).toArray)),
      )
      .build()
  }

  def createFraudBox(ctx: BlockchainContext, WID: Array[Byte], RWTCount: Long): OutBox = {
    val txB = ctx.newTxBuilder()
    txB.outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.Fraud._1)
      .tokens(new ErgoToken(networkConfig._2.tokens.RWTId, RWTCount))
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(WID))
      )
      .build()
  }

  def createCommitment(ctx: BlockchainContext, WID: Array[Byte], eventId: Array[Byte], commitment: Array[Byte], RWTCount: Long): OutBox = {
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.Commitment._1)
      .tokens(new ErgoToken(networkConfig._2.tokens.RWTId, RWTCount))
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(WID)),
        ErgoValueBuilder.buildFor(Colls.fromArray(eventId)),
        ErgoValueBuilder.buildFor(Colls.fromArray(commitment)),
        ErgoValueBuilder.buildFor(Colls.fromArray(Utils.getContractScriptHash(contracts.WatcherPermit._1))),
      ).build()
  }

  def createTriggerEventBox(ctx: BlockchainContext, WID: Seq[Array[Byte]], commitment: Commitment, RWTCount: Long, commitmentCount: Option[Int] = None): OutBox = {
    val size = WID.length
    val digest = scorex.crypto.hash.Blake2b256(WID.reduce((a, b) => a ++ b))
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue * size)
      .contract(contracts.WatcherTriggerEvent._1)
      .tokens(new ErgoToken(networkConfig._2.tokens.RWTId, RWTCount))
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(digest)),
        ErgoValueBuilder.buildFor(Colls.fromArray(commitment.partsArray().map(item => Colls.fromArray(item)))),
        ErgoValueBuilder.buildFor(Colls.fromArray(Utils.getContractScriptHash(contracts.WatcherPermit._1))),
        ErgoValueBuilder.buildFor(commitmentCount.getOrElse(WID.size))
      ).build()
  }


  def createFakeTriggerEventBox(ctx: BlockchainContext, WID: Seq[Array[Byte]], commitment: Commitment, RWTCount: Long): OutBox = {
    val size = WID.length
    val digest = scorex.crypto.hash.Blake2b256(WID.slice(1, WID.size).reduce((a, b) => a ++ b))
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue * size)
      .contract(contracts.WatcherTriggerEvent._1)
      .tokens(new ErgoToken(networkConfig._2.tokens.RWTId, RWTCount))
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(digest).map(item => Colls.fromArray(item)).toArray)),
        ErgoValueBuilder.buildFor(Colls.fromArray(commitment.partsArray().map(item => Colls.fromArray(item)))),
        ErgoValueBuilder.buildFor(Colls.fromArray(Utils.getContractScriptHash(contracts.WatcherPermit._1))),
        ErgoValueBuilder.buildFor(WID.size)
      ).build()
  }

  def createGuardNftBox(ctx: BlockchainContext, guardPks: Array[Array[Byte]], requiredSign: Int, requiredUpdate: Int): OutBox = {
    ctx.newTxBuilder().outBoxBuilder()
      .value(Configs.minBoxValue)
      .contract(contracts.GuardSign._1)
      .tokens(new ErgoToken(networkConfig._1.mainTokens.GuardNFT, 1))
      .registers(
        ErgoValueBuilder.buildFor(Colls.fromArray(guardPks.map(item => Colls.fromArray(item)))),
        ErgoValueBuilder.buildFor(Colls.fromArray(Seq(requiredSign, requiredUpdate).toArray)),
      )
      .build()
  }

  def createLockBox(ctx: BlockchainContext, amount: Long, tokens: ErgoToken*): OutBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(contracts.Lock._1)
      .build()
  }

  def createLockBox(ctx: BlockchainContext, amount: Long): OutBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .contract(contracts.Lock._1)
      .build()
  }

  def getTokenCount(TokenId: String, box: InputBox): Long = {
    val RWT = box.getTokens.asScala.filter(token => token.getId.toString == TokenId).toArray
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
    })
    tokens.update("", totalErg)
    tokens
  }

  def createEmissionBox(ctx: BlockchainContext, amount: Long, tokens: ErgoToken*): OutBox = {
    val txb = ctx.newTxBuilder()
    txb.outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(contracts.Emission._1)
      .build()
  }

}
