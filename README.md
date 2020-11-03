# Cardigan (WIP)

This mod is an attempt to optimize the Minecraft networking stack by:

* updating the pipeline from using the `java.util.zip.*` zlib bindings to use libdeflate
* introducing optimized packet splitting and encryption from Velocity
* adding an async entity tracker
* adding flush consolidation

This project can be considered an extreme WIP and will likely be considerably broken. The
goal is to provide a networking complement to Lithium.