#!/bin/sh

ps -fu $USER | grep ServerMain | grep -v grep | awk '{print $2;}'
ps -fu $USER | grep ServerMain | grep -v grep | awk '{print $2;}' | xargs --no-run-if-empty kill -9
