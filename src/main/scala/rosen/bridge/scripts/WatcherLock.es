{
  // ----------------- REGISTERS
  // R4: Coll[Byte] = Owner WID
  // ----------------- TOKENS
  // [repo, Permit, WIDToken, Lock] => [repo, Permit(Optional), WIDToken(+userChange)]
  val repoNFT = fromBase64("REPO_NFT");
  val repo = INPUTS(0);
  val repoOut = OUTPUTS(0);
  val widBox = INPUTS(2);
  val watcherIndex = repoOut.R7[Int].getOrElse(-1);
  val watcherWID = SELF.R4[Coll[Byte]].get;
  sigmaProp(
    allOf(
      Coll(
        SELF.id == INPUTS(3).id,                            // prevent spend multiple lock
        widBox.tokens(0)._1 == watcherWID,
        repo.R4[Coll[Coll[Byte]]].get(watcherIndex) == watcherWID,
        if(repoOut.R4[Coll[Coll[Byte]]].get.size > watcherIndex) {
          repoOut.R4[Coll[Coll[Byte]]].get(watcherIndex) != watcherWID
        }else {
            repoOut.R4[Coll[Coll[Byte]]].get.size <= watcherIndex
        },
        repo.tokens(0)._1 == repoNFT
      )
    )
  )
}
