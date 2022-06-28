package helpers

import io.circe.Json

case class MainTokens(RepoNFT: String, GuardNFT: String, RSN: String) {
  def toJson(): Json = {
    Json.fromFields(List(
      ("RepoNFT", Json.fromString(RepoNFT)),
      ("GuardNFT", Json.fromString(GuardNFT)),
      ("RSN", Json.fromString(RSN))
    ))
  }
}

case class Tokens(CleanupNFT: String, RWTId: String) {
  def toJson(): Json = {
    Json.fromFields(List(
      ("CleanupNFT", Json.fromString(CleanupNFT)),
      ("RWTId", Json.fromString(RWTId)),
    ))
  }
}

case class Network(tokens: Tokens, cleanupConfirm: Int)
