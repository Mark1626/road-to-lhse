/**
 * AVL tree implementation using sqlite closure.c and amatch.c as reference
 */

#ifndef AVL_H
#define AVL_H

typedef struct avl_node_t {
  int key;
  struct avl_node_t *before;
  struct avl_node_t *after;
  struct avl_node_t *up;
  int height;
  int imbalance;
} avl_node_t;

avl_node_t* avl_node_new(int key);

/**
 * Insert a new AVL node. Returns NULL onn success. If key is not unique, 
 * then do not perform insert but leave node unchanged. Return a pointer to
 * the exisiting node with same key.
 */
avl_node_t *avl_insert(avl_node_t **head, avl_node_t *node);


/**
 * Search the tree rooted at p for entry key. Return a pointer to the entry
 * or return NULL
 */
avl_node_t *avl_search(avl_node_t *p, int key);

void avl_destroy(avl_node_t *p);

#endif /* AVL_H */
