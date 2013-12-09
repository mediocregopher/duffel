# Scripts

Duffel has a number of scripts that it may or may not need for its projects.
These scripts are housed in a directory called `scripts` in the root of the
project.

## Basic Usage

Example:

```
/opt/my-duffel/
    scripts/
        is-in-group
    root/
        tmp/
            one.file
            two.file
            red.file
            blu.file
```

Scripts are called with whatever environment the caller of the duffel command
has, with cwd being the root of the duffel project (in the above example:
`./scripts/is-in-group ...`)

## Scripts list

The following is a list of all scripts that duffel may need. Files on this list
are not *required* for using duffel, but may be required for certain
functionality.

* [is-in-group](is-in-group.md)
