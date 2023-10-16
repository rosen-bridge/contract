{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] = [WID]
  // ----------------- TOKENS
  // 0: RWT

  val repoNFT = fromBase64("REPO_NFT");
  val cleanupNFT = fromBase64("CLEANUP_NFT");
  val outputWithToken = OUTPUTS.slice(1, OUTPUTS.size).filter { (box: Box) => box.tokens.size > 0 }
  val outputWithRWT = outputWithToken.exists { (box: Box) => box.tokens.exists { (token: (Coll[Byte], Long)) => token._1 == SELF.tokens(0)._1 } }
  // RSN Slash
  // [Repo, Fraud, Cleanup] => [Repo, Cleanup, Slashed]
  sigmaProp(
    allOf(
      Coll(
        outputWithRWT == false,
        SELF.id == INPUTS(1).id,
        INPUTS(0).tokens(0)._1 == repoNFT,
        INPUTS(2).tokens(0)._1 == cleanupNFT,
      )
    )
  )
}
