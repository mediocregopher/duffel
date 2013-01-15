# Extensions

Duffel has a system of extensions to provide for different behaviors for different files in your
system. All files processed by duffel go through an extension (by default the extension used if
none is specified is put, which simply reads in the chmod/owner/group permissions and places the file).

## Basic Usage

The extension you want is specified in the filename, after the base filename and before the hostname
specifier (if you choose to specify).

```
/opt/my-duffel/
    tmp/
        one.txt._put        
```

Extensions also work with hostname specifiers:

```
/opt/my-duffel/
    tmp/
        one.txt._put._hostname1
        one.txt._put.__default
```

Additionally, given two different extensions (let's say ```ext1``` and ```ext2```) you can mix and
match extensions using hostname specifiers:

```
/opt/my-duffel/
    tmp/
        one.txt._ext1._hostname1
        one.txt._ext2._hostname2
```

## Metadata

If you look at the metadata section in the main README you can get a good idea of how to use
```_meta.json``` files. In reality the parameters shown there are passed into the ```put``` extension
and processed there. All extensions take in the metadata from _meta.json files for files/folders
and act on it in their own way. Read the docs for each extension individually to see what parameters
they can take in.

## Extension list

There's one main extension, ```put```. It puts files/folders. You can read the main README
page for an explanation of how it works (remember, if the extension isn't specified, ```put``` is used
automatically).

Here are a list of other extensions which have been written thus far:
* [git](git.md)
* [ignore](ignore.md)
* [touch](touch.md)
