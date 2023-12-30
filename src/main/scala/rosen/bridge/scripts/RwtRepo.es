{
  // ----------------- REGISTERS
  // R4: Coll[Coll[Byte]] = [Chain id, WID_0, WID_1, ...] (Stores Chain id and related watcher ids)
  // R5: Coll[Long] = [0, X-RWT_0, X-RWT_1, ...] (The first element is zero and the rest indicates X-RWT count for watcher i)
  // R6: Coll[Long] = [Commitment RWT count, Watcher quorum percentage, minimum needed approval, maximum needed approval, Collateral Erg amount, Collateral Rsn Amount]
  // (Minimum number of commitments needed for an event is: min(R6[3], R6[1] * (len(R4) - 1) / 100 + R6[2]) )
  // R7: Int = Watcher index (only used in returning or extending permits)
  // ----------------- TOKENS
  // 0: X-RWT Repo NFT
  // 1: X-RWT
  // 2: RSN

  val GuardNFT = fromBase64("GUARD_NFT");
  val watcherCollateralScriptHash = fromBase64("WATCHER_COLLATERAL_SCRIPT_HASH");
  if(OUTPUTS(0).tokens(0)._1 == GuardNFT){
    // RWT Repo Update transaction
    sigmaProp(true)
  } else {
    val permitScriptHash = fromBase64("PERMIT_SCRIPT_HASH");
    val repoOut = OUTPUTS(0)
    val repo = SELF
    val widListSize = repo.R5[Coll[Long]].get.size
    val widOutListSize = repoOut.R5[Coll[Long]].get.size
    val repoReplication = allOf(
      Coll(
        repoOut.propositionBytes == repo.propositionBytes,
        repoOut.R6[Coll[Long]].get == repo.R6[Coll[Long]].get,
        repoOut.tokens(0)._1 == repo.tokens(0)._1,
        repoOut.tokens(0)._2 == repo.tokens(0)._2,
        repoOut.tokens(1)._1 == repo.tokens(1)._1,
        repoOut.tokens(2)._1 == repo.tokens(2)._1,
        repoOut.R4[Coll[Coll[Byte]]].get.size == repoOut.R5[Coll[Long]].get.size,
      )
    )
    if(repo.tokens(1)._2 > repoOut.tokens(1)._2){
      // Getting Watcher Permit
      val WIDIndex = repoOut.R7[Int].getOrElse(-1)
      val permit = OUTPUTS(1)
      val outWIDBox = OUTPUTS(2)
      val RWTOut = repo.tokens(1)._2 - repoOut.tokens(1)._2
      val permitCreation = allOf(
        Coll(
          repoReplication,
          RWTOut == repoOut.tokens(2)._2 - repo.tokens(2)._2,
          permit.tokens(0)._2 == RWTOut,
          permit.tokens(0)._1 == SELF.tokens(1)._1,
          blake2b256(permit.propositionBytes) == permitScriptHash,
        )
      )
      if(WIDIndex == -1){
        // Getting initial permit
        // [Repo, UserInputs] => [Repo, watcherPermit, WIDBox, watcherCollateral]
        val watcherCollateral = OUTPUTS(3)
        sigmaProp(
          allOf(
            Coll(
              permitCreation,
              widOutListSize == widListSize + 1,
              repoOut.R4[Coll[Coll[Byte]]].get.slice(0, widOutListSize - 1) == repo.R4[Coll[Coll[Byte]]].get,
              repoOut.R4[Coll[Coll[Byte]]].get(widOutListSize - 1) == repo.id,
              repoOut.R5[Coll[Long]].get.slice(0, widOutListSize - 1) == repo.R5[Coll[Long]].get,
              repoOut.R5[Coll[Long]].get(widOutListSize - 1) == RWTOut,
              permit.R4[Coll[Coll[Byte]]].get == Coll(repo.id),
              outWIDBox.tokens(0)._1 == repo.id,
              outWIDBox.tokens(0)._2 >= 3,
              blake2b256(watcherCollateral.propositionBytes) == watcherCollateralScriptHash,
              watcherCollateral.R4[Coll[Byte]].get == repo.id,
              watcherCollateral.value >= repo.R6[Coll[Long]].get(4),
              if(repo.R6[Coll[Long]].get(5) > 0){
                allOf(
                  Coll(
                    watcherCollateral.tokens(0)._1 == repo.tokens(2)._1,
                    watcherCollateral.tokens(0)._2 >= repo.R6[Coll[Long]].get(5)
                  )
                )
              }else{
                true
              }
            )
          )
        )
      } else {
        // Extending Permit
        // [Repo, WIDBox] => [Repo, watcherPermit, WIDBox]
        val WID = repo.R4[Coll[Coll[Byte]]].get(WIDIndex)
        val currentRWT = repo.R5[Coll[Long]].get(WIDIndex)
        val WIDBox = INPUTS(1)
        sigmaProp(
          allOf(
            Coll(
              permitCreation,
              WID == WIDBox.tokens(0)._1,
              WIDBox.tokens(0)._2 >= 2,
              repoOut.R4[Coll[Coll[Byte]]].get == repo.R4[Coll[Coll[Byte]]].get,
              repoOut.R5[Coll[Long]].get(WIDIndex) == currentRWT + RWTOut,
              repoOut.R5[Coll[Long]].get.slice(0, WIDIndex) == repo.R5[Coll[Long]].get.slice(0, WIDIndex),
              repoOut.R5[Coll[Long]].get.slice(WIDIndex + 1, widOutListSize) == repo.R5[Coll[Long]].get.slice(WIDIndex + 1, widOutListSize),
              permit.R4[Coll[Coll[Byte]]].get == Coll(WID),
              outWIDBox.tokens(0)._1 == WID,
            )
          )
        )
      }
    }else{
      // Returning Watcher Permit
      val permit = INPUTS(1)
      val RWTIn = repoOut.tokens(1)._2 - repo.tokens(1)._2
      val WIDIndex = repoOut.R7[Int].get
      val WIDCheckInRepo = if(repo.R5[Coll[Long]].get(WIDIndex) > RWTIn) {
        // Returning some RWTs
        // [repo, Permit, WIDToken] => [repo, Permit(Optional), WIDToken(+userChange)]
        // [repo, Fraud, Cleanup] => [repo, Cleanup]
        allOf(
          Coll(
            repo.R5[Coll[Long]].get(WIDIndex) == repoOut.R5[Coll[Long]].get(WIDIndex) + RWTIn,
            repo.R4[Coll[Coll[Byte]]].get == repoOut.R4[Coll[Coll[Byte]]].get,
            repo.R5[Coll[Long]].get.slice(0, WIDIndex) == repoOut.R5[Coll[Long]].get.slice(0, WIDIndex),
            repo.R5[Coll[Long]].get.slice(WIDIndex + 1, widListSize) == repoOut.R5[Coll[Long]].get.slice(WIDIndex + 1, widListSize)
          )
        )
      }else{
        // Returning the permit
        // [repo, Permit, WIDToken, watcherCollateral] => [repo, WIDToken(+userChange)]
        val watcherCollateral = INPUTS(3);
        allOf(
          Coll(
            widOutListSize == widListSize - 1,
            repo.R5[Coll[Long]].get(WIDIndex) == RWTIn,
            repo.R4[Coll[Coll[Byte]]].get.slice(0, WIDIndex) == repoOut.R4[Coll[Coll[Byte]]].get.slice(0, WIDIndex),
            repo.R4[Coll[Coll[Byte]]].get.slice(WIDIndex + 1, widListSize) == repoOut.R4[Coll[Coll[Byte]]].get.slice(WIDIndex, widOutListSize),
            repo.R5[Coll[Long]].get.slice(0, WIDIndex) == repoOut.R5[Coll[Long]].get.slice(0, WIDIndex),
            repo.R5[Coll[Long]].get.slice(WIDIndex + 1, widListSize) == repoOut.R5[Coll[Long]].get.slice(WIDIndex, widOutListSize),
            blake2b256(watcherCollateral.propositionBytes) == watcherCollateralScriptHash,
          )
        )
      }
      val WID = repo.R4[Coll[Coll[Byte]]].get(WIDIndex)
      sigmaProp(
        allOf(
          Coll(
            repoReplication,
            Coll(WID) == permit.R4[Coll[Coll[Byte]]].get,
            RWTIn == repo.tokens(2)._2 - repoOut.tokens(2)._2,
            WIDCheckInRepo
          )
        )
      )
    }
  }
}
