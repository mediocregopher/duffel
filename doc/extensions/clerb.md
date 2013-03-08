# clerb extension

The clerb extension lets you make template files which duffel will execute for you at runtime. The
templating system used is [clerb](https://github.com/mediocregopher/clerb), which is very simple and
easy to learn.

## Usage

Use similar to a normal file: the file contents will be whatever you want them to be, and for metadata
you can use the `owner`, `group`, and `chmod`.

## Example

Given the following directory structure:
```
/opt/my-duffel/
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
