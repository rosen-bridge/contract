package helpers

case class MainTokens(RepoNFT: String, GuardNFT: String, RSN: String) {
  def toJson(): String = {
    s"""
       | {
       |    "RepoNFT": "${RepoNFT}",
       |    "GuardNFT": "${GuardNFT}",
       |    "RSN": "${RSN}"
       | }
       |""".stripMargin
  }
}

case class Tokens(CleanupNFT: String, RWTId: String, cleanupConfirm: Int) {
  def toJson(): String = {
    s"""
       | {
       |    "CleanupNFT": "${CleanupNFT}",
       |    "RWTId": "${RWTId}",
       |    "cleanupConfirm": ${cleanupConfirm}
       | }
       |""".stripMargin
  }
}
