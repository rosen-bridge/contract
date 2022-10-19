package helpers

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoContract
import scorex.crypto.hash.Digest32

import java.math.BigInteger

object Utils {
  private val secureRandom = new java.security.SecureRandom

  def selectConfig(networkName: String, networkType: String) : (ErgoNetwork, Network, MainTokens) = {
    (
      Configs.generalConfig(networkType)._1,
      Configs.allNetworksToken((networkName, networkType)),
      Configs.generalConfig(networkType)._2
    )
  }

  def randBigInt: BigInt = new BigInteger(256, secureRandom)

  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  def getContractAddress(contract: ErgoContract, addressEncoder: ErgoAddressEncoder): String = {
    val ergoTree = contract.getErgoTree
    addressEncoder.fromProposition(ergoTree).get.toString
  }
}
