# Symbolic link extension

lets you set up symbolic links instead of copying a file.

Useful if you want to keep your duffel version controlled (for dotfiles maybe)

## Usage

Simply put ._lns (as in ``` ln -s ``` ) as the extension

Meta Permissions don't apply since it's a symbolic link

## Example

Given the following directory structure:
```
/opt/my-duffel/
    tmp/
        foo._lns
```

The link will only be made if the file doesn't already exist.
