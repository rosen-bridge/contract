package helpers

import com.typesafe.config.{Config, ConfigFactory, ConfigObject}
import io.circe.Json
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ErgoClient, NetworkType, RestApiErgoClient}

import scala.collection.mutable

trait ConfigHelper {
  val config: Config  = ConfigFactory.load()

  /**
   * Read the config and return the value of the key
   *
   * @param key     key to find
   * @param default default value if the key is not found
   * @return value of the key
   */
  def readKey(key: String, default: String = null): String = {
    try {
      config.getString(key)
    } catch {
      case _: Throwable =>
        println(s"$key is required.")
        sys.exit()
    }
  }

  def readKeyDynamic(config: ConfigObject, key: String, default: String = null): String = {
    try {
      if(config.containsKey(key)) config.toConfig.getString(key)
      else if(default.nonEmpty) default
      else throw new Throwable(s"$key not found!")
    } catch {
      case ex: Throwable =>
        println(ex.getMessage)
        null
    }
  }
}


object Configs extends ConfigHelper {

  lazy val fee: Long = readKey("fee.default", "1000000").toLong
  lazy val maxFee: Long = readKey("fee.max", "1000000").toLong
  lazy val minBoxValue: Long = readKey("box.min").toLong

  private lazy val ergoNetworksConfig = config.getObject("networks")
  // (String, String) => (networkName, networkType)
  var allNetworksToken = mutable.Map.empty[(String, String), Network]
  // networkName => Json of idKey
  var allNetworksIdKey = mutable.Map.empty[String, Json]
  ergoNetworksConfig.keySet().forEach(networkName => {
    val networkConfig = ergoNetworksConfig.get(networkName).asInstanceOf[ConfigObject]
    allNetworksIdKey(networkName) = Json.fromString(readKeyDynamic(networkConfig, "idKey"))
    networkConfig.keySet().toArray.toSeq.filterNot(obj => obj == "idKey").foreach(networkType => {
      val networkDataConfig = networkConfig.get(networkType.toString).asInstanceOf[ConfigObject]
      val networkTokensConfig = networkDataConfig.get("tokens").asInstanceOf[ConfigObject]
      val tokens = Tokens(
        readKeyDynamic(networkTokensConfig, "CleanupNFT"),
        readKeyDynamic(networkTokensConfig, "RWTId")
      )
      val lockAddress = if (networkName != "ergo") readKeyDynamic(networkDataConfig, "lock-address", "PLEASE SET LOCK_ADDRESS MANUALLY") else ""
      allNetworksToken((networkName, networkType.toString)) =  Network(
        tokens,
        lockAddress,
        readKeyDynamic(networkDataConfig, "cleanup-confirm").toInt
      )
    })
  })

  private lazy val networkGeneralConfig = config.getObject("network-general")
  var generalConfig = mutable.Map.empty[String, (ErgoNetwork, MainTokens)]
  networkGeneralConfig.keySet().forEach(networkTypeName => {
    val mainNetworkConfig = networkGeneralConfig.get(networkTypeName).asInstanceOf[ConfigObject]
    val mainTokensConfig = mainNetworkConfig.get("main-tokens").asInstanceOf[ConfigObject]
    val ergNetworkConfig = mainNetworkConfig.get("ergo-network").asInstanceOf[ConfigObject]

    // Prepare general tokens
    val mainTokens = MainTokens(
      readKeyDynamic(mainTokensConfig, "RepoNFT"),
      readKeyDynamic(mainTokensConfig, "GuardNFT"),
      readKeyDynamic(mainTokensConfig, "RSN"),
      readKeyDynamic(mainTokensConfig, "RSNRatioNFT")
    )

    // Prepare general network config
    val node = readKeyDynamic(ergNetworkConfig, "node")
    val networkType: NetworkType = if (readKeyDynamic(ergNetworkConfig, "type").toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
    val explorerUrlConf = readKeyDynamic(ergNetworkConfig, "explorer-url")
    val explorer: String = if (explorerUrlConf.isEmpty) RestApiErgoClient.getDefaultExplorerUrl(networkType) else explorerUrlConf
    val ergoClient: ErgoClient = RestApiErgoClient.create(node, networkType, "", explorer)
    val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
    val ergoNetwork = ErgoNetwork(ergoClient, addressEncoder)

    generalConfig(networkTypeName) = (ergoNetwork, mainTokens)
  })

  lazy val tokensMapDirPath: String = readKey("tokensMap.dirPath", "./tokensMap")

}
