/**
 * t-digest implementation from https://github.com/RedisBloom/t-digest-c
 * 
 * This was reimplemented with the minimal needed functions
 * for learning purposes
 *
 * Copyright (c) 2021 Redis, All rights reserved.
 * Copyright (c) 2018 Andrew Werner, All rights reserved.
 *
 */
#ifndef TDIGEST_H
#define TDIGEST_H

#include <errno.h>
#include <math.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdint.h>

typedef struct td_histogram_t {
  double compression;
  double min;
  double max;
  int cap;
  int merged_nodes;
  int unmerged_nodes;
  long long total_compressions;
  long long merged_weight;
  long long unmerged_weight;
  double *nodes_mean;
  long long *nodes_weight;
} td_histogram_t;

#ifndef _alloc
#define _alloc malloc
#endif
#ifndef _free
#define _free free
#endif

#define __td_max(x, y) (((x) > (y)) ? (x) : (y))
#define __td_min(x, y) (((x) < (y)) ? (x) : (y))

static inline double weighted_average_sorted(double x1, double w1, double x2,
                                             double w2) {
  const double x = (x1 * w1 + x2 * w2) / (w1 + w2);
  return __td_max(x1, __td_min(x, x2));
}

static inline bool _tdigest_long_long_add_safe(long long a, long long b) {
  if (b < 0) {
    return (a >= __LONG_LONG_MAX__ - b);
  } else {
    return (a <= __LONG_LONG_MAX__ - b);
  }
}

static inline double weighted_average(double x1, double w1, double x2,
                                      double w2) {
  if (x1 <= x2) {
    return weighted_average_sorted(x1, w1, x2, w2);
  } else {
    return weighted_average_sorted(x2, w2, x1, w1);
  }
}

static void inline swap(double *arr, int i, int j) {
  const double temp = arr[i];
  arr[i] = arr[j];
  arr[j] = temp;
}

static void inline swap_l(long long *arr, int i, int j) {
  const long long temp = arr[i];
  arr[i] = arr[j];
  arr[j] = temp;
}

static inline size_t cap_from_compression(double compression) {
  if ((size_t)compression > ((SIZE_MAX / sizeof(double) / 6) - 10)) {
    return 0;
  }

  return (6 * (size_t)(compression)) + 10;
}

static int tdigest_init(td_histogram_t **res, double compression) {
  const size_t capacity = cap_from_compression(compression);
  if (capacity < 1)
    return 1;
  
  td_histogram_t *td = (td_histogram_t *)_alloc(sizeof(td_histogram_t));
  if (!td)
    return 1;

  td->cap = capacity;
  td->compression = compression;
  td->min = __DBL_MAX__;
  td->max = -td->min;
  td->merged_nodes = 0;
  td->merged_weight = 0;
  td->unmerged_nodes = 0;
  td->unmerged_weight = 0;
  td->total_compressions = 0;
  td->nodes_mean = (double *)_alloc(capacity * sizeof(double));
  if (!td->nodes_mean)
    return 1;
  td->nodes_weight = (long long *)_alloc(capacity * sizeof(long long));
  if (!td->nodes_weight)
    return 1;
  
  *res = td;

  return 0;
}

static void tdigest_free(td_histogram_t *td) {
  if (td->nodes_mean)
    _free((void *)td->nodes_mean);
  if (td->nodes_weight)
    _free((void *)td->nodes_weight);
  _free((void *)td);
}

static unsigned int partition(double *means, long long *weights,
                              unsigned int start, unsigned int end,
                              unsigned int pivot_idx) {
  const double pivot_mean = means[pivot_idx];
  swap(means, pivot_idx, end);
  swap_l(weights, pivot_idx, end);

  int i = start - 1;
  for (unsigned j = start; j < end; j++) {
    if (means[j] < pivot_mean) {
      i++;
      swap(means, i, j);
      swap_l(weights, i, j);
    }
  }
  swap(means, i + 1, end);
  swap_l(weights, i + 1, end);
  return i + 1;
}

