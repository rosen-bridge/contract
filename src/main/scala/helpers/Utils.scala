package helpers

import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoType, ErgoValue, JavaHelpers}
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.serialization.ErgoTreeSerializer
import special.collection.Coll
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

import java.math.BigInteger
import scala.util.Random

object Utils {
  private val random = Random
  private val secureRandom = new java.security.SecureRandom

  def selectConfig(networkName: String, networkType: String) : (Tokens, MainTokens) = {
    (Configs.allNetworksToken((networkName, networkType)), Configs.mainTokens(networkType))
  }

  def randBigInt: BigInt = new BigInteger(256, secureRandom)

  def toByteArray(s: String): Array[Byte] = Base16.decode(s).get

  def toHexString(array: Array[Byte]): String = Base16.encode(array)

  def hexToGroupElement(data: String): GroupElement = {
    SigmaDsl.decodePoint(JavaHelpers.collFrom(toByteArray(data)))
  }

  def getAddress(addressBytes: Array[Byte]): ErgoAddress = {
    val ergoTree = ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(addressBytes)
    Configs.addressEncoder.fromProposition(ergoTree).get
  }

  def addressGetBytes(address: Address):Array[Byte] ={
    address.getErgoAddress.script.bytes
  }

  def getContractAddress(contract: ErgoContract): String = {
    val ergoTree = contract.getErgoTree
    Configs.addressEncoder.fromProposition(ergoTree).get.toString
  }

  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  def longListToErgoValue(elements: Array[Long]): ErgoValue[Coll[Long]] = {
    val longColl = JavaHelpers.SigmaDsl.Colls.fromArray(elements)
    ErgoValue.of(longColl, ErgoType.longType())
  }

  def randDouble: Double = random.nextDouble()

  def randLong(min: Long, max: Long): Long = {
    val range = max - min
    (randDouble * range).toLong + min
  }


  def randomId(): String = {
    val randomBytes = Array.fill(32)((scala.util.Random.nextInt(256) - 128).toByte)
    randomBytes.map("%02x" format _).mkString
  }

  def getProveDlogAddress(z: BigInt, ctx: BlockchainContext): String = {
    val g: GroupElement = CryptoConstants.dlogGroup.generator
    val gZ: GroupElement = g.exp(z.bigInteger)
    val contract = ctx.compileContract(
      ConstantsBuilder.create()
        .item(
          "gZ", gZ
        ).build(), "{proveDlog(gZ)}"
    )
    Configs.addressEncoder.fromProposition(contract.getErgoTree).get.toString
  }

  def randomAddr(): Address = {
    Configs.ergoClient.execute((ctx: BlockchainContext) => {
      val rnd = randBigInt
      val add = getProveDlogAddress(rnd, ctx)
      Address.create(add)
    })
  }
}
