# clj-ipfs
Clojure client for IPFS 

This project provides a Clojure client for IPFS. It communicates with a node over HTTP using the API defined here: https://docs.ipfs.io/reference/api/http/

## Example usage

In the following repl example, we put a block in IPFS and then retrieve it.

```.clj
[ipfs.main] →  (require '[ipfs.client :as c])
nil
[ipfs.main] →  (c/block-put "asdf")
{:key "QmeYzshSoNHr2QUWqmkMAy6raRhcmzTuroy7johWJNn3fY", :size 4}
[ipfs.main] →  (c/block-get "QmeYzshSoNHr2QUWqmkMAy6raRhcmzTuroy7johWJNn3fY")
"asdf"
```

## Current support

Much of the api here has parity with the python client including the following apis: files, blocks, objects, refs, bitswap, keys, pin, bootstrap, swarm, dht, config, log

There are some known missing pieces including support for `pubsub`.

## License

This code is distributed under the terms of the [MIT license](https://opensource.org/licenses/MIT).  Details can be found in the file
[LICENSE](LICENSE) in this repository.
