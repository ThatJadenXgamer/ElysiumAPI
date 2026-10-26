## Elysium API Code Convention

![linebreak](https://cdn.modrinth.com/data/cached_images/6a7b4189d580191e8b7254ac5fb37e054f654d9f.png)

### What is this?

Hello, hello! If you're reading this then it probably means you want to contribute to **Elysium API**, 
if that's the case then this document will catch you up to speed on our various domain-specific syntax conventions.
So that anything you end up making adheres to our pre-existing codebase.

![linebreak](https://cdn.modrinth.com/data/cached_images/6a7b4189d580191e8b7254ac5fb37e054f654d9f.png)

### Loader and Version Denominators 

Elysium uses the Asphodel Gradle Template which allows for shared code across multiple modloaders and versions; and 
right now our scope and targets are for the versions: `1.20.1` and `1.21.1` and the loaders: `fabric`, `forge` and `neoforge`.

To keep track of what code belongs to which version or loader's specific implementations on `src/main` we suffix the class names.

We first declare the version denominators as following:
* `20` - For implementation exclusive to version **1.20.1**
* `21` - For implementation exclusive to version **1.21.1**

###### *(example: `CustomBlock20.java` or `CustomBlock21.java`)*

Then we declare the loader denominators as following:
* `F` - **Fabric**-side implementations
* `L` - **Forge**-side implementations
* `N` - **NeoForge**-side implementations
* `Q` - **Quilt**-side implementation *(We do not support Quilt but in-case that changes in the future, we'd use this.)*

###### *(example: `CustomBlockF.java` or `CustomBlockL.java`)*

Of course, it goes without saying but in-case of implementations that are exclusive to a certain versions and/or loaders then
you can mix and match them in a way that is descriptive of where it's used (i.e. `CustomBlock20F.java` or `CustomBlockFL.java`)

![linebreak](https://cdn.modrinth.com/data/cached_images/6a7b4189d580191e8b7254ac5fb37e054f654d9f.png)

### What is up with the `api` and `impl` packages?

Elysium API's package structure is split into an `api` package where all the frontend exposed libraries for downstream mods reside,
likewise the `impl` package is where all the internal backend side of the mod resides that we don't want developers using our mod be touching.

Sometimes there may be some things inside of `api` that you wouldn't want developers accessing; in which case use `@ApiStatus.Internal`.
Pretty standard stuff but worth mentioning anyways.

![linebreak](https://cdn.modrinth.com/data/cached_images/6a7b4189d580191e8b7254ac5fb37e054f654d9f.png)

### Asphodel Merges

As briefly mentioned earlier we use Asphodel Gradle to ease multiloader and multiversion development; and one of the useful
tools we have in helping out with such a task is the `asphodel::merge` directive. whenever using these please delegate all the merge contributor
classes to the `merge` package so that we can easily keep track of which class is a true primary implementation and what is a contributor.

![linebreak](https://cdn.modrinth.com/data/cached_images/6a7b4189d580191e8b7254ac5fb37e054f654d9f.png)