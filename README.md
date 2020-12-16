# Krypton

**CAUTION!** Krypton is a work-in-progress. I do not provide any guarantees about its stability,
             compatibility with other mods, or support for every possible setup out there. Support
             for this mod is provided on a "best-effort" basis. This is not my day job, it is a hobby
             growing out of related work I've done. **You have been warned.**

This Fabric mod derives from work done in the [Velocity](https://velocitypowered.com/) and [Tuinity](https://github.com/Spottedleaf/Tuinity)
projects. Specifically, Krypton is an attempt to optimize the Minecraft networking stack.

Krypton derives itself from Ancient Greek _kryptos_, which means "the hidden one". This makes
it evident most of the benefit from Krypton is "hidden" but is noticeable by a server administrator.

[Please join my Discord](https://discord.gg/RUGArxEQ8J) to discuss the mod or get support.

## Compiling / Releases

**CAUTION!** Krypton is a work-in-progress. I do not provide any guarantees about its stability,
             compatibility with other mods, or support for every possible setup out there. Support
             for this mod is provided on a "best-effort" basis. This is not my day job, it is a hobby
             growing out of related work I've done. **You have been warned.**

Development builds may be downloaded from my [Jenkins server](https://ci.velocitypowered.com/job/krypton/).
They may or may not work. You can also compile the mod from source.

## Compatibility

### With other mods

I try to ensure that Krypton will work with the Fabric API, Lithium, and Sodium. Support beyond these
mods is provided on a best effort basis.

### With your operating system

Krypton will work anywhere you can launch a Fabric server. The following Krypton components will work regardless of platform:

* introducing optimized packet splitting (client and server)
* adding an async entity tracker (server only)
* adding flush consolidation (server only)
* micro-optimizations to reduce garbage produced by the networking stack (client and server)

The following components currently only work on Linux x86_64 and aarch64, but we plan to add Windows support:

* updating the pipeline from using the `java.util.zip.*` zlib bindings to use libdeflate (client and server)

The following components work only on Linux x86_64 and aarch64 and it is unlikely we will add Windows support:

* introducing optimized encryption (server only, client eventually)

#### Why are you focused on Linux?

There are several practical reasons for me to focus on Linux first:

* I primarily use Linux as my primary development platform. This is largely personal preference but it helps
  that native development tools on Linux are often faster and better than on Windows. This makes prototyping
  new potential features easier.
* Krypton often introduces native code for certain highly-optimized libraries that are not written in Java and
  where the benefit of using the library is able to pay for the cost of a JNI transition. However, when we use
  a native library, we need to invest effort into compiling and testing the code on each platform we intend to
  support that library on. Since Linux is my main development platform, the cost for Linux support is effectively free.
* Most of the functionality Krypton introduces has the most impact on the server. While the Minecraft client
  uses the same networking components as the server, it does not utilize the networking stack as heavily as the
  server. It's a fairly consistent pattern that most Minecraft servers are deployed on Linux servers, be that through a
  shared host, someone repurposing an old computer to run a Minecraft server, or buying a dedicated server.
* In the case of encryption, the OpenSSL library is used to support direct encryption of packets with memory copies
  required. While OpenSSL is common on most free and open source *nixes, it is notably absent from Windows and not
  in any reasonably up-to-date form on macOS. In the US, the export of cryptography has some restrictions and I do not
  want to bother with it, so I let the Linux distributions handle this issue for me.
