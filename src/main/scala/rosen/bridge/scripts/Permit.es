{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] = [WID]
  // ----------------- TOKENS
  // 0: X-RWT

  val repoNFT = fromBase64("REPO_NFT");
  val commitmentScriptHash = fromBase64("COMMITMENT_SCRIPT_HASH");
  val WID = SELF.R4[Coll[Coll[Byte]]].get
  val outputWithToken = OUTPUTS.slice(2, OUTPUTS.size).filter { (box: Box) => box.tokens.size > 0 }
  val outputWithRWT = outputWithToken.exists { (box: Box) => box.tokens.exists { (token: (Coll[Byte], Long)) => token._1 == SELF.tokens(0)._1 } }
  val secondBoxHasRWT = OUTPUTS(1).tokens.exists { (token: (Coll[Byte], Long)) => token._1 == SELF.tokens(0)._1 }
  if(OUTPUTS(0).tokens(0)._1 == repoNFT){
    // Updating Permit (Return or receive more tokens)
    // [Repo, Permit(SELF), WID] => [Repo, Permit(optional), WID(+userChange)]
    val outputPermitCheck = if(secondBoxHasRWT){
      allOf(
        Coll(
          OUTPUTS(1).tokens(0)._1 == SELF.tokens(0)._1,
          OUTPUTS(1).propositionBytes == SELF.propositionBytes,
          SELF.R4[Coll[Coll[Byte]]].get == OUTPUTS(1).R4[Coll[Coll[Byte]]].get
        )
      )
    }else{
      true
    }
    sigmaProp(
      allOf(
        Coll(
          outputWithRWT == false,
          INPUTS(2).tokens(0)._1 == WID(0),
          outputPermitCheck,
        )
      )
    )
  }else{
    // Event Commitment Creation
    // [Permit(s), WID] => [Permit, Commitment, WID]
    val totalPermits = INPUTS.filter{(box:Box)
       => box.tokens(0)._1 == SELF.tokens(0)._1
       }
       .map{(box:Box) => box.tokens(0)._2}
       .fold(0L, { (a: Long, b: Long) => a + b })
    sigmaProp(
      allOf(
        Coll(
          OUTPUTS(0).tokens(0)._1 == SELF.tokens(0)._1,
          OUTPUTS(1).tokens(0)._2 == totalPermits - OUTPUTS(0).tokens(0)._2,
          OUTPUTS(1).tokens(0)._1 == SELF.tokens(0)._1,
          blake2b256(OUTPUTS(1).propositionBytes) == commitmentScriptHash,
          OUTPUTS(1).R5[Coll[Coll[Byte]]].isDefined,
          OUTPUTS(1).R6[Coll[Byte]].isDefined,
          OUTPUTS(1).R7[Coll[Byte]].get == blake2b256(SELF.propositionBytes),
          OUTPUTS(1).R4[Coll[Coll[Byte]]].get == WID,
          outputWithRWT == false,
          OUTPUTS(0).propositionBytes == SELF.propositionBytes,
          OUTPUTS(0).R4[Coll[Coll[Byte]]].get == WID,
          OUTPUTS(2).tokens(0)._1 == WID(0),
        )
      )
    )
  }
}
