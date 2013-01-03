# git extension

The git extension makes it possible to initialize and set up git repos in folders of your choosing.
To use it apply the git extension to a folder of your choosing. 

## Usage

You'll need to apply at least one metadata parameter to that folder, `git_url`, which is the url 
duffel will use to clone and pull from. Duffel will attempt to keep the project up to date to the 
latest commit from a specific branch (defaults to "master", but can be specified with `git_branch`) 
everytime it runs. 

The final option is `git_user`, which is the username you want the git clone and pull commands to 
run as. This user's `.gitconfig` and `.ssh/config` files will be used. By default the user running 
duffel is used. Additionally, if `git_user` specifies a user which is not the current one and the
current one isn't root then an error will be thrown, since duffel can't automatically switch to
another user in that case.

The git extension also supports put's `owner`, `group`, and `chmod`. These will only be applied to the
top level folder, all sub-files and sub-folders will be owned by whichever user is specified with the
`git_user` parameter. A weird side-effect of this is that if you're running duffel as root, and you
want the entire project including the top-level directory of it to be owned by some user, you need to
specify that user for `git_user`, `owner`, and `group`. The `owner` and `group` ensure that the top-
level directory is owned by the desired user. This is sucks and I want to change it, I just need to
think of how.

## Example

Given the following structure:


Given the following directory structure:
```
/opt/my-duffel/
    tmp/
        duffel-repo._git/
            some_other_file
            _meta.json
```

with `_meta.json` containing:

```json
{
    "." : { "git_url" : "https://github.com/mediocregopher/duffel.git",
            "git_branch" : "master",
            "git_user" : "mediocregopher",
            "owner" : "mediocregopher",
            "group" : "mediocregopher" }
}
```

The duffel project will be cloned into `/tmp/duffel-repo`, with all files and the top level folder
being owned by `mediocregopher`. Additionally `some_other_file` will be put in `/tmp/duffel-repo`
using the normal put extension.