static void tdigest_qsort(double *means, long long *weights, unsigned int start,
                          unsigned int end) {
  if (start < end) {
    if ((end - start) == 1) {
      if (means[start] > means[end]) {
        swap(means, start, end);
        swap_l(weights, start, end);
      }
      return;
    }
    const unsigned int pivot_idx = (end + start) / 2;
    const unsigned int new_pivot_idx =
        partition(means, weights, start, end, pivot_idx);
    if (new_pivot_idx > start) {
      tdigest_qsort(means, weights, start, new_pivot_idx - 1);
    }
    tdigest_qsort(means, weights, new_pivot_idx + 1, end);
  }
}

static inline bool _should_compress(td_histogram_t *td) {
  return ((td->merged_nodes + td->unmerged_nodes) >= (td->cap - 1));
}

static inline int _check_td_overflow(const double new_unmerged_weight,
                                     const double new_total_weight) {
  // double-precision overflow detected on h->unmerged_weight
  if (new_unmerged_weight == INFINITY) {
    return EDOM;
  }
  if (new_total_weight == INFINITY) {
    return EDOM;
  }
  const double denom = 2 * M_PI * new_total_weight * log(new_total_weight);
  if (denom == INFINITY) {
    return EDOM;
  }

  return 0;
}

static inline int _check_overflow(const double v) {
  // double-precision overflow detected on h->unmerged_weight
  if (v == INFINITY) {
    return EDOM;
  }
  return 0;
}

static int tdigest_compress(td_histogram_t *td) {
  if (td->unmerged_nodes == 0) {
    return 0;
  }
  int N = td->merged_nodes + td->unmerged_nodes;
  tdigest_qsort(td->nodes_mean, td->nodes_weight, 0, N - 1);
  const double total_weight = (double)td->merged_weight + (double)td->unmerged_weight;
  const int overflow_res =
      _check_td_overflow((double)td->unmerged_weight, (double)total_weight);
  if (overflow_res != 0)
    return overflow_res;
  if (total_weight <= 1)
    return 0;
  const double denom = 2 * M_PI * total_weight * log(total_weight);
  if (_check_overflow(denom) != 0)
    return EDOM;

  // Compute the normalizer given compression and number of points.
  const double normalizer = td->compression / denom;
  if (_check_overflow(normalizer) != 0)
    return EDOM;
  int cur = 0;
  double weight_so_far = 0;

  for (int i = 1; i < N; i++) {
    const double proposed_weight =
        (double)td->nodes_weight[cur] + (double)td->nodes_weight[i];
    const double z = proposed_weight * normalizer;
    // quantile up to cur
    const double q0 = weight_so_far / total_weight;
    // quantile up to cur + i
    const double q2 = (weight_so_far + proposed_weight) / total_weight;
    // Convert  a quantile to the k-scale
    const bool should_add = (z <= (q0 * (1 - q0))) && (z <= (q2 * (1 - q2)));
    if (should_add) {
      td->nodes_weight[cur] += td->nodes_weight[i];
      const double delta = td->nodes_mean[i] - td->nodes_mean[cur];
      const double weighted_delta =
          (delta * td->nodes_weight[i]) / td->nodes_weight[cur];
      td->nodes_mean[cur] += weighted_delta;
    } else {
      weight_so_far += td->nodes_weight[cur];
      cur++;
      td->nodes_weight[cur] = td->nodes_weight[i];
      td->nodes_mean[cur] = td->nodes_mean[i];
    }
    if (cur != i) {
      td->nodes_weight[i] = 0;
      td->nodes_mean[i] = 0.0;
    }
  }
  td->merged_nodes = cur + 1;
  td->merged_weight = total_weight;
  td->unmerged_nodes = 0;
  td->unmerged_weight = 0;
  td->total_compressions++;
  return 0;
}

static inline int next_node(td_histogram_t *td) {
  return td->merged_nodes + td->unmerged_nodes;
}

#define CAPACITY_FULL 34

