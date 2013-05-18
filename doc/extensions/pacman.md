# pacman extension

Pacman is the default package manager (get it?) for Arch linux. This extension makes initial setup
of a box with a set of packages significantly easier. Due to the hands-on nature of package management
in Arch, this is extension is not aimed at in-depth maintenance of your packages. It does not handle
versioning or updating the packages, and if there are any problems at all it will stop execution.

## Usage

Your `*._pacman` file will not actually be put in the filesystem. The file should contain a list of
package names, with one package per line. That's it. Everytime duffel runs it will look through
the list and remove all from the list packages that are already installed, regardless of if they could be upgraded
or not. It then runs:
```
sudo pacman -S --noconfirm --noprogressbar <extra opts> <packages>
```

If there
are any errors of any sort duffel will output them and stop execution.

Extra options can be specified in _meta.json with the `extra-opts` field. It should be a list of
strings, each one a parameter to include, or the value for the previous paramater. For example:
```json
{
    "toinstall" : { "extra-opts" : [ "--cachedir" "/some/directory" "--debug" ] }
}
```

## Example

Given the following directory structure:
```
/opt/my-duffel/
    tmp/
        toinstall._pacman
```

with `toinstall._pacman` containing:

```
zsh
openssh
scrot
feh
rsync
vlc
fortune
cowsay
```

Duffel will install any of those packages which are not already installed.
