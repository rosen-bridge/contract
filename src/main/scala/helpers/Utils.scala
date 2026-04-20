package helpers

import io.circe.Json
import io.circe.parser.parse
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoContract
import rosen.bridge.{Contracts, TokensMap}
import scorex.crypto.hash.Digest32

import java.io.{File, PrintWriter}
import java.math.BigInteger
import scala.io.Source

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

  def buildTypeScriptPackage(
    networkType: String,
    version: String,
    tag: String,
  ): Unit = {
    println(s"Building TypeScript package for network type: $networkType")
    println(s"Version: $version, Tag: $tag")
    
    val contractsFile = s"contracts-$networkType-$version.json"
    val tokensFile = s"tokensMap-$networkType-$version.json"
    
    if (!new File(contractsFile).exists()) {
      println(s"Error: $contractsFile not found. Please run 'all' command first.")
      System.exit(1)
    }
    
    val contractsJsonString = readFileContent(contractsFile)
    val tokensJsonString = if (new File(tokensFile).exists()) readFileContent(tokensFile) else "{}"
    
    val contractsJson = parse(contractsJsonString).getOrElse(Json.Null)
    val tokensJson = parse(tokensJsonString).getOrElse(Json.Null)
    
    val packageDir = s"./ts-packages/${networkType}-contracts"
    val distDir = s"$packageDir/dist"
    
    new File(packageDir).mkdirs()
    new File(distDir).mkdirs()
    
    generatePackageJson(packageDir, s"${networkType}-contracts", version, tag)
    
    generateIndexJs(distDir, contractsJsonString, tokensJsonString)
    
    generateIndexDts(distDir, contractsJson, tokensJson)
    
    println(s"TypeScript package generated successfully at: $packageDir")
    println(s"To install: npm install $packageDir")
  }

  def readFileContent(filename: String): String = {
    val source = Source.fromFile(filename)
    val content = try source.mkString finally source.close()
    content
  }

  def generatePackageJson(dir: String, packageName: String, version: String, tag: String): Unit = {
    val packageJson = 
      s"""{
         |  "name": "$packageName",
         |  "version": "$version",
         |  "description": "TypeScript package with tag $tag for Rosen Bridge contracts and tokens",
         |  "main": "dist/index.js",
         |  "types": "dist/index.d.ts",
         |  "type": "module",
         |  "files": [
         |    "dist"
         |  ],
         |  "license": "MIT",
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

  def generateIndexJs(distDir: String, contractsJsonString: String, tokensJsonString: String): Unit = {
    val indexJs = 
      s"""
export const contracts = $contractsJsonString;

export const tokens = $tokensJsonString;
"""
    
    val writer = new PrintWriter(s"$distDir/index.js")
    writer.write(indexJs)
    writer.close()
  }

  def generateIndexDts(distDir: String, contractsJson: Json, tokensJson: Json): Unit = {
    val contractsType = generateInterface(contractsJson, "Contracts", 0)
    val tokensType = generateInterface(tokensJson, "Tokens", 0)
    
    val indexDts = 
      s"""
${contractsType}

${tokensType}

export const contracts: Contracts;
export const tokens: Tokens;

"""
    
    val writer = new PrintWriter(s"$distDir/index.d.ts")
    writer.write(indexDts)
    writer.close()
  }

  def generateInterface(json: Json, name: String, indentLevel: Int): String = {
    val indent = "  " * indentLevel
    val nextIndent = "  " * (indentLevel + 1)
    
    json.fold(
      jsonNull = s"${indent}export type $name = null;",
      jsonBoolean = b => s"${indent}export type $name = boolean;",
      jsonNumber = n => s"${indent}export type $name = number;",
      jsonString = s => s"${indent}export type $name = string;",
      jsonArray = arr => {
        if (arr.isEmpty) s"${indent}export type $name = any[];"
        else {
          val itemType = generateInterface(arr.head, s"${name}Item", indentLevel)
          s"${itemType}\n\n${indent}export type $name = ${name}Item[];"
        }
      },
      jsonObject = obj => {
        val fields = obj.toList.sortBy(_._1).map { case (key, value) =>
          val fieldType = generateInlineType(value, indentLevel + 2)
          s"""${nextIndent}"$key": $fieldType;"""
        }.mkString("\n")
        s"""${indent}export interface $name {
${fields}
${indent}}"""
      }
    )
  }

  def generateInlineType(json: Json, indentLevel: Int): String = {
    val indent = "  " * indentLevel
    
    json.fold(
      jsonNull = "null",
      jsonBoolean = b => "boolean",
      jsonNumber = n => "number",
      jsonString = s => "string",
      jsonArray = arr => {
        if (arr.isEmpty) "any[]"
        else {
          val itemType = generateInlineType(arr.head, indentLevel)
          s"${itemType}[]"
        }
      },
      jsonObject = obj => {
        if (obj.isEmpty) "{}"
        else {
          val fields = obj.toList.sortBy(_._1).map { case (key, value) =>
            val fieldType = generateInlineType(value, indentLevel + 1)
            s"""${indent}  "$key": $fieldType;"""
          }.mkString("\n")
          s"""{
${fields}
${indent}}"""
        }
      }
    )
  }
}
