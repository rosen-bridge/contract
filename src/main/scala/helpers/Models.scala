package helpers

import io.circe.Json
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ErgoClient

case class MainTokens(RWTRepoNFT: String, GuardNFT: String, RSN: String, MinFeeNFT: String, EmissionNFT: String, ERSN: String) {
  def toJson(): Json = {
    Json.fromFields(List(
      ("RWTRepoNFT", Json.fromString(RWTRepoNFT)),
      ("GuardNFT", Json.fromString(GuardNFT)),
      ("RSN", Json.fromString(RSN)),
      ("MinFeeNFT", Json.fromString(MinFeeNFT)),
      ("EmissionNFT", Json.fromString(EmissionNFT)),
      ("ERSN", Json.fromString(ERSN)),
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

case class Network(tokens: Tokens, lockAddress: String, coldAddress: String, cleanupConfirm: Int)

case class NetworkGeneral(ergoNetwork: ErgoNetwork, mainTokens: MainTokens)
