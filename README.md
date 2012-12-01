# duffel

## Basic Usage

Duffel is a standalone executable which will automatically deploy a directory structure to your machine.

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

## Per-group files

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

## Templates

With the following directory structure:
```
/opt/my-duffel/
    tmp/
        one.txt._t
```

```one.txt._t``` will be parsed through a template engine (tbd which one) and placed as ```/tmp/one.txt```.

If you have:
```
/opt/my-duffel/
    tmp/
        one.txt._t._hostname1
        one.txt._t.__default
```

The first file will be parsed and placed if the machine's name is ```hostname1```, otherwise the second one will
be.

You can also mix and match template files with non-template files:
```
/opt/my-duffel/
    tmp/
        one.txt._hostname1
        one.txt._t.__default
```

The above will place the first file un-parsed if the machine's name is ```hostname1```, otherwise it will parse the
second file and place it.

Another example:
```
/opt/my-duffel/
    tmp/
        one.txt._t._g_groupname1
        one.txt._t._hostname1
        one.txt._hostname2
        one.txt.__default
```

If hostname == ```hostname1```, parse and place the second file. Else if hostname == ```hostname2```, place the third file. Else if
the machine is in ```groupname1``` parse and place the first file. Else, place the fourth file.

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

* "apply_to_contents" (bool): Whether or not to apply the metadata parameters to the contents of the directory
* "apply_recursively" (bool): Whether or not to apply the metadata parameters recursively (implies "apply_to_contents")
* "delete_untracked"  (bool): Delete any files/folders in the directory which aren't explicitely put by duffel

All of these parameters default to false if not specified.

### Metadata files for specific nodes

Metadata files undergo the same selection process as the other files. For example:
```
/opt/my-duffel/
    tmp/
        _meta.json._nodename1
        _meta.json.__default
        one.txt
```

## License

Copyright © 2012 Brian Picciano

Distributed under the Eclipse Public License, the same as Clojure.
