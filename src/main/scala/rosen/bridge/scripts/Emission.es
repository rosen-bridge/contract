{
  // ----------------- REGISTERS
  // ----------------- TOKENS
  // 0: RSN
  // 1: EmittedRSN
  
  val GuardNFT = fromBase64("GUARD_NFT");

  if (CONTEXT.dataInputs.size > 0) {
    // Emission Box Update transaction
    val GuardBox = CONTEXT.dataInputs(0);
    val verifyGuard = GuardBox.tokens.exists { (token: (Coll[Byte], Long)) => token._1 == GuardNFT };
    val updateSignCount = GuardBox.R5[Coll[Int]].get(1);
    val signedColl = GuardBox.R4[Coll[Coll[Byte]]].get.map { (row: Coll[Byte]) => proveDlog(decodePoint(row)) };
    sigmaProp(
      allOf(
        Coll(
          verifyGuard,
          atLeast(updateSignCount, signedColl)
        )
      )
    )
  } else {
    // Emission transaction
    val emissionOut = OUTPUTS(0)
    val emission = SELF
    sigmaProp(
      allOf(
        Coll(
          emissionOut.propositionBytes == emission.propositionBytes,
          emissionOut.value >= emission.value,
          emissionOut.tokens.size == emission.tokens.size,
          emission.tokens.size >= 2,
          emissionOut.tokens(0)._1 == emission.tokens(0)._1,
          emissionOut.tokens(1)._1 == emission.tokens(1)._1,
          emissionOut.tokens(1)._2 > emission.tokens(1)._2,
          emission.tokens(0)._2 - emissionOut.tokens(0)._2 == emissionOut.tokens(1)._2 - emission.tokens(1)._2
        )
      )
    )
  }
}
