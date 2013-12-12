# duffel

Unpack your files from your repo to your computer.

## Basic Usage

Duffel is a standalone executable which will automatically deploy a directory
structure to your machine. You can download the executable
[here](https://raw.github.com/mediocregopher/duffel/master/duffel). Put it so
that it's in your path and is executable. When you run it the first time it will
download the duffel jar for you, and from then on you can call `duffel` and
it will automatically load up the duffel jar and pass all cli arguments through
to it.

Given the following directory structure:
```
/opt/my-duffel/
    root/
        tmp/
            one.txt
            two.txt
```

Running the following command:
```bash
duffel /opt/my-duffel
#or
cd /opt/my-duffel && duffel .
```

Will place the `one.txt` and `two.txt` files that were in your directory
structure into `/tmp` on the machine.

## Extensions

Duffel's default behavior is `put`, which simply puts a file or directory in
a given location. There are other behaviors, or extensions, which you can use,
like `touch` or `clerb`. These can be specified by putting a `._` and then the
extension name after the filename. For example:

```
/opt/my-duffel/
    root/
        tmp/
            one.txt._touch
            two.txt._clerb
            three.txt._put
```

If your file already has a `._` as part of the normal name, you'll need to
specify the `._put` (or whatever extension) at the end of the file, or duffel
will incorrectly process it.

A full list of extensions and links to their in-depth descriptions can be found
[here][extensions].

## Metadata

Metadata for files and directories is set in files called `_meta.json`. Each
extension accepts different metadata properties. For example, `put` uses
`chmod`, `owner`, and `group` on files and directories. For example:

```
/opt/my-duffel/
    root/
        tmp/
            _meta.json
            one.txt
            two/
                three.txt
```

with the following in `_meta.json`:

```json
{
    "one.txt": {
        "chmod": "0400",
        "owner": "user1",
        "group": "group1"
    },
    "two": {
        "owner": "user2",
        "group": "group2",
        "apply_deep": true
    }
}
```

would make `/tmp/one.txt` have the permissions of `0400` and be owned by
`user1:group2`, while the directory `two` would be owned by `user2:group2`.
Additionally, the `apply_deep` parameter indicates that the metadata set on
`two` will cascade down to its children (and their children, etc...). If
`apply_shallow` had been used instead, then the metadata would only have been
set on its children, not recursively.

`apply_deep` and `apply_shallow` are independent of extension, and simply carry
along whatever metadata the see. If a child sets the same key as is being
cascaded by one of the `apply_*`, then the child's value overwrites.

### Globs and `.`

You can use file globs (`*`) to capture multiple files/directories with a single
statement. For instance, `"foo.*"` would apply metadata to a file called
`foo.1`, `foo.2`, and `foo.3`, all at the same time (assuming their all in the
same directory). Currently, it's undefined what happens when two globs match on
the same file, although hopefully that can be remedied.

Additionally, you can use the `"."` key to set metadata on the directory the
`_meta.json` file is currently in.

## Location Specifiers

It's possible to specify different files or directories to use for depending on
certain parameters, such as what host you're running duffel on. You can do this
by placing the specifier after the extension in the file's name.

For example: `one.txt._put,specifier`. Since `put` is the default extension,
this can also be written: `one.txt._,specifier`.

The files with the same name but different specifiers are grouped, and the most
specific specifier is chosen. The specifiers, in order of specificity, can be
the following:

* The fdqn of a box
* The short hostname of a box
* A [groupname][groups]
* `_default` (matches on all boxes, automatically used if none other is)

For example:
```
/opt/my-duffel/
    root/
        tmp/
            one.txt._,hostname1
            one.txt._,hostname2
            one.txt._,_default
```

(The final `one.txt._,_default` could just be `one.txt`)

If the current host's shortname is `hostname1`, than the first `one.txt` file
will be used. If it's `hostname2`, than the second `one.txt` file will be used.
If it's neither, than the third `one.txt` file will be used.

If the `one.txt._,_default` file hadn't been present, and the hostname on the
box wasn't `hostname1` or `hostname2`, than no `one.txt` file would be placed.

Finally, it should be noted that it is completely valid to have different
extensions for different specifiers. For instance having `one.txt._put,a` and
`one.txt._touch,b` in the same directory.

## License

Copyright © 2012 Brian Picciano

Distributed under the Eclipse Public License, the same as Clojure.

[extensions]: /doc/extensions/extensions.md
[groups]: /doc/scripts/scripts.md
