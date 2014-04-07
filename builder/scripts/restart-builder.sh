#!/bin/bash
cd "$(dirname "$0")"
sh kill-builder.sh
sh start-builder.sh
