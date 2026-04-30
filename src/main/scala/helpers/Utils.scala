package helpers

import io.circe.Json
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoContract
import rosen.bridge.{Contracts, TokensMap}
import scorex.crypto.hash.Digest32

import java.io.{File, PrintWriter}
import java.math.BigInteger

object Utils {
  private val secureRandom = new java.security.SecureRandom

  /**
   * Create json of TokenMap
   * @param networkVersion String ex: 1.0.0
   * @param networkType String ex: mainnet-alpha-1
   * @param saved Boolean default true, if true the json file will be saved
   * @return Json of TokenMap
    */  
  def createTokenMap(networkVersion: String, networkType: String = "", saved: Boolean = true): Json = {
    val generalConfig = Configs.generalConfig
    var resultJson: Json = Json.Null
    
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
        )
        resultJson = tokensMapJson
        if (saved) {
          TokensMap.createTokensMapJsonFile(tokensMapJson.spaces2, conf, networkVersion)
        }
        println(s"Json of TokensMap created for network type $conf!")
      }
    })
    resultJson
  }

  /**
   * Create json of contracts
   * @param networkVersion String ex: 1.0.0
   * @param networkType String ex: mainnet-alpha-1
   * @param saved Boolean default true, if true the json file will be saved
   * @return Json of contracts
   */
  def createContracts(networkVersion: String, networkType: String = "", saved: Boolean = true): Json = {
    val generalConfig = Configs.generalConfig
    val allNetworks   = Configs.allNetworksToken
    var resultJson    = Json.Null

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

        val chainEntries = chainsForType.map {
          case (chainName, netCfg) =>
            val contracts = new Contracts(generalConfig(nt), netCfg)
            chainName -> contracts.buildContractsJson(chainName)
        }

        val finalJson = Json.fromFields(
          List(
            "version" -> Json.fromString(networkVersion),
            "tokens" -> generalTokens.toJson()
          ) ++ chainEntries
        )
        resultJson = finalJson
        
        if (saved) {
          val outputName = s"contracts-$nt-$networkVersion.json"
          val writer = new PrintWriter(outputName)
          try writer.write(finalJson.spaces2)
          finally writer.close()
        }

        println(s"Contracts json created for network type: $nt")
      }
    }
    resultJson
  }

  def randBigInt: BigInt = new BigInteger(256, secureRandom)

  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  def getContractAddress(contract: ErgoContract, addressEncoder: ErgoAddressEncoder): String = {
    val ergoTree = contract.getErgoTree
    addressEncoder.fromProposition(ergoTree).get.toString
  }

  def generateAllResources(
    version: String,
    networkType: String = "",
  ): Unit = {
    val networkTypes: Seq[String] =
      if (networkType.nonEmpty)
        Seq(networkType)
      else
        Configs.generalConfig.keys.toSeq
    
    networkTypes.foreach(nt =>
      { 
        println(s"Generating resources for network type: $nt")
        val contractsJson = createContracts(version, nt, saved = true)
        val tokensJson = createTokenMap(version, nt, saved = true)
        
        generateTypeScriptPackage(
          networkType = nt,
          version = version,
          contractsJson = contractsJson,
          tokensJson = tokensJson
        )
      }
    ) 
  }

  def generateTypeScriptPackage (
    networkType: String,
    version: String,
    contractsJson: Json,
    tokensJson: Json,
  ): Unit = {
    println(s"Generating TypeScript package for network type: $networkType with Version: $version")
    
    val packageName = s"@rosen-bridge/contract"
    val packageDir = s"./ts-packages/contract-$networkType"
    val distDir = s"$packageDir/dist"
    
    new File(packageDir).mkdirs()
    new File(distDir).mkdirs()
    
    writeFile(s"$packageDir/package.json", renderPackageJson(packageName, version, networkType))
    writeFile(s"$packageDir/README.md", generateReadmeContent())
    writeFile(s"$distDir/contracts.js", renderContractsJs(contractsJson))
    writeFile(s"$distDir/tokens.js", renderTokensJs(tokensJson))
    writeFile(s"$distDir/contracts.d.ts", renderContractsDts(contractsJson))
    writeFile(s"$distDir/tokens.d.ts", renderTokensDts())
    writeFile(s"$distDir/index.js", Templates.indexJsTemplate)
    writeFile(s"$distDir/index.d.ts", Templates.indexDtsTemplate)
    
    println(s"TypeScript package generated successfully at: $packageDir")
  }

  def writeFile(path: String, content: String): Unit = {
    val writer = new PrintWriter(path)
    writer.write(content)
    writer.close()
  }

  def renderPackageJson(packageName: String, version: String, networkType: String): String = {
    Templates.packageJsonTemplate
      .replace("$packageName", packageName)
      .replace("$version", version)
      .replace("$networkType", networkType)
  }

  def renderContractsJs(contractsJson: Json): String = {
    val chains = extractChainNames(contractsJson)
    val networkChainsArray = chains.map(c => s""""$c"""").mkString("[", ", ", "]")
    Templates.contractsJsTemplate
    .replace("$contractsJson", contractsJson.spaces2)
    .replace("$networkChainsArray", networkChainsArray)
  }

  def renderTokensJs(tokensJson: Json): String = {
    Templates.tokensJsTemplate.replace("$tokensJson", tokensJson.spaces2)
  }

  def renderContractsDts(contractsJson: Json): String = {
    val chains = extractChainNames(contractsJson)
    val globalTokens = extractKeys(contractsJson, "tokens")
    val addresses = extractNestedKeys(contractsJson, chains.headOption, "addresses")
    val chainTokens = extractNestedKeys(contractsJson, chains.headOption, "tokens")

    val globalTokensStr = globalTokens.map(k => s"""  "$k": string;""").mkString("\n")
    val addressesStr = addresses.map(k => s"""  "$k": string;""").mkString("\n")
    val chainTokensStr = chainTokens.map(k => s"""  "$k": string;""").mkString("\n")
    val networkUnion = chains.map(c => s"""  | "$c"""").mkString("\n")

    Templates.contractsDtsTemplate
      .replace("$globalTokens", globalTokensStr)
      .replace("$addresses", addressesStr)
      .replace("$chainTokens", chainTokensStr)
      .replace("$networkUnion", networkUnion)
  }

  def renderTokensDts(): String = {
    Templates.tokensDtsTemplate
  }

  def extractChainNames(json: Json): List[String] =
    json.hcursor.keys.map(_.filterNot(k => k == "version" || k == "tokens").toList).getOrElse(Nil)

  def extractKeys(json: Json, field: String): List[String] =
    json.hcursor.downField(field).keys.map(_.toList).getOrElse(Nil)

  def extractNestedKeys(json: Json, chainOpt: Option[String], field: String): List[String] =
    chainOpt.flatMap { chain =>
      json.hcursor.downField(chain).downField(field).keys.map(_.toList)
    }.getOrElse(Nil)

  def generateReadmeContent(): String = 
    Templates.readmeTemplate
}
