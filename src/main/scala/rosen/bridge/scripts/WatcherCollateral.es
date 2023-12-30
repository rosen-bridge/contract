{
  // ----------------- REGISTERS
  // R4: Coll[Byte] Owner WID
  // ----------------- TOKENS
  // 0: AWC NFT
  // 0: RSN collateral

  // [repo, Permit, WIDToken, Collateral] => [repo, WIDToken(+userChange)]
  val repoNFT = fromBase64("REPO_NFT");
  val repo = INPUTS(0);
  val repoOut = OUTPUTS(0);
  val widBox = INPUTS(2);
  val watcherIndex = repoOut.R7[Int].getOrElse(-1);
  val watcherWID = SELF.R4[Coll[Byte]].get;
  sigmaProp(
    allOf(
      Coll(
        widBox.tokens(0)._1 == watcherWID,
        widBox.tokens(0)._2 >= 2,
        repo.R4[Coll[Coll[Byte]]].get(watcherIndex) == watcherWID,
        repoOut.R4[Coll[Coll[Byte]]].get.size == watcherIndex || repoOut.R4[Coll[Coll[Byte]]].get(watcherIndex) != watcherWID,
        repo.tokens(0)._1 == repoNFT
      )
    )
  )
}
