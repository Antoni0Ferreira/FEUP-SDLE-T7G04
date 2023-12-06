## Instructions

### Compilation

 - `make compile` - to compile all classes
 - `make compileCRDT` - to compile all CRDTs
 - `make compileServerClient` - to compile all server client logic classes
 - `make compileShopping` - to compile all shopping list package classes

### Clean
 - `make clean` - to remove all .class files

### Run

From the `crdt/src` folder run:
 - `java sdle.serverClient.ServerManager` to turn on the serverManager 
 - `java sdle.serverClient.Server 127.0.0.2 8000` to turn on a server binded to the 127.0.0.2 IP in the port 8000
 - `java sdle.serverClient.Client` to turn on the client