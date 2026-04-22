package helpers

object Templates {

  val packageJsonTemplate: String =
    """{
      |  "name": "$packageName",
      |  "version": "$version-$networkType",
      |  "description": "TypeScript package for Rosen Bridge $networkType contracts and tokens",
      |  "repository": {
      |    "type": "git",
      |    "url": "git+https://github.com/rosen-bridge/contract.git"
      |  },
      |  "license": "MIT",
      |  "author": "Rosen Team",
      |  "types": "dist/index.d.ts",
      |  "main": "dist/index.js",
      |  "type": "module",
      |  "files": [
      |    "dist"
      |  ],
      |  "engines": {
      |    "node": ">=22.18.0",
      |    "npm": "11.6.2"
      |  }
      |}
      |""".stripMargin

  val contractsJsTemplate: String = 
    """export const contracts = $contractsJson;
      |""".stripMargin

  val tokensJsTemplate: String = 
    """export const tokens = $tokensJson;
      |""".stripMargin

  val indexJsTemplate: String = 
    """export { contracts } from './contracts.js';
      |export { tokens } from './tokens.js';
      |""".stripMargin

  val contractsDtsTemplate: String =
    """export type NetworkChain = 
      |$networkUnion;
      |
      |export interface GlobalTokens {
      |$globalTokens
      |}
      |
      |export interface ChainAddresses {
      |$addresses
      |}
      |
      |export interface ChainTokens {
      |$chainTokens
      |}
      |
      |export interface ChainConfig {
      |  "addresses": ChainAddresses;
      |  "tokens": ChainTokens;
      |  "cleanupConfirm": number;
      |}
      |
      |export interface Contracts extends Record<NetworkChain, ChainConfig> {
      |  "version": string;
      |  "tokens": GlobalTokens;
      |};
      |
      |export declare const contracts: Contracts;
      |""".stripMargin

  val tokensDtsTemplate: String =
    """import type { NetworkChain } from './index.d.ts';
       |
       |export interface TokenInfo {
       |  "tokenId": string;
       |  "name": string;
       |  "decimals": number;
       |  "type": string;
       |  "residency": string;
       |  "extra": Record<string, string | number | boolean>;
       |}
       |
       |export interface Tokens {
       |  "version": string;
       |  "tokens": Partial<Record<NetworkChain, TokenInfo>>[];
       |}
       |
       |export declare const tokens: Tokens;
       |""".stripMargin

  val indexDtsTemplate: String =
    """export type { Contracts, ChainConfig, ChainAddresses, ChainTokens, GlobalTokens, NetworkChain } from './contracts.d.ts';
      |export type { Tokens, TokenInfo } from './tokens.d.ts';
      |export { contracts } from './contracts.js';
      |export { tokens } from './tokens.js';
      |""".stripMargin

val readmeTemplate: String =
    """# @rosen-bridge/contract
      |
      |## Table of contents
      |
      |- [Introduction](#introduction)
      |- [Installation](#installation)
      |- [Usage](#usage)
      |
      |## Introduction
      |
      |`@rosen-bridge/contract` contains the TokenMaps and address of contracts for the **$networkType** network of the Rosen Bridge.
      |
      |This package provides TypeScript definitions and exports for:
      |- Contract addresses for all supported chains
      |- Token mappings for cross-chain assets
      |- Network configuration constants
      |
      |## Installation
      |
      |**npm:**
      |```bash
      |npm i @rosen-bridge/contract
      |```
      |
      |**yarn:**
      |```bash
      |yarn add @rosen-bridge/contract
      |```
      |
      |## Usage
      |
      |```typescript
      |import { contracts, tokens } from '@rosen-bridge/contract';
      |
      |// Get contract addresses
      |const rwtRepo = contracts.ergo.addresses.RWTRepo;
      |
      |// Get global tokens
      |const rsnTokenId = contracts.tokens.RSN;
      |
      |// Get token mapping
      |const adaOnCardano = tokens.tokens[0].cardano;
      |```
      |""".stripMargin
}
