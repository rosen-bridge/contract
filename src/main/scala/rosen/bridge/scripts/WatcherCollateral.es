{
  // ----------------- REGISTERS
  // R4: Coll[Byte] = Owner WID
  // R5: Long = locked RSN
  // ----------------- TOKENS
  // 0: X-AWC NFT
  // 0: RSN collateral

  val repoNFT = fromBase64("REPO_NFT");
  val repo = INPUTS(0);
  val repoOut = OUTPUTS(0);
  val WID = SELF.R4[Coll[Byte]].get;
  val transferedRwt = repoOut.tokens(1)._2 - repo.tokens(1)._2
  if(transferedRwt < SELF.R5[Long].get){
    val outCollateral = OUTPUTS(1)
    // [repo, Collateral, Permit(Optional), WIDToken] => [repo, Collateral, Permit(Optional), WIDToken]
    // [repo, Collateral, Fraud, Cleanup] => [repo, Collateral, Cleanup]
    sigmaProp(
      allOf(
        Coll(
          repo.tokens(0)._1 == repoNFT,
          repo.tokens(3)._1 == SELF.tokens(0)._1,
          outCollateral.value == SELF.value,
          outCollateral.tokens(0)._1 == SELF.tokens(0)._1,
          if (SELF.tokens.size > 1) {
            // RSN collateral check
            outCollateral.tokens(1)._1 == SELF.tokens(1)._1 &&
            outCollateral.tokens(1)._2 == SELF.tokens(1)._2
          } else {
            true
          },
          outCollateral.R4[Coll[Byte]].get == WID,
          outCollateral.R5[Long].get + transferedRwt == SELF.R5[Long].get,
          if(transferedRwt < 0) {
              // WID check in extend permit
              OUTPUTS(3).tokens(0)._1 == WID &&
              OUTPUTS(3).tokens(0)._2 >= 2
          } else {
            true
          }
        )
      )
    )
  }
  else {
    val widBox = INPUTS(3);
    // Compelete Return
    // [repo, Collateral, Permit, WIDBox] => [repo, WIDBox(+userChange)]
    sigmaProp(
      allOf(
        Coll(
          widBox.tokens(0)._1 == WID,
          widBox.tokens(0)._2 >= 2,
          repoOut.R5[Long].get + 1 == repo.R5[Long].get,
          repo.tokens(0)._1 == repoNFT,
          repo.tokens(3)._1 == SELF.tokens(0)._1,
        )
      )
    )
  }
}
