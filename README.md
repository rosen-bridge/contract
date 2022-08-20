
# Rosen Bridge
This project is used for creating a centralized file for the address of contracts and TokenMaps depending on network and network type, all libraries and projects in Rosen Bridge are compatible with the outputs of these files.

## How to use ?
For build:
```shell
sbt run assembly
```

For create TokenMap file run this command (this is an example for testnet):
```shell
java -jar target/scala-2.12/contract-assembly-0.1.0-SNAPSHOT.jar tokens --type testnet --version 1.0.0
```

For create address of all contracts run this command (this is an example for cardano network with testnet type):
```shell
java -jar target/scala-2.12/contract-assembly-0.1.0-SNAPSHOT.jar contracts --network cardano --type testnet --version 1.0.0
```

You can see all commands with:  
```shell
java -jar target/scala-2.12/contract-assembly-0.1.0-SNAPSHOT.jar --help 
```
