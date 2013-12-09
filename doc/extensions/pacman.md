# pacman extension

Pacman is the default package manager (get it?) for Arch linux. This extension
makes initial setup of a box with a set of packages significantly easier. Due to
the hands-on nature of package management in Arch, this is extension is not
aimed at in-depth maintenance of your packages. It does not handle versioning or
updating the packages, and if there are any problems at all it will stop
execution.

## Usage

Your `*._pacman` file will not actually be put in the filesystem. The file
should contain a list of package names, with one package per line. It then runs:

```bash
sudo pacman -Sy --noconfirm --noprogressbar --needed <extra opts> <packages>
```

If there are any errors of any sort duffel will output them and stop execution.

Extra options can be specified in `_meta.json` with the `extra-opts` field. It
should be a list of strings, each one a parameter to include, or the value for
the previous paramater. For example:
```json
{
    "toinstall" : { "extra-opts" : [ "--cachedir" "/some/directory" "--debug" ] }
}
```

## Example

Given the following directory structure:
```
/opt/my-duffel/
    root/
        toinstall._pacman
```

with `toinstall._pacman` containing:
```
zsh
openssh
scrot
feh

#empty lines and lines starting with a hash will be ignored
rsync
vlc
fortune
cowsay
```

Duffel will sync all of those packages

# yaourt extension

Duffel can also interact with yaourt. instead of `*._pacman` use `*._yaourt`.
The behavior remains exactly the same except that it uses yaourt instead of
pacman so you can install packages straight out of the AUR. If yaourt doesn't
already exist on your system duffel will first install it through the
archlinuxfr repo.
