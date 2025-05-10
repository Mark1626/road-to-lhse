UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
STATICFLAG=-fPIC -shared
else
STATICFLAG=-fPIC -dynamiclib
endif

CFLAGS=-Wall -Wextra -std=c99 -O3 -g -pedantic `pkg-config --cflags --libs sqlite3`

tdigest.dylib: sqlite3-tdigest.c
	gcc $(CFLAGS) $(STATICFLAG) -o $@ $<

clean:
	rm -f tdigest.dylib

.PHONY: clean
