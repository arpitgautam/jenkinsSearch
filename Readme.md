# Build Machine Search

## About

Build Machine Search is a java based indexing engine. It is meant to allow user discover all jenkins machines on a network 
and index on an elastic search server. Furthermore this elastic search server can be queried using a different tool to get information about these machines.

1. It discoveres all jenkins machines over a range of ip addresses.
1. Collects data from these machines
1. Sends this data to elastic search server.

## License

[Apache License, Version 2.0](http://apache.org/licenses/LICENSE-2.0).