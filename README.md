# Krypton (WIP)

This Fabric mod is an attempt to optimize the Minecraft networking stack by:

* updating the pipeline from using the `java.util.zip.*` zlib bindings to use libdeflate
* introducing optimized packet splitting
* introducing optimized encryption
* adding an async entity tracker
* adding flush consolidation

This project can be considered a WIP. The goal is to provide a networking complement to
Lithium.

Krypton derives itself from Ancient Greek _kryptos_, which means "the hidden one". This makes
it evident most of the benefit from Krypton is "hidden" but is noticeable by a server administrator.

I hope you will get involved in the development of Krypton, it'll be pretty exciting!
[Please join my Discord](https://discord.gg/RUGArxEQ8J).

## Compiling / Releases

Well, this mod is not quite stable yet. It's my first Fabric mod after all. You should just
compile from source.

## Compatibility

The following Krypton components should work regardless of platform:

* introducing optimized packet splitting
* adding an async entity tracker
* adding flush consolidation

The following components currently only work on Linux x86_64 and aarch64, but we plan to add Windows support:

* updating the pipeline from using the `java.util.zip.*` zlib bindings to use libdeflate

The following components work only on Linux x86_64 and aarch64 and it is unlikely we will add Windows support:

* introducing optimized encryption

### Why?

For one thing, Linux is my primary development environment. For another thing, Krypton often
introduces native code (that's how we can get libdeflate in place of zlib for instance). This
code has to be compiled and tested against each OS we intend to add support for.

The compression code uses the [libdeflate library](https://github.com/ebiggers/libdeflate) and
is thus simple enough to port to other OSes. I already have a proof of concept of a libdeflate JNI
binding being compiled and tested successfully on Windows, so it is only a matter of time before
I bring that support to Windows.

On the other hand, the encryption code uses the OpenSSL library, common on most free and open source
*nixes but is notably absent from Windows and not in any reasonably up-to-date form on macOS. It would require
a lot of headaches for me to ship encryption code directly in Krypton so I have elected to offload this
task to the Linux distributions. This doesn't preclude Windows or macOS ports, it's just not a priority for me.