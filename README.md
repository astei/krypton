# Krypton (WIP)

This Fabric mod is an attempt to optimize the Minecraft networking stack by:

* updating the pipeline from using the `java.util.zip.*` zlib bindings to use libdeflate
* introducing optimized packet splitting and encryption from Velocity
* adding an async entity tracker
* adding flush consolidation

This project can be considered a WIP and will likely be broken. The goal is to provide a
networking complement to Lithium.

Krypton derives itself from Ancient Greek _kryptos_, which means "the hidden one". This makes
it evident most of the benefit from Krypton is "hidden" but is noticeable by a server administrator.

I hope you will get involved in the development of Krypton, it'll be pretty exciting!