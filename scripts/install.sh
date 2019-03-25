#!/bin/bash

version=0.4.19
arch=linux-amd64
file=go-ipfs_v$version_arch.tar.gz
source=https://dist.ipfs.io/go-ipfs/v$version/$file
command=$1

if ! hash ipfs 2>/dev/null; then
    wget $source
    tar -zxvf ./file
    pushd go-ipfs
    sudo ./install.sh
    popd
fi

if [ "$command" == "start" ]; then
    ipfs daemon
fi
