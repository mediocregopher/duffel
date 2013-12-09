# touch extension

The touch extension lets you initialize an empty file with specific permissions,
or to change the permissions on an existing file.

## Usage

The touch extension supports the `chmod`, `owner`, and `group` meta parameters
from the [put][put] extension.

## Example

Given the following directory structure:
```
/opt/my-duffel/
    root/
        tmp/
            _meta.json
            foo._touch
```

with `_meta.json` containing:

```json
{
    "foo" : { "chmod" : "0755",
              "owner" : "foouser",
              "group" : "bargroup" }
}
```

The foo file will be created as an empty file if it doesn't already exist.
duffel will then apply the permissions given in `_meta.json`, regardless of
whether the file existed previously or not.

[put]: put.md
