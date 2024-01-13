{
  // ----------------- REGISTERS
  // R4: Coll[Byte] = WID
  // ----------------- TOKENS
  // 0: X-RWT

  val repoNFT = fromBase64("REPO_NFT");
  val commitmentScriptHash = fromBase64("COMMITMENT_SCRIPT_HASH");
  val WID = SELF.R4[Coll[Byte]].get
  val inputPermits = INPUTS.filter{
    (box:Box) => 
      box.tokens.size > 0 &&
      box.tokens(0)._1 == SELF.tokens(0)._1 &&
      box.propositionBytes == SELF.propositionBytes
    }
    .map{(box:Box) => box.tokens(0)._2}
    .fold(0L, { (a: Long, b: Long) => a + b })
  val hasOuputPermit = OUTPUTS(2).tokens.size > 0 && OUTPUTS(2).tokens(0)._1 == SELF.tokens(0)._1
  if(OUTPUTS(0).tokens(0)._1 == repoNFT){
    // Updating Permit (Return or receive more tokens)
    // [Repo, Collateral, Permit(SELF), WID] => [Repo, Collateral, Permit(optional), WID(+userChange)]
    val transferedRwt = OUTPUTS(0).tokens(1)._2 - INPUTS(0).tokens(1)._2
    val outputPermitCheck = if(hasOuputPermit){
      allOf(
        Coll(
          inputPermits - transferedRwt == OUTPUTS(2).tokens(0)._2,
          OUTPUTS(2).tokens(0)._1 == SELF.tokens(0)._1,
          OUTPUTS(2).propositionBytes == SELF.propositionBytes,
          SELF.R4[Coll[Byte]].get == OUTPUTS(2).R4[Coll[Byte]].get
        )
      )
    }else{
      inputPermits == transferedRwt
    }
    sigmaProp(
      allOf(
        Coll(
          INPUTS(3).tokens(0)._1 == WID,
          INPUTS(3).tokens(0)._2 >= 2,
          outputPermitCheck,
        )
      )
    )
  }else{
    // Event Commitment Creation
    // [Permit(s), WID] => [Permit, Commitment, WID]
    sigmaProp(
      allOf(
        Coll(
          OUTPUTS(0).tokens(0)._1 == SELF.tokens(0)._1,
          OUTPUTS(1).tokens(0)._2 == inputPermits - OUTPUTS(0).tokens(0)._2,
          OUTPUTS(1).tokens(0)._1 == SELF.tokens(0)._1,
          blake2b256(OUTPUTS(1).propositionBytes) == commitmentScriptHash,
          OUTPUTS(1).R5[Coll[Byte]].isDefined,
          OUTPUTS(1).R6[Coll[Byte]].isDefined,
          OUTPUTS(1).R7[Coll[Byte]].get == blake2b256(SELF.propositionBytes),
          OUTPUTS(1).R4[Coll[Byte]].get == WID,
          OUTPUTS(0).propositionBytes == SELF.propositionBytes,
          OUTPUTS(0).R4[Coll[Byte]].get == WID,
          OUTPUTS(2).tokens(0)._1 == WID,
        )
      )
    )
  }
}
