# CYM-SOCKET
CYM-SOCKET is an api for the server CraftYourMind, using Netty to transmit packets between several BungeeCord instances.

It uses packets corrections and redundancy to transmit Messages with reliability.
Messages are identified with their UUID.
Command layers just have to register Listeners to receive and send packets on the desired channel.
