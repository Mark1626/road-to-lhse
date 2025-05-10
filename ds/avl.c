/**
 * AVL tree implementation using sqlite closure.c and amatch.c as reference
 */
#include <stdio.h>
#include <stdlib.h>

typedef struct avl_node_t {
  int key;
  struct avl_node_t *before;
  struct avl_node_t *after;
  struct avl_node_t *up;
  int height;
  int imbalance;
} avl_node_t;

avl_node_t* avl_node_new(int key) {
    avl_node_t *p = (avl_node_t*)calloc(1, sizeof(avl_node_t));
    p->key = key;
    return p;
}

void avl_recompute_height(avl_node_t *node) {
  int before = node->before ? node->before->height : 0;
  int after = node->after ? node->after->height : 0;
  node->imbalance = before - after;
  node->height = (before > after ? before : after) + 1;
}

/**
 *
 *    P            B
 *   /  \        /  \
 *  B    Z  =>  X    P
 * / \              / \
 * X  Y            Y   Z
 */
avl_node_t *avl_rotate_before(avl_node_t *p) {
  avl_node_t *b = p->before;
  avl_node_t *y = b->after;
  b->up = p->up;
  b->after = p;

  p->up = b;
  p->before = y;
  if (y)
    y->up = p;

  avl_recompute_height(p);
  avl_recompute_height(b);

  return b;
}

/**
 *
 *    P               A
 *   / \             /  \
 *  X   A     =>    P    Z
 *     /  \        / \
 *    Y    Z      X   Y
 */
avl_node_t *avl_rotate_after(avl_node_t *p) {
  avl_node_t *a = p->after;
  avl_node_t *y = a->before;

  a->up = p->up;
  a->before = p;

  p->up = a;
  p->after = y;
  if (y)
    y->up = p;

  avl_recompute_height(p);
  avl_recompute_height(a);

  return a;
}

/*
** Return a pointer to the pBefore or pAfter pointer in the parent
** of p that points to p.  Or if p is the root node, return pp.
*/
avl_node_t **avl_from_ptr(avl_node_t *p, avl_node_t **pp) {
  avl_node_t *up = p->up;
  if (up == 0)
    return pp;
  if (up->after == p)
    return &up->after;
  return &up->before;
}

avl_node_t *avl_balance(avl_node_t *p) {
  avl_node_t *top = p;
  avl_node_t **pp;
  while (p) {
    avl_recompute_height(p);
    if (p->imbalance >= 2) {
      avl_node_t *b = p->before;
      if (b->imbalance < 0)
        p->before = avl_rotate_after(b);
      pp = avl_from_ptr(p, &p);
      p = *pp = avl_rotate_before(p);
    } else if (p->imbalance <= (-2)) {
      avl_node_t *a = p->after;
      if (a->imbalance > 0)
        p->after = avl_rotate_before(a);
      pp = avl_from_ptr(p, &p);
      p = *pp = avl_rotate_after(p);
    }
    top = p;
    p = p->up;
  }
  return top;
}

avl_node_t *avl_search(avl_node_t *p, int key) {
  while (p && key != p->key)
    p = (key < p->key) ? p->before : p->after;
  return p;
}

avl_node_t *avl_first(avl_node_t *p) {
  if (p)
    while (p->before)
      p = p->before;
  return p;
}

avl_node_t *avl_next(avl_node_t *p) {
  avl_node_t *prev = 0;
  while (p && p->after == prev) {
    prev = p;
    p = p->up;
  }
  if (p && prev == 0)
    p = avl_first(p->after);
  return p;
}

avl_node_t *avl_insert(avl_node_t **head, avl_node_t *node) {
  avl_node_t *p = *head;
  if (p == 0) {
    p = node;
    node->up = 0;
  } else {
    while (p) {
      if (node->key < p->key) {
        if (p->before) {
          p = p->before;
        } else {
          p->before = node;
          node->up = p;
          break;
        }
      } else if (node->key > p->key) {
        if (p->after) {
            p = p->after;
        } else {
            p->after = node;
            node->up = p;
            break;
        }
      } else {
        return p;
      }
    }
  }
  node->before = 0;
  node->after = 0;
  node->height = 1;
  node->imbalance = 0;
  *head = avl_balance(p);
  return 0;
}

void avl_destroy(avl_node_t *p) {
  if (p) {
    avl_destroy(p->before);
    avl_destroy(p->after);
    free(p);
  }
}
