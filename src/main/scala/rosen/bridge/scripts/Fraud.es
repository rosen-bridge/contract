{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] = [WID]
  // ----------------- TOKENS
  // 0: RWT

  val repoNFT = fromBase64("REPO_NFT");
  val cleanupNFT = fromBase64("CLEANUP_NFT");
  val transferedRwt = OUTPUTS(0).tokens(1)._2 - INPUTS(0).tokens(1)._2
  // RSN Slash
  // [Repo, Collateral, Fraud, Cleanup] => [Repo, Collateral, Cleanup, Slashed]
  sigmaProp(
    allOf(
      Coll(
        SELF.tokens(0)._2 == transferedRwt,
        SELF.id == INPUTS(2).id,
        INPUTS(0).tokens(0)._1 == repoNFT,
        INPUTS(3).tokens(0)._1 == cleanupNFT,
      )
    )
  )
}
