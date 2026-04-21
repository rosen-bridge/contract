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

  def buildTypeScriptPackage(
    networkType: String,
    version: String,
    tag: String,
  ): Unit = {
    println(s"Building TypeScript package for network type: $networkType")
    println(s"Version: $version, Tag: $tag")
    
    val contractsJson = createContracts(version, networkType, saved = false)
    val tokensJson = createTokenMap(version, networkType, saved = false)

    val packageName = s"@rosen-bridge/contract"
    val packageDir = s"./ts-package/contract"
    val distDir = s"$packageDir/dist"
    
    new File(packageDir).mkdirs()
    new File(distDir).mkdirs()
    
    generatePackageJson(packageDir, packageName, version, tag, networkType)
    
    generateContractsJs(distDir, contractsJson)
    generateTokensJs(distDir, tokensJson)
    
    generateContractsDts(distDir, contractsJson)
    generateTokensDts(distDir, tokensJson)
    
    generateIndexJs(distDir)
    generateIndexDts(distDir)
    
    println(s"TypeScript package generated successfully at: $packageDir")
    println(s"To install: npm install $packageName")
  }

  def generatePackageJson(dir: String, packageName: String, version: String, tag: String, networkType: String): Unit = {
    val packageJson = 
      s"""{
         |  "name": "$packageName",
         |  "version": "$version-$tag",
         |  "description": "TypeScript package for Rosen Bridge $networkType contracts and tokens",
         |  "repository": {
         |     "type": "git",
         |     "url": "git+https://github.com/rosen-bridge/contract.git"
         |   },
         |  "license": "MIT",
         |  "author": "Rosen Team",
         |  "types": "dist/index.d.ts",
         |  "main": "dist/index.js",
         |  "type": "module",
         |  "files": [
         |    "dist"
         |  ],
         |  "engines": {
         |    "node": ">=22.18.0",
         |    "npm": "11.6.2"
         |  }
         |}
         |""".stripMargin
    
    val writer = new PrintWriter(s"$dir/package.json")
    writer.write(packageJson)
    writer.close()
  }

  def generateContractsJs(distDir: String, contractsJson: Json): Unit = {
    val contractsJs = s"""export const contracts = ${contractsJson.spaces2};\n"""
    new PrintWriter(s"$distDir/contracts.js") { write(contractsJs); close() }
  }

  def generateTokensJs(distDir: String, tokensJson: Json): Unit = {
    val tokensJs = s"""export const tokens = ${tokensJson.spaces2};\n"""
    new PrintWriter(s"$distDir/tokens.js") { write(tokensJs); close() }
  }

  def generateContractsDts(distDir: String, json: Json): Unit = {
    val chains = extractChainNames(json)
    val globalTokens = extractKeys(json, "tokens")
    val addresses = extractNestedKeys(json, chains.headOption, "addresses")
    val chainTokens = extractNestedKeys(json, chains.headOption, "tokens")

    val content =
      s"""
export interface GlobalTokens {
${globalTokens.map(k => s"""  "$k": string;""").mkString("\n")}
}

export interface ChainAddresses {
${addresses.map(k => s"""  "$k": string;""").mkString("\n")}
}

export interface ChainTokens {
${chainTokens.map(k => s"""  "$k": string;""").mkString("\n")}
}

export interface ChainConfig {
  "addresses": ChainAddresses;
  "tokens": ChainTokens;
  "cleanupConfirm": number;
}

export interface Contracts {
  "version": string;
  "tokens": GlobalTokens;
${chains.map(c => s"""  "$c": ChainConfig;""").mkString("\n")}
}

export declare const contracts: Contracts;
"""
    new PrintWriter(s"$distDir/contracts.d.ts") { write(content.trim()); close() }
  }

  def generateTokensDts(distDir: String, json: Json): Unit = {
    val networks = extractAllTokenNetworks(json)

    val content =
      s"""
export interface TokenInfo {
  "tokenId": string;
  "name": string;
  "decimals": number;
  "type": string;
  "residency": string;
  "extra"?: Record<string, any>;
}

export interface Tokens {
  "version": string;
  "tokens": {
${networks.map(n => s"""    "$n"?: TokenInfo;""").mkString("\n")}
  }[];
}

export declare const tokens: Tokens;
"""
    new PrintWriter(s"$distDir/tokens.d.ts") { write(content.trim()); close() }
  }

  def generateIndexJs(distDir: String): Unit =
    new PrintWriter(s"$distDir/index.js") {
      write("export { contracts } from './contracts.js';\nexport { tokens } from './tokens.js';\n")
      close()
    }

  def generateIndexDts(distDir: String): Unit =
    new PrintWriter(s"$distDir/index.d.ts") {
      write(
        """export type { Contracts } from './contracts.d.ts';
export type { Tokens } from './tokens.d.ts';
export { contracts } from './contracts.js';
export { tokens } from './tokens.js';
"""
      )
      close()
    }

  def extractChainNames(json: Json): List[String] =
    json.hcursor.keys.map(_.filterNot(k => k == "version" || k == "tokens").toList).getOrElse(Nil)

  def extractKeys(json: Json, field: String): List[String] =
    json.hcursor.downField(field).keys.map(_.toList).getOrElse(Nil)

  def extractNestedKeys(json: Json, chainOpt: Option[String], field: String): List[String] =
    chainOpt.flatMap { chain =>
      json.hcursor.downField(chain).downField(field).keys.map(_.toList)
    }.getOrElse(Nil)

  def extractAllTokenNetworks(json: Json): List[String] = {
    val allNetworks = scala.collection.mutable.Set[String]()
    
    json.hcursor
      .downField("tokens")
      .focus
      .flatMap(_.asArray)
      .foreach { array =>
        array.foreach { tokenObj =>
          tokenObj.asObject.foreach { obj =>
            obj.keys.foreach(allNetworks.add)
          }
        }
      }
    
    allNetworks.toList.sorted
  }
}
