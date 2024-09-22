package helpers

import io.circe.Json
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoContract
import rosen.bridge.{Contracts, TokensMap}
import scorex.crypto.hash.Digest32

import java.math.BigInteger

object Utils {
  private val secureRandom = new java.security.SecureRandom

  def selectConfig(networkName: String, networkType: String) : (NetworkGeneral, Network) = {
    (
      Configs.generalConfig(networkType),
      Configs.allNetworksToken((networkName, networkType))
    )
  }

  /**
   * Create json of TokenMap
   * @param networkVersion String ex: 1.0.0
   * @param networkType String ex: mainnet-alpha-1
   */
  def createTokenMap(networkVersion: String, networkType: String = ""): Unit = {
    val generalConfig = Configs.generalConfig
    generalConfig.keys.toSeq.foreach(conf => {
      if ((conf contains networkType) || networkType.isEmpty){
        val fileType = conf ++ ".json"
        val tokensMap = TokensMap.readTokensFromFiles(Configs.tokensMapDirPath, List(fileType))
        val idKeys = TokensMap.createIdKeysJson()
        val tokensMapJson = tokensMap.deepMerge(
          idKeys
        ).deepMerge(
          Json.fromFields(List(("version", Json.fromString(networkVersion))))
        ).toString()
        TokensMap.createTokensMapJsonFile(tokensMapJson, conf, networkVersion)
        println(s"Json of TokensMap created for network type $conf!")
      }
    })
  }

  /**
   * Create json of contracts
   * @param networkVersion String ex: 1.0.0
   * @param networkName String ex: cardano
   * @param networkType String ex: mainnet-alpha-1
   */
  def createContracts(networkVersion: String, networkName: String = "", networkType: String = ""): Unit = {
    if(networkName.nonEmpty) {
      val networkConfig: (NetworkGeneral, Network) = selectConfig(networkName, networkType)
      val contracts = new Contracts(networkConfig._1, networkConfig._2)
      contracts.createContractsJson(
        networkName,
        networkType,
        networkVersion,
      )
      println("Json of Contracts created!")
    }
    else{
      val generalConfig = Configs.generalConfig
      val allNetworksToken = Configs.allNetworksToken

      allNetworksToken.keys.toSeq.foreach(network => {
        if ((network._2 contains networkType) || networkType.isEmpty){
          val networkObj = allNetworksToken(network)
          val generalObj = generalConfig(network._2)
          val contracts = new Contracts(generalObj, networkObj)
          contracts.createContractsJson(
            network._1,
            network._2,
            networkVersion,
          )
          println(s"Json of Contracts created for network ${network._1} with type ${network._2}!")
        }
      })
    }
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
