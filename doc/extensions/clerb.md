# clerb extension

The clerb extension lets you make template files which duffel will execute for
you at runtime. The templating system used is [clerb][clerb], which is very
simple and easy to learn.

## Usage

Use similar to a normal file: the file contents will be whatever you want them
to be.

Available metadata keys are:

* `chmod`: The permissions, as a string, you want the file or folder to have
           (default `0644` for files, and `0755` for directories)
* `owner`: The owner the file should have. Default is whatever user is running
           duffel
* `group`: The group the file should have. Default is whatever user is running
           duffel

## Example

Given the following directory structure:
```
/opt/my-duffel/
    root/
        tmp/
            tplfile._clerb
```

with `tplfile._clerb` containing:
```
blah blah
1 + 2 = ##(+ 1 2)##
```

duffel will put the file `/tmp/tplfile` with the contents:
```
blah blah
1 + 2 = 3
```

[clerb]: https://github.com/mediocregopher/clerb
