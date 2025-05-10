/**
 * Sqlite t-digest extension using https://github.com/RedisBloom/t-digest-c
 *
 * Author: M Nimalan (@mark1626)
 * License: MIT
 *
 * On Macos
 * gcc -fPIC -dynamiclib -O3 -g `pkg-config --cflags --libs sqlite3` sqlite3-tdigest.c -o tdigest.dylib
 *
 * On Unix
 * gcc -fPIC -shared -O3 -g `pkg-config --cflags --libs sqlite3` sqlite3-tdigest.c -o tdigest.so
 *
 * Usage:
 * 
 * .load tdigest
 * create table test (val float);
 * select count(val), tdigest(val, 0.5) from test;
 * select count(val), tdigest(val, 0.5) from test group by val < 5;
 **/
#include "sqlite3ext.h"
#include <stddef.h>
SQLITE_EXTENSION_INIT1

#include <assert.h>
#include <string.h>

#define _alloc sqlite3_malloc
#define _free sqlite3_free

#include "./tdigest.h"

typedef struct TDigestCtx {
  td_histogram_t *td;
  bool initialized;
  double quantile;
} TDigestCtx;

static void tdigest_step(sqlite3_context *context, int argc,
                         sqlite3_value **argv) {
  TDigestCtx *p;
  int type;
  assert(argc == 2);
  p = (TDigestCtx *)sqlite3_aggregate_context(context, sizeof(*p));
  type = sqlite3_value_numeric_type(argv[0]);
  if (p == 0)
    return;
  if (p && type != SQLITE_NULL) {
    if (!p->initialized) {
      int res = tdigest_init(&p->td, 200.0);
      if (res) {
        sqlite3_result_error_nomem(context);
        return;
      }
      p->quantile = sqlite3_value_double(argv[1]);
      p->initialized = true;
    }

    int res = tdigest_add(p->td, sqlite3_value_double(argv[0]), 1);
    if (res) {
      sqlite3_result_error(context, "Error adding to digest", -1);
      return;
    }
  }
}

static void tdigest_finalize(sqlite3_context *context) {
  TDigestCtx *p = (TDigestCtx *)sqlite3_aggregate_context(context, 0);
  if (!p) {
    sqlite3_result_null(context);
    return;
  }
  double result = tdigest_quantile(p->td, p->quantile);
  if (isnan(result)) {
    sqlite3_result_null(context);
  } else {
    sqlite3_result_double(context, result);
  }
  tdigest_free(p->td);
  memset(p, 0, sizeof(*p));
}

#ifdef _WIN32
__declspec(dllexport)
#endif
    int sqlite3_tdigest_init(sqlite3 *db, char **errmsg_ptr,
                             const sqlite3_api_routines *api) {
  (void)errmsg_ptr;
  int rc = SQLITE_OK;
  SQLITE_EXTENSION_INIT2(api);
  static const int flags = SQLITE_UTF8 | SQLITE_INNOCUOUS;

  rc = sqlite3_create_function(db, "tdigest", 2, flags, 0, 0, tdigest_step,
                               tdigest_finalize);
  return rc;
}
