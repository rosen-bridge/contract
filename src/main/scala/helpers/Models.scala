package helpers

import io.circe.Json
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoClient

case class MainTokens(RepoNFT: String, GuardNFT: String, RSN: String, RSNRatioNFT: String) {
  def toJson(): Json = {
    Json.fromFields(List(
      ("RepoNFT", Json.fromString(RepoNFT)),
      ("GuardNFT", Json.fromString(GuardNFT)),
      ("RSN", Json.fromString(RSN)),
      ("RSNRatioNFT", Json.fromString(RSNRatioNFT))
    ))
  }
}

case class ErgoNetwork(ergoClient: ErgoClient, addressEncoder: ErgoAddressEncoder)

case class Tokens(CleanupNFT: String, RWTId: String, AwcNFT: String, RepoConfigNFT: String) {
  def toJson(): Json = {
    Json.fromFields(List(
      ("CleanupNFT", Json.fromString(CleanupNFT)),
      ("RWTId", Json.fromString(RWTId)),
      ("AwcNFT", Json.fromString(AwcNFT)),
      ("RepoConfigNFT", Json.fromString(RepoConfigNFT)),
    ))
  }
}

case class Network(tokens: Tokens, lockAddress: String, cleanupConfirm: Int)

case class NetworkGeneral(ergoNetwork: ErgoNetwork, mainTokens: MainTokens, minimumFeeAddress: String)
