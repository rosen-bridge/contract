package helpers

import io.circe.Json
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoContract
import rosen.bridge.{Contracts, TokensMap}
import scorex.crypto.hash.Digest32

import java.math.BigInteger
import java.io.PrintWriter

object Utils {
  private val secureRandom = new java.security.SecureRandom


  /**
   * Create json of TokenMap
   * @param networkVersion String ex: 1.0.0
   * @param networkType String ex: public-launch or pandora
   */
  def createTokenMap(networkVersion: String, networkType: String = ""): Unit = {
    val generalConfig = Configs.generalConfig
    generalConfig.keys.toSeq.foreach(conf => {
      if ((conf contains networkType) || networkType.isEmpty){
        val fileType = conf ++ ".json"
        val tokensMap = TokensMap.readTokensFromFiles(Configs.tokensMapDirPath, List(fileType))
        val tokensMapJson = tokensMap.deepMerge(
          Json.fromFields(
            List(
              ("version", Json.fromString(networkVersion))
            )
          )
        ).toString()
        TokensMap.createTokensMapJsonFile(tokensMapJson, conf, networkVersion)
        println(s"Json of TokensMap created for network type $conf!")
      }
    })
  }

  /**
   * Create json of contracts
   * @param networkVersion String ex: 1.0.0
   * @param networkType String ex: public-launch or pandora
   */
  def createContracts(networkVersion: String, networkType: String = ""): Unit = {
    if (networkType.isEmpty) {
      println("Creating contracts for ALL network types...")
      createContracts(networkVersion,"public-launch")
      createContracts(networkVersion, "pandora")
      return
    }
    
    def writeJsonToFile(filename: String, json: Json): Unit = {
      val writer = new PrintWriter(filename)
      try {
        writer.write(json.spaces2)
      } finally {
        writer.close()
      }
    }
        
    val generalConfig = Configs.generalConfig
    val allNetworks = Configs.allNetworksToken
    
    val chainsForType = allNetworks.collect {
      case ((chainName, chainType), netCfg) 
        if chainType == networkType => (chainName, chainType, netCfg)
    }.toList
    
    if (chainsForType.isEmpty) {
      println(s"No networks found for type: $networkType")
      return
    }
    
    val networkTypeKey = networkType
    val generalTokens = generalConfig(networkTypeKey).mainTokens
    
    val chainEntries = chainsForType.map { case (chainName, chainType, netCfg) =>
      val general = generalConfig(chainType)
      val contracts = new Contracts(general, netCfg)
      
      chainName -> Json.fromFields(List(
        ("addresses", contracts.toJsonAddresses(chainName)),
        ("tokens", netCfg.tokens.toJson()),
        ("cleanupConfirm", Json.fromInt(netCfg.cleanupConfirm))
      ))
    }
    
    
    val finalJson = Json.fromFields(
      List(
        ("version", Json.fromString(networkVersion)),
        ("tokens", generalTokens.toJson())
      ) ++ chainEntries
    )
    
    val outputName = s"contracts-$networkType-$networkVersion.json"
    writeJsonToFile(outputName, finalJson)
    
    println(s"Merged contract file created: $outputName")
    println(s"Contains ${chainsForType.size} chains: ${chainsForType.map(_._1).mkString(", ")}")
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