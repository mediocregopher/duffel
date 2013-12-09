# put extension

Puts a file or directory in the same relative place on the filesystem. Can
specify the permissions on the file as well.

## Usage

The file contents will be whatever you want them to be.

Available metadata keys are:

* `chmod`: The permissions, as a string, you want the file or folder to have
           (default `0644` for files, and `0755` for directories)
* `owner`: The owner the file should have. Default is whatever user is running
           duffel
* `group`: The group the file should have. Default is whatever user is running
           duffel
* `delete_untracked`: Delete any files found in the directory on the real
                      filesystem that aren't being put by duffel (Directory
                      only, default: `false`)

## Example

Given the following directory structure:
```
/opt/my-duffel/
    root/
        tmp/
            file._put
```

duffel will put the file `/tmp/file` with the contents as a direct copy from
the project.

