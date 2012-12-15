# duffel

## Basic Usage

Duffel is a standalone executable which will automatically deploy a directory structure to your machine. You can
download the exectuable [here](https://raw.github.com/mediocregopher/duffel/master/duffel). Put it so that it's in
your path and is executable. When you run it the first time it will download the duffel jar for you, and from then
on you can call ```duffel``` and it will automatically load up the duffel jar and pass all cli arguments through to
it.

Given the following directory structure:
```
/opt/my-duffel/
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

Will place the one.txt and two.txt files that were in your directory structure into /tmp on the machine.

## Per-host files

With the following directory structure:
```
/opt/my-duffel/
    tmp/
        one.txt._hostname1
        one.txt._hostname2
        one.txt.__default
```

duffel will place the first file as `/tmp/one.txt` if the machine that it's running on's hostname is `hostname1`,
the second file if it's `hostname2`, and the third for all others. For hostname matching duffel will go through
each candidate file and try to match the fdqn. If none are matched it will go again and try to match just the
hostname. Finally it will look for *.__default.

**Note** in the following scenario:
```
/opt/my-duffel/
    tmp/
        one.txt._hostname1
        one.txt._hostname2
        one.txt
```

The third item will have .__default implicitely appended to the end. However, this is could create problems if
your file names naturally have ```._``` in them, so be careful.

If you don't specify a default file (either explicitely or implicitely), such as in the following:
```
/opt/my-duffel/
    tmp/
        one.txt._hostname1
        one.txt._hostname2
```

then the file will not be put if it does not match one of the given hostnames.

You can apply the same to directories as well:
```
/opt/my-duffel/
    tmp/
        one._hostname1/
            green.txt
        one.__default/
            blue.txt
```

Will put ```/tmp/one/``` with ```green.txt``` in it if the host's name is ```hostname1```, and ```blue.txt```
otherwise.

## Per-group files (not done yet)
**Per-group files haven't been implemented yet**

With the following directory structure:
```
/opt/my-duffel/
    tmp/
        one.txt._g_nodegroup1
        one.txt._g_nodegroup2
        one.txt._hostname1
        one.txt.__default
```

This will check if the box is a member of ```nodegroup1```, etc... and place the individual file accordingly.

The order of priority for matching is as follows:
* fdqn
* hostname
* group
* default

## Metadata

Every folder (including the top level one, if you're feeling dangerous) can have a ```_meta.json``` file. This file contains information
about the contents of the folder that couldn't otherwise be encoded into the filenames, such as the permissions of the file, ownership,
etc... Given the following structure:
```
/opt/my-duffel/
    tmp/
        _meta.json
        one.txt
```

The ```_meta.json``` file might look like the following:
```json
{
    ".":       { "chmod": "0755" },
    "one.txt": { "chmod": "0644" }
}
```

This would give ```/tmp``` the permissions of 0755 and ```/tmp/one.txt``` the permissions of 0644. We can also set ownership for files and
folders, although running duffel as anything but root and trying to do this will not work. Here's an example of setting ownership:
```json
{
    ".":       { "chmod": "0755", "owner":"root", "group":"root" },
    "one.txt": { "chmod": "0644", "owner":"nginx","group":"nginx" }
}
```

The metadata object can also be for a directory. For instance, given the following structure:
```
/opt/my-duffel/
    tmp/
        _meta.json
        one/
            two.txt
```

The ```_meta.json``` file could look like:
```json
{
    ".":   { "owner":"root" },
    "one": { "owner":"notroot", "chmod":"0700" }
}
```

If you set owner but not group, by default the group name set will be the same as the owner name. The owner defaults to whatever user is
running the duffel process if it's not set. Chmod defaults to 0644 for files and 0755 for directories.

All directory objects (including the one for ".") can additionally have the following parameters:

* "apply_to_contents" (bool): Whether or not to apply the metadata parameters to the contents of the 
                              directory
* "apply_recursively" (bool): Whether or not to apply the metadata parameters recursively (implies 
                              "apply_to_contents")
* "delete_untracked"  (bool): Delete any files/folders in the directory which aren't explicitely put 
                              by duffel
* "force_ownership"   (bool): By default if a directory already exists duffel won't try to change its 
                              ownership or chmod permissions. This option makes it force (attempt) to
                              change the directory's permissions, in contrast with the behavior for
                              files which always attempt to change permissions. I'm still going back
                              and forth on this behavior, I'm not married to it.

All of these parameters default to false if not specified.

### Metadata files for specific nodes

Metadata files undergo the same selection process as the other files. For example:
```
/opt/my-duffel/
    tmp/
        _meta.json._hostname1
        _meta.json.__default
        one.txt
```

### Cascading Metadata

When ```apply_to_contents``` or ```apply_recursively``` are set for a directory, we have to look at how the permissions for the directories
lower in the tree are going to be set. For example:
```
/opt/my-duffel/
    tmp/
        _meta.json
        one/
            _meta.json
            two.txt
```

With the first ```_meta.json``` having:
```json
{
    ".": { "owner":"user1", "apply_recursively":true }
}
```

and the second:
```json
{
    ".":       { "owner":"user2", "apply_to_contents":true }
    "two.txt": { "owner":"user3" }
}
```

As with most (hopefully all, if I've designed this right) things in duffel, the most specific rule is what gets used. So in the above example
this is how the permissions for each item would end up:

* /tmp            -> owned by user1
* /tmp/one/       -> owned by user2
* /tmp/one/two.xt -> owned by user3

## Special Folder Names

There are special folder names you can use in the root of your duffel project which will map based on environment variables or even custom scripts.

For example:
```
/opt/my-duffel/
    tmp/
        one.txt
    _HOME/
        two.txt
```

Given the above, if your ```$HOME``` environment variable is set to ```/home/user1```, then the final structure that duffel will put is:
```
/tmp/one.txt
/home/user1/two.txt
```

**NOTE** You can only use the environment variable special syntax in the root of the duffel project, doing something like ```/tmp/_HOME```
will not work.

It will be possible to be able to script your own mappings, so you don't have to set a bunch of crap in your environment. I haven't
worked out the deets on that yet though.

## License

Copyright © 2012 Brian Picciano

Distributed under the Eclipse Public License, the same as Clojure.
