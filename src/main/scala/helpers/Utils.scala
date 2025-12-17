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
   * @param networkType String ex: mainnet-alpha-1
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
   * @param networkType String ex: mainnet-alpha-1
   */
  def createContracts(networkVersion: String, networkType: String = ""): Unit = {

    val generalConfig = Configs.generalConfig
    val allNetworks   = Configs.allNetworksToken

    val networkTypes: Seq[String] =
      if (networkType.nonEmpty)
        Seq(networkType)
      else
        generalConfig.keys.toSeq

    networkTypes.foreach { nt =>
      val chainsForType = allNetworks.collect {
        case ((chainName, chainType), netCfg) if chainType == nt =>
          (chainName, netCfg)
      }.toList

      if (chainsForType.isEmpty) {
        println(s"No networks found for type: $nt")
      } else {

        val generalTokens = generalConfig(nt).mainTokens

        val chainEntries = chainsForType.map { case (chainName, netCfg) =>
        val contracts = new Contracts(generalConfig(nt), netCfg)
        chainName -> contracts.buildContractsJson(chainName, networkVersion)
      }

        val finalJson = Json.fromFields(
          List(
            "version" -> Json.fromString(networkVersion),
            "tokens" -> generalTokens.toJson()
          ) ++ chainEntries
        )

        val outputName = s"contracts-$nt-$networkVersion.json"
        val writer = new PrintWriter(outputName)
        try writer.write(finalJson.spaces2)
        finally writer.close()

        println(s"Contracts json created for network type: $nt")
      }
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
