# Extensions

Duffel has a system of extensions to provide for different behaviors for
different files in your system. All files processed by duffel go through an
extension (by default the extension used if none is specified is `put`, which
simply reads in the chmod/owner/group permissions and places the file).

## Basic Usage

The extension you want is specified in the filename, after the base filename and
before the hostname specifier (if you choose to specify).

```
/opt/my-duffel/
    tmp/
        one.txt._put
```

Extensions also work with hostname specifiers:

```
/opt/my-duffel/
    tmp/
        one.txt._put,hostname1
        one.txt._put,_default
```

Additionally, given two different extensions (let's say `ext1` and `ext2`) you
can mix and match extensions using hostname specifiers:

```
/opt/my-duffel/
    tmp/
        one.txt._ext1,hostname1
        one.txt._ext2,hostname2
```

## Metadata

If you look at the metadata section in the main README you can get a good idea
of how to use `_meta.json` files. All extensions take in the metadata from
`_meta.json` files for files/folders and act on it in their own way. Read the
docs for each extension individually to see what parameters they can take in.

## Extension list

There's one main extension, `put`. It puts files/folders. If an extension isn't
specified than `put` is automatically used.

Here are a list of other extensions which have been written thus far:
* [put](put.md)
* [ignore](ignore.md)
* [touch](touch.md)
* [clerb](clerb.md)
* [pacman](pacman.md)
