# Duffel symbolic link extension

Sets up a symbolic link that will point from somewhere on your filesystem to a file/folder inside
your duffel repo.

## Usage

Simply put ._dlns (as in ```duffel ln -s ``` ) as the extension.

Permissions don't apply since it's a symbolic link.

## Example

Given the following directory structure:
```
/opt/my-duffel/
    tmp/
        foo._lns
```

A symbolic link from `/tmp/foo` to `/opt/my-duffel/tmp/foo._lns` will be created.

The element being linked to can also be a directory.

*Note*: The link will only be made if the file/folder doesn't already exist.
