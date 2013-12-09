# is-in-group script

The is-in-group script is housed in the [scripts](scripts.md) directory and
needs to be given by the user if duffel is required to determine whether or not
a node is in a group.

## Usage

Duffel will call the script with the group name in question as the only
argument. The script must output on stdout either a "0" (not in given group) or
a "1" (is in given group). Newline optional. Check the [scripts](scripts.md)
page for information on the environment the script will be called in.