static int tdigest_add(td_histogram_t *td, double mean, long long weight) {
  if (_should_compress(td)) {
    const int overflow_res = tdigest_compress(td);
    if (overflow_res != 0)
      return overflow_res;
  }

  const int pos = next_node(td);
  if (pos >= td->cap)
    return CAPACITY_FULL;
  if (_tdigest_long_long_add_safe(td->unmerged_weight, weight) == false)
    return EDOM;
  const long long new_unmerged_weight = td->unmerged_weight + weight;
  if (_tdigest_long_long_add_safe(new_unmerged_weight, td->merged_weight) ==
      false)
    return EDOM;
  const long long new_total_weight = new_unmerged_weight + td->merged_weight;

  const int overflow_res =
      _check_td_overflow((double)new_unmerged_weight, (double)new_total_weight);
  if (overflow_res != 0)
    return overflow_res;

  if (mean < td->min)
    td->min = mean;
  if (mean > td->max)
    td->max = mean;

  td->nodes_mean[pos] = mean;
  td->nodes_weight[pos] = weight;
  td->unmerged_nodes++;
  td->unmerged_weight = new_unmerged_weight;
  return 0;
}

static double td_internal_iterate_centroids_to_index(
    const td_histogram_t *td, const double index, const double left_centroid_weight,
    const int total_centroids, double *weights_so_far, int *node_pos) {
  if (left_centroid_weight > 1 && index < left_centroid_weight / 2)
    return td->min + (index - 1) / (left_centroid_weight / 2 - 1) *
                         (td->nodes_mean[0] - td->min);

  if (index > td->merged_weight - 1)
    return td->max;

  const double right_centroid_weight =
      (double)td->nodes_weight[total_centroids - 1];
  const double right_centroid_mean = td->nodes_mean[total_centroids - 1];
  if (right_centroid_weight > 1 &&
      (double)td->merged_weight - index <= right_centroid_weight / 2) {
    return td->max - ((double)td->merged_weight - index - 1) /
                         (right_centroid_weight / 2 - 1) *
                         (td->max - right_centroid_mean);
  }

  for (; *node_pos < total_centroids - 1; (*node_pos)++) {
    const int i = *node_pos;
    const double node_weight = (double)td->nodes_weight[i];
    const double node_weight_next = (double)td->nodes_weight[i + 1];
    const double node_mean = td->nodes_mean[i];
    const double node_mean_next = td->nodes_mean[i + 1];
    const double dw = (node_weight + node_weight_next) / 2;
    if (*weights_so_far + dw > index) {
      double left_unit = 0;
      if (node_weight == 1) {
        if (index - *weights_so_far < 0.5)
          return node_mean;
        else
          left_unit = 0.5;
      }
      double right_unit = 0;
      if (node_weight_next == 1) {
        if (*weights_so_far + dw - index <= 0.5)
          return node_mean_next;
        right_unit = 0.5;
      }
      const double z1 = index - *weights_so_far - left_unit;
      const double z2 = *weights_so_far + dw - index - right_unit;
      return weighted_average(node_mean, z2, node_mean_next, z1);
    }
    *weights_so_far += dw;
  }

  const double z1 = index - td->merged_weight - right_centroid_weight / 2;
  const double z2 = right_centroid_weight / 2 - z1;
  return weighted_average(right_centroid_mean, z1, td->max, z2);
}

static double tdigest_quantile(td_histogram_t *td, double q) {
  tdigest_compress(td);
  if (q < 0.0 || q > 1.0 || td->merged_nodes + td->unmerged_nodes == 0)
    return NAN;
  if (td->merged_nodes + td->unmerged_nodes == 1)
    return td->nodes_mean[0];
  const double index = q * (double)td->merged_weight;
  if (index < 1)
    return td->min;

  const int n = td->merged_nodes;
  const double left_centroid_weight = (double)td->nodes_weight[0];
  double weight_so_far = left_centroid_weight / 2;
  int i = 0;
  return td_internal_iterate_centroids_to_index(td, index, left_centroid_weight,
                                                n, &weight_so_far, &i);
}

#endif
