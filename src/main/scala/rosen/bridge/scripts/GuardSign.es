{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] = [GUARD_PKS]
  // R5: Coll[Int] = [Payment Required Signs, Update Required Signs]
  // ----------------- TOKENS
  // 0: GuardNFT
  // --------------------
  // [GuardSign (sign atleast update required sign on it)] => [GuardSign]
  val GuardBox = INPUTS(0);
  val signedColl = SELF.R4[Coll[Coll[Byte]]].get.map { (row: Coll[Byte]) => proveDlog(decodePoint(row)) }
  val updateSignCount = SELF.R5[Coll[Int]].get(1)
  val updateSignLeast = atLeast(updateSignCount, signedColl)
  sigmaProp(
    allOf(
      Coll(
        SELF.propositionBytes == GuardBox.propositionBytes,
        OUTPUTS(0).propositionBytes == SELF.propositionBytes,
        OUTPUTS(0).tokens(0)._1 == SELF.tokens(0)._1,
        updateSignLeast
      )
    )
  )
}
